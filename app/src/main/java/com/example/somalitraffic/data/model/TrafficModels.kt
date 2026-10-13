package com.example.somalitraffic.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intersections")
data class Intersection(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val allowedRadius: Int = 400,
    val city: String = "Muqdisho",
    val district: String = "Hodan",
    val currentStatus: String = "Furan", // Furan, Dhexdhexaad, Mashquul, Saxmad badan
    val assignedReporterId: String? = null,
    val status: String = "active",
    val lastUpdated: String? = null,
    val roadNames: String = "",
    val aliases: String = "",
    val connectingRoads: String = "",
    val normalizedName: String = "",
    val latestVerifiedReport: String? = null,
    val recentReportCount: Int = 0,
    val hasAccident: Boolean = false,
    val hasClosure: Boolean = false,
    val hasHazard: Boolean = false,
    val hazardDescription: String? = null,
    val alternativeRoute: String? = null,
    val verifiedByReporter: Boolean = true
)

@Entity(tableName = "traffic_reports")
data class TrafficReport(
    @PrimaryKey val id: String,
    val intersectionId: String,
    val reporterId: String,
    val status: String, // Furan, Dhexdhexaad, Mashquul, Saxmad badan
    val cause: String? = null,
    val notes: String? = null,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val distanceFromIntersection: Double? = null,
    val verificationStatus: String = "verified", // verified, outside_radius
    val createdAt: String
)

@Entity(tableName = "traffic_incidents")
data class TrafficIncident(
    @PrimaryKey val id: String,
    val title: String,
    val category: String, // Saxmad, Shil, Waddo xiran, Caqabad, Khatar waddo
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val severity: String, // Dhexdhexaad, Sare, Degdeg ah
    val status: String = "active",
    val description: String? = null,
    val createdAt: String
)

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceName: String,
    val deviceModel: String,
    val platform: String = "Android", // "Android" or "iOS"
    val appVersion: String = "3.3",
    val deviceFingerprint: String = "",
    val appInstallationId: String? = null,
    val pushToken: String? = null,
    val isProtected: Boolean = true,
    val isLost: Boolean = false,
    val lostAt: String? = null,
    val foundAt: String? = null,
    val batteryLevel: Int? = 85,
    val isOnline: Boolean = true,
    val networkStatus: String = "Online", // "WiFi", "Cellular", "Offline"
    val locationPermissionStatus: String = "Granted", // "Granted", "Coarse", "Denied"
    val lastLatitude: Double? = null,
    val lastLongitude: Double? = null,
    val lastAccuracy: Double? = null,
    val lastLocationAddress: String? = "KM4, Mogadishu",
    val lastSeen: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Entity(tableName = "device_locations")
data class DeviceLocation(
    @PrimaryKey val id: String,
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double? = null,
    val source: String = "GPS", // "GPS", "Network", "Cell"
    val address: String? = null,
    val batteryLevel: Int? = null,
    val recordedAt: String
)

@Entity(tableName = "device_recovery_events")
data class DeviceRecoveryEvent(
    @PrimaryKey val id: String,
    val deviceId: String,
    val userId: String,
    val eventType: String, // "PROTECTION_ENABLED", "PROTECTION_DISABLED", "MARKED_LOST", "MARKED_FOUND", "FIND_NOW_REQUESTED", "ALARM_TRIGGERED"
    val description: String,
    val metadata: String? = null,
    val createdAt: String
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey val id: String,
    val userId: String,
    val planName: String = "Driver Pass",
    val amount: Double = 1.0,
    val currency: String = "USD",
    val status: String = "pending", // active, expired, cancelled, pending, rejected
    val paymentMethod: String = "MANUAL",
    val senderPhone: String? = null,
    val destinationPhone: String? = null,
    val paymentReference: String? = null,
    val startDate: String,
    val endDate: String,
    val autoRenew: Boolean = false,
    val reviewedBy: String? = null,
    val reviewedAt: String? = null,
    val rejectionReason: String? = null
)

@Entity(tableName = "support_tickets")
data class SupportTicket(
    @PrimaryKey val id: String,
    val userId: String,
    val ticketNumber: Int = 1001,
    val category: String, // Koontada, Lacag-bixinta, Saxmadda, Ilaalinta Taleefanka, Wariyaha, Kale
    val priority: String = "Dhexdhexaad", // Hoose, Dhexdhexaad, Sare, Degdeg ah
    val subject: String,
    val message: String,
    val status: String = "Furan", // Furan, Socda, La xaliyay, Xiran
    val assignedAgent: String? = null,
    val createdAt: String
)

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey val id: String,
    val actorId: String?,
    val actorName: String,
    val action: String,
    val targetType: String,
    val targetId: String?,
    val metadata: String? = null,
    val createdAt: String
)

data class TrafficAlert(
    val id: String,
    val title: String,
    val category: String,
    val location: String,
    val severity: String,
    val timeAgo: String,
    val timestamp: String,
    val description: String
)

