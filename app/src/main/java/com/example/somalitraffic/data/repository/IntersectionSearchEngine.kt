package com.example.somalitraffic.data.repository

import com.example.somalitraffic.data.model.Intersection
import java.util.Locale
import kotlin.math.*

data class SearchResultItem(
    val intersection: Intersection,
    val distanceMeters: Double? = null,
    val matchReason: String? = null
) {
    val formattedDistance: String?
        get() {
            if (distanceMeters == null) return null
            return if (distanceMeters < 1000) {
                "${distanceMeters.toInt()} m"
            } else {
                String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
            }
        }

    val hasRecentTrafficData: Boolean
        get() = !intersection.lastUpdated.isNullOrBlank() &&
                intersection.currentStatus.isNotBlank() &&
                intersection.currentStatus != "Lama yaqaan"
}

object IntersectionSearchEngine {
    private val SOMALI_PREFIXES = listOf(
        "isgoyska ", "isgoys ", "wadada ", "waddada ", "jidka ", "laamiga ", "road ", "street "
    )

    fun normalize(text: String): String {
        var clean = text.trim().lowercase(Locale.ROOT)
        // Normalize common punctuation to space except separators
        clean = clean.replace(Regex("[.,'\"`\\(\\)]"), " ")
        var changed = true
        while (changed) {
            val prev = clean
            for (prefix in SOMALI_PREFIXES) {
                if (clean.startsWith(prefix)) {
                    clean = clean.removePrefix(prefix).trim()
                }
            }
            changed = (clean != prev)
        }
        return clean.replace(Regex("\\s+"), " ").trim()
    }

    fun search(
        query: String,
        intersections: List<Intersection>,
        userLat: Double? = null,
        userLng: Double? = null
    ): List<SearchResultItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            return intersections.map { item ->
                val dist = if (userLat != null && userLng != null) {
                    calculateDistanceInMeters(userLat, userLng, item.latitude, item.longitude)
                } else null
                SearchResultItem(item, dist)
            }.sortedWith(
                compareBy<SearchResultItem> { it.distanceMeters ?: Double.MAX_VALUE }
                    .thenBy { it.intersection.name }
            )
        }

        val normQuery = normalize(trimmed)

        // Check for two-road intersection search: e.g. "Maka Al-Mukarama + 21 October" or "Maka Al-Mukarama & 21 October" or "Maka Al-Mukarama iyo 21 October"
        val delimiters = Regex("\\s*(?:\\+|&|/|\\biyo\\b|\\band\\b)\\s*", RegexOption.IGNORE_CASE)
        val parts = trimmed.split(delimiters).map { normalize(it) }.filter { it.isNotBlank() }
        val isTwoRoadQuery = parts.size >= 2

        val scoredResults = mutableListOf<Pair<SearchResultItem, Int>>()

        for (item in intersections) {
            val normName = normalize(item.name)
            val normRoads = normalize(item.roadNames)
            val normAliases = normalize(item.aliases)
            val normConnecting = normalize(item.connectingRoads)
            val normDistrict = normalize(item.district)
            val combined = "$normName $normRoads $normAliases $normConnecting $normDistrict"

            var score = -1
            var reason: String? = null

            if (isTwoRoadQuery) {
                val part1 = parts[0]
                val part2 = parts[1]
                val matchesPart1 = combined.contains(part1)
                val matchesPart2 = combined.contains(part2)
                if (matchesPart1 && matchesPart2) {
                    score = 100
                    reason = "Isgoys u dhexeeya ${parts[0]} iyo ${parts[1]}"
                }
            }

            if (score == -1) {
                // Exact / strong name match
                if (normName == normQuery || item.name.equals(trimmed, ignoreCase = true)) {
                    score = 90
                    reason = "Isgoys toos ah"
                } else if (normAliases.split(",").map { normalize(it) }.any { it == normQuery }) {
                    score = 85
                    reason = "Magac kale / Summad"
                } else if (normName.startsWith(normQuery)) {
                    score = 80
                    reason = "Magaca isgoyska"
                } else if (normName.contains(normQuery)) {
                    score = 75
                    reason = "Magaca isgoyska"
                } else if (normRoads.contains(normQuery)) {
                    score = 70
                    reason = "Waddada ku taal"
                } else if (normConnecting.contains(normQuery)) {
                    score = 65
                    reason = "Waddooyinka isku xirma"
                } else if (normAliases.contains(normQuery)) {
                    score = 60
                    reason = "Aagga / Isgoyska"
                } else if (normDistrict.contains(normQuery)) {
                    score = 50
                    reason = "Degmada ${item.district}"
                } else {
                    // Token-based fuzzy check
                    val tokens = normQuery.split(" ").filter { it.length > 1 }
                    if (tokens.isNotEmpty() && tokens.all { combined.contains(it) }) {
                        score = 40
                        reason = "Isku dhigma"
                    }
                }
            }

            if (score > 0) {
                val dist = if (userLat != null && userLng != null) {
                    calculateDistanceInMeters(userLat, userLng, item.latitude, item.longitude)
                } else null
                scoredResults.add(Pair(SearchResultItem(item, dist, reason), score))
            }
        }

        // Sort primarily by score descending, secondarily by distance ascending
        return scoredResults
            .sortedWith(
                compareByDescending<Pair<SearchResultItem, Int>> { it.second }
                    .thenBy { it.first.distanceMeters ?: Double.MAX_VALUE }
            )
            .map { it.first }
    }

    fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
