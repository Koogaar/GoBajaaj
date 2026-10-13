package com.example.somalitraffic.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

/**
 * Continuous GPS stream for the DRIVER phone while a ride is active.
 * Drives: driver_presence (matching) + bajaj_rides live location (rider view).
 *
 * Balanced-power priority with 4s interval: good enough for a Bajaaj moving
 * through a city while staying battery friendly (spec: high frequency during
 * the trip only, stopped when the ride ends).
 */
class DriverLocationTracker(context: Context) {

    private val fused = LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start(onLocation: (Location) -> Unit) {
        stop()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_500L)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(2f)
            .setWaitForAccurateLocation(false)
            .build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { loc ->
                    if (isUsable(loc)) onLocation(loc)
                }
            }
        }
        callback = cb
        try {
            fused.requestLocationUpdates(request, cb, android.os.Looper.getMainLooper())
        } catch (_: Throwable) {
            callback = null
        }
    }

    fun stop() {
        callback?.let {
            try {
                fused.removeLocationUpdates(it)
            } catch (_: Exception) {
            }
        }
        callback = null
    }

    private fun isUsable(loc: Location): Boolean =
        loc.latitude in -90.0..90.0 && loc.longitude in -180.0..180.0 &&
            (loc.accuracy <= 120f || loc.accuracy == 0f)
}
