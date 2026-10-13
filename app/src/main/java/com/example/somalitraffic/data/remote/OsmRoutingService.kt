package com.example.somalitraffic.data.remote

import com.example.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * GoBajaaj routing via OSRM / OpenStreetMap road data.
 *
 * Real production road routing:
 * - OSRM road geometry & maneuvers
 * - Turn-by-turn navigation instructions in Somali
 * - Distance, duration, ETA
 * - Automatic off-route detection
 */
object OsmRoutingService {

    data class NavigationStep(
        val instruction: String,       // e.g. "Midig u leexo", "Si toos ah u soco", "Roundabout-ka ka qaado exit-ka 2aad"
        val roadName: String,          // e.g. "Waddada Maka Al Mukarama"
        val distanceMeters: Double,    // distance of this maneuver
        val durationSeconds: Double,
        val maneuverType: String,      // turn, roundabout, arrive, depart, continue, fork, uturn
        val modifier: String?,         // right, left, slight right, uturn, straight
        val stepLat: Double,
        val stepLng: Double
    )

    data class RouteResult(
        val distanceMeters: Double,
        val durationSeconds: Double,
        /** ordered [lat, lng] pairs of the road geometry */
        val points: List<Pair<Double, Double>>,
        val steps: List<NavigationStep> = emptyList()
    ) {
        fun pointsJson(): String =
            points.joinToString(prefix = "[", postfix = "]") { "[${it.first},${it.second}]" }

        fun etaMinutes(): Int = (durationSeconds / 60.0).coerceAtLeast(1.0).toInt()
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private const val CACHE_LIMIT = 24
    private val cache = object : LinkedHashMap<String, RouteResult>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RouteResult>): Boolean =
            size > CACHE_LIMIT
    }

    /**
     * Translates OSRM maneuver type and modifier into clear, natural Somali instructions.
     */
    fun toSomaliInstruction(
        type: String?,
        modifier: String?,
        exit: Int?,
        roadName: String?
    ): String {
        val road = if (!roadName.isNullOrBlank()) " ($roadName)" else ""
        return when (type?.lowercase(Locale.US)) {
            "depart" -> "Biloow safarka$road"
            "arrive" -> "Waxaad soo gaartay goobta$road"
            "roundabout", "rotary", "roundabout turn" -> {
                val exitText = if (exit != null) "exit-ka ${exit}aad" else "wareegga"
                "Roundabout-ka ka qaado $exitText$road"
            }
            "turn", "end of road", "fork" -> {
                when (modifier?.lowercase(Locale.US)) {
                    "right", "sharp right" -> "Midig u leexo$road"
                    "slight right" -> "Wax yar midig u leexo$road"
                    "left", "sharp left" -> "Bidix u leexo$road"
                    "slight left" -> "Wax yar bidix u leexo$road"
                    "uturn" -> "U-turn samee"
                    "straight" -> "Si toos ah u soco$road"
                    else -> "Leexo$road"
                }
            }
            "continue", "new name" -> {
                when (modifier?.lowercase(Locale.US)) {
                    "uturn" -> "U-turn samee"
                    else -> "Si toos ah u soco$road"
                }
            }
            else -> {
                when (modifier?.lowercase(Locale.US)) {
                    "right", "sharp right" -> "Midig u leexo$road"
                    "slight right" -> "Wax yar midig u leexo$road"
                    "left", "sharp left" -> "Bidix u leexo$road"
                    "slight left" -> "Wax yar bidix u leexo$road"
                    "uturn" -> "U-turn samee"
                    else -> "Si toos ah u soco$road"
                }
            }
        }
    }

    /** Blocking call - must be invoked on Dispatchers.IO. Returns null when offline/unreachable. */
    fun fetchRoute(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): RouteResult? {
        val key = "${fromLat.round4()},${fromLng.round4()}-${toLat.round4()},${toLng.round4()}"
        synchronized(cache) { cache[key] }?.let { return it }

        val url = "${BuildConfig.OSRM_BASE_URL}/route/v1/driving/" +
            "$fromLng,$fromLat;$toLng,$toLat" +
            "?overview=full&geometries=geojson&alternatives=false&steps=true"

        return try {
            val resp = client.newCall(Request.Builder().url(url).build()).execute()
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            val json = JSONObject(body)
            if (json.optString("code") != "Ok") return null
            val routes = json.getJSONArray("routes")
            val route = routes.optJSONObject(0) ?: return null
            val distance = route.optDouble("distance", Double.NaN)
            val duration = route.optDouble("duration", Double.NaN)
            if (distance.isNaN() || duration.isNaN()) return null

            val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
            val points = ArrayList<Pair<Double, Double>>(coords.length())
            for (i in 0 until coords.length()) {
                val c = coords.getJSONArray(i)
                // GeoJSON coordinates are [lng, lat]
                points.add(Pair(c.optDouble(1), c.optDouble(0)))
            }

            // Parse steps for turn-by-turn navigation
            val stepList = mutableListOf<NavigationStep>()
            val legs = route.optJSONArray("legs")
            if (legs != null && legs.length() > 0) {
                val leg0 = legs.optJSONObject(0)
                val steps = leg0?.optJSONArray("steps")
                if (steps != null) {
                    for (sIdx in 0 until steps.length()) {
                        val st = steps.optJSONObject(sIdx) ?: continue
                        val stDistance = st.optDouble("distance", 0.0)
                        val stDuration = st.optDouble("duration", 0.0)
                        val stName = st.optString("name", "")
                        val man = st.optJSONObject("maneuver")
                        val manType = man?.optString("type", "turn") ?: "turn"
                        val manMod = if (man != null && man.has("modifier")) man.optString("modifier") else null
                        val manExit = if (man != null && man.has("exit")) man.optInt("exit") else null
                        val locArr = man?.optJSONArray("location")
                        val stLng = locArr?.optDouble(0) ?: toLng
                        val stLat = locArr?.optDouble(1) ?: toLat

                        val somaliText = toSomaliInstruction(manType, manMod, manExit, stName)
                        stepList.add(
                            NavigationStep(
                                instruction = somaliText,
                                roadName = stName,
                                distanceMeters = stDistance,
                                durationSeconds = stDuration,
                                maneuverType = manType,
                                modifier = manMod,
                                stepLat = stLat,
                                stepLng = stLng
                            )
                        )
                    }
                }
            }

            val result = RouteResult(distance, duration, points, stepList)
            if (points.isNotEmpty()) {
                synchronized(cache) { cache[key] = result }
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Offline/no-network fallback: straight-line distance with a city-driving
     * average speed (Muqdisho traffic ~22 km/h) so ETA still renders honestly.
     */
    fun estimateStraightLine(
        fromLat: Double,
        fromLng: Double,
        toLat: Double,
        toLng: Double
    ): RouteResult {
        val d = haversineMeters(fromLat, fromLng, toLat, toLng)
        val road = d * 1.35
        val seconds = (road / (22_000.0 / 3600.0))
        val fallbackStep = NavigationStep(
            instruction = "Si toos ah u soco goobta",
            roadName = "Waddada Tooska ah",
            distanceMeters = road,
            durationSeconds = seconds,
            maneuverType = "continue",
            modifier = "straight",
            stepLat = toLat,
            stepLng = toLng
        )
        return RouteResult(
            road,
            seconds,
            listOf(Pair(fromLat, fromLng), Pair(toLat, toLng)),
            listOf(fallbackStep)
        )
    }

    fun parsePointsJson(pointsJson: String?): List<Pair<Double, Double>> {
        if (pointsJson.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(pointsJson)
            val list = mutableListOf<Pair<Double, Double>>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONArray(i) ?: continue
                if (p.length() >= 2) {
                    list.add(Pair(p.optDouble(0), p.optDouble(1)))
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Distance in meters from driver (pLat, pLng) to closest point on the route polyline.
     * Used for automatic off-route detection and rerouting.
     */
    fun distanceToPolylineMeters(
        pLat: Double,
        pLng: Double,
        polyline: List<Pair<Double, Double>>
    ): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        var minDistance = Double.MAX_VALUE
        for (pt in polyline) {
            val d = haversineMeters(pLat, pLng, pt.first, pt.second)
            if (d < minDistance) {
                minDistance = d
            }
        }
        return minDistance
    }

    fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    fun formatDistance(meters: Double): String =
        if (meters >= 1000) String.format(Locale.US, "%.1f km", meters / 1000.0)
        else "${meters.toInt()} m"

    fun formatEta(durationSeconds: Double): String {
        val minutes = (durationSeconds / 60.0).toInt()
        return if (minutes >= 60) {
            val h = minutes / 60
            val m = minutes % 60
            "${h}h ${m}m"
        } else if (minutes > 0) {
            "$minutes min"
        } else {
            "< 1 min"
        }
    }

    fun formatNextTurnPrompt(distanceMeters: Double, instruction: String): String {
        return if (distanceMeters >= 1000) {
            "${String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)} kadib $instruction"
        } else if (distanceMeters >= 40) {
            "${distanceMeters.toInt()}m kadib $instruction"
        } else {
            "$instruction hadda!"
        }
    }

    private fun Double.round4(): Double = Math.round(this * 10000.0) / 10000.0
}
