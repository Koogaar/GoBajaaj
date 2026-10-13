package com.example.somalitraffic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RideStatus(val somaliTitle: String) {
    SEARCHING("Raadinaya Darawal..."),
    BIDDING("Dalabyo Darawal ayaa soo socda"),
    DRIVER_ASSIGNED("Darawal la ku qiriyay — wuu aqbalayaa"),
    GOING_TO_PICKUP("Darawalka ayaa kusoo socda"),
    ARRIVED("Darawalku wuu soo gaaray goobta"),
    ON_TRIP("Safarku wuu socdaa"),
    COMPLETED("Safarku wuu dhammaady"),
    CANCELLED("Dalabkii waa la joojiyay");

    companion object {
        fun fromString(status: String?): RideStatus {
            return entries.find { it.name.equals(status, ignoreCase = true) } ?: SEARCHING
        }

        /** Maps DB lifecycle values (incl. legacy ones) to the ride state machine. */
        fun fromDb(status: String?): RideStatus = when (status?.lowercase()) {
            "pending" -> SEARCHING
            "searching" -> SEARCHING
            "assigned" -> DRIVER_ASSIGNED
            "accepted" -> GOING_TO_PICKUP
            "arrived" -> ARRIVED
            "in_progress" -> ON_TRIP
            "completed" -> COMPLETED
            "cancelled" -> CANCELLED
            else -> SEARCHING
        }
    }
}

/** Road route from OSRM/OSRM-compatible engine (spec §7 - no Google required). */
data class RouteInfo(
    val distanceMeters: Double,
    val durationSeconds: Double,
    /** Compose-ready JSON string: [[lat,lng],...] for the map route layer */
    val pointsJson: String,
    val fromLat: Double,
    val fromLng: Double,
    val toLat: Double,
    val toLng: Double,
    val steps: List<com.example.somalitraffic.data.remote.OsmRoutingService.NavigationStep> = emptyList()
) {
    val etaMinutes: Int
        get() = (durationSeconds / 60.0).coerceAtLeast(1.0).toInt()
}

/** Driver approach states derived from distance (DB stores no DRIVER_NEARBY row). */
enum class ApproachState(val somaliText: String) {
    ON_THE_WAY("Darawalka ayaa kuu soo socda"),
    NEARBY("Darawalku wuu kuu dhow yahay"),
    ARRIVED("Darawalku wuu yimid");

    companion object {
        fun fromDistanceMeters(m: Double?, arrived: Boolean): ApproachState {
            if (arrived) return ARRIVED
            if (m == null) return ON_THE_WAY
            return if (m <= 150.0) NEARBY else ON_THE_WAY
        }
    }
}

enum class VehicleType(val displayName: String, val basePrice: Double, val iconCode: String) {
    BAJAAJ("Bajaj", 1.50, "two_wheeler"),
    CAR("Car", 2.50, "directions_car"),
    SUV("SUV", 4.00, "airport_shuttle");

    companion object {
        fun fromString(type: String?): VehicleType {
            return entries.find { it.name.equals(type, ignoreCase = true) } ?: BAJAAJ
        }
    }
}

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey val id: String,
    val riderId: String,
    val riderName: String,
    val riderPhone: String,
    val pickupAddress: String,
    val destinationAddress: String,
    val vehicleType: String = "BAJAAJ",
    val riderOffer: Double = 1.50,
    val agreedFare: Double? = null,
    val status: String = "pending",
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val assignedDriverPhone: String? = null,
    val assignedDriverPlate: String? = null,
    val assignedDriverRating: Double = 4.8,
    val etaMinutes: Int = 5,
    val createdAt: String = "",
    val updatedAt: String = "",
    // --- Live tracking (GoBajaaj backend) ---
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val destLat: Double? = null,
    val destLng: Double? = null,
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val driverHeading: Double? = null,
    val driverSpeed: Double? = null,
    val driverLocationAt: String? = null,
    val assignedAt: String? = null
) {
    val isTerminal: Boolean
        get() = status == "completed" || status == "cancelled"

    val hasLiveDriverLocation: Boolean
        get() = driverLat != null && driverLng != null
}

data class RideRequest(
    val id: String,
    val riderId: String,
    val riderName: String,
    val riderPhone: String,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val destinationAddress: String,
    val destinationLat: Double,
    val destinationLng: Double,
    val vehicleType: VehicleType = VehicleType.BAJAAJ,
    val riderOffer: Double,
    val agreedFare: Double? = null,
    val status: RideStatus = RideStatus.SEARCHING,
    val assignedDriverId: String? = null,
    val assignedDriverName: String? = null,
    val assignedDriverPhone: String? = null,
    val assignedDriverPlate: String? = null,
    val assignedDriverRating: Double = 4.8,
    val etaMinutes: Int = 5,
    val createdAt: String = ""
)

data class DriverBid(
    val id: String,
    val rideId: String,
    val driverId: String,
    val driverName: String,
    val driverPhone: String,
    val driverPlate: String,
    val driverRating: Double = 4.8,
    val totalTrips: Int = 120,
    val vehicleModel: String = "Bajaj TVS King",
    val distanceKm: Double = 0.8,
    val etaMinutes: Int = 4,
    val offeredFare: Double,
    val isCounterOffer: Boolean = false,
    val status: String = "pending" // pending, accepted, declined
)

data class RoadClosure(
    val id: String,
    val roadName: String,
    val area: String,
    val latitude: Double,
    val longitude: Double,
    val reason: String,
    val startTime: String,
    val expectedEndTime: String,
    val status: String = "ACTIVE", // PLANNED, ACTIVE, ENDED
    val description: String = "",
    val createdAt: String = ""
)

data class LostPhoneReport(
    val id: String,
    val userId: String,
    val reporterPhone: String,
    val phoneBrand: String,
    val phoneModel: String,
    val phoneColor: String,
    val imeiMasked: String? = null,
    val lostDate: String,
    val lostLocation: String,
    val description: String,
    val status: String = "ACTIVE", // PENDING, ACTIVE, FOUND, RETURNED, CLOSED
    val photoUrl: String? = null,
    val createdAt: String = ""
)
