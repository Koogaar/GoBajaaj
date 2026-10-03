package com.example.somalitraffic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RideStatus(val somaliTitle: String) {
    SEARCHING("Raadinaya Darawal..."),
    BIDDING("Dalabyo Darawal ayaa soo socda"),
    DRIVER_ASSIGNED("Darawalku wuu soo socdaa"),
    GOING_TO_PICKUP("Darawalka ayaa kusoo socda"),
    ARRIVED("Darawalku wuu soo gaaray goobta"),
    ON_TRIP("Safarku wuu socdaa"),
    COMPLETED("Safarku wuu dhammaaday"),
    CANCELLED("Dalabkii waa la joojiyay");

    companion object {
        fun fromString(status: String?): RideStatus {
            return entries.find { it.name.equals(status, ignoreCase = true) } ?: SEARCHING
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
    val updatedAt: String = ""
)

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
