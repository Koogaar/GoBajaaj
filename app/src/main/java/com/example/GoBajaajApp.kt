package com.example

import android.app.Application
import com.example.somalitraffic.data.model.UserRole
import com.example.somalitraffic.data.repository.SomaliTrafficRepository
import com.example.somalitraffic.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Application-scoped GLOBAL POLLERS (spec: no FCM available, so we poll).
 * Runs for the whole process lifetime, so drivers/rider get ride updates and
 * the 24h Driver Pass expiry notice even when the UI is not on screen.
 */
class GoBajaajApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val repository: SomaliTrafficRepository by lazy { SomaliTrafficRepository(this) }

    private var pollJobs: List<Job> = emptyList()
    private val notifiedAssigned = mutableSetOf<String>()

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            org.maplibre.android.MapLibre.getInstance(this)
        } catch (_: Throwable) {
        }
        NotificationHelper.createChannels(this)
        startGlobalPollers()
    }

    fun startGlobalPollers() {
        pollJobs.forEach { it.cancel() }
        pollJobs = listOf(
            pollSubscriptionExpiry(),
            pollDriverAssignedRides(),
            pollRiderActiveRide()
        )
    }

    fun stopGlobalPollers() {
        pollJobs.forEach { it.cancel() }
        pollJobs = emptyList()
    }

    // --- 1) Driver Pass 24h expiry: check every minute + notify once ---
    private fun pollSubscriptionExpiry() = appScope.launch {
        var wasActive = false
        while (isActive) {
            delay(60_000)
            try {
                val user = repository.prefs.getCurrentUser() ?: run { wasActive = false; continue }
                if (user.role != UserRole.DRIVER) continue
                val res = repository.syncCurrentSubscription()
                val sub = res.getOrNull()
                val active = repository.isSubscriptionActive(sub)
                if (wasActive && !active) {
                    NotificationHelper.showSubscriptionExpiredNotification(this@GoBajaajApp)
                }
                wasActive = active
            } catch (_: Exception) {
            }
        }
    }

    // --- 2) Auto-assigned rides waiting for the driver (every 4s) ---
    private fun pollDriverAssignedRides() = appScope.launch {
        while (isActive) {
            delay(4_000)
            try {
                val user = repository.prefs.getCurrentUser() ?: continue
                if (user.role != UserRole.DRIVER) continue
                val rides = repository.fetchRidesForDriver("in.(assigned)").getOrDefault(emptyList())
                rides.forEach { ride ->
                    if (notifiedAssigned.add(ride.id)) {
                        NotificationHelper.showAssignedRideNotification(
                            context = this@GoBajaajApp,
                            rideId = ride.id,
                            pickup = ride.pickupAddress,
                            destination = ride.destinationAddress,
                            offer = "$${ride.riderOffer}"
                        )
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // --- 3) Rider's active ride status transitions (every 5s) ---
    private fun pollRiderActiveRide() = appScope.launch {
        while (isActive) {
            delay(5_000)
            try {
                val rideId = repository.prefs.getActiveRideId()
                if (rideId.isNullOrBlank()) continue
                val ride = repository.getRideById(rideId).getOrNull() ?: continue
                val st = ride.status.lowercase()
                val last = repository.prefs.getRideWatchStatus()
                if (last != st && !ride.isTerminal) {
                    when (st) {
                        "accepted" -> NotificationHelper.showRideAcceptedNotification(
                            this@GoBajaajApp,
                            driverName = ride.assignedDriverName ?: "Darawal",
                            plate = ride.assignedDriverPlate ?: "TR-1001",
                            etaMinutes = ride.etaMinutes
                        )
                        "arrived" -> NotificationHelper.showDriverArrivedNotification(
                            this@GoBajaajApp,
                            driverName = ride.assignedDriverName ?: "Darawal",
                            pickup = ride.pickupAddress
                        )
                        "in_progress" -> NotificationHelper.showTripStartedNotification(this@GoBajaajApp)
                    }
                }
                if (ride.isTerminal) {
                    if (st == "completed") {
                        NotificationHelper.showRideCompletedNotification(this@GoBajaajApp)
                    } else if (st == "cancelled" && last != "cancelled" && last != null) {
                        NotificationHelper.showRideCancelledNotification(this@GoBajaajApp)
                    }
                    repository.prefs.setActiveRideId(null)
                    repository.prefs.setRideWatchStatus(null)
                } else {
                    repository.prefs.setRideWatchStatus(st)
                }
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        lateinit var instance: GoBajaajApp
            private set
    }
}
