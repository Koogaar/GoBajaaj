package com.example.somalitraffic.data.model

enum class UserRole(val code: String, val titleSomali: String) {
    DRIVER("driver", "Wade Gaadiid (Driver)"),
    PASSENGER("passenger", "Rakaab (Passenger)"),
    REPORTER("reporter", "Wariye Gaadiid (Traffic Reporter)"),
    SUPPORT_AGENT("support_agent", "Shaqaalaha Taageerada (Support Agent)"),
    USER_MANAGER("user_manager", "Maamulaha Isticmaalayaasha"),
    SUBSCRIPTION_MANAGER("subscription_manager", "Maamulaha Lacag-bixinta"),
    TRAFFIC_MANAGER("traffic_manager", "Maamulaha Saxmadda"),
    REPORTER_MANAGER("reporter_manager", "Maamulaha Wariyeyaasha"),
    SUPER_ADMIN("super_admin", "Maamulaha Guud (Super Admin)");

    companion object {
        fun fromString(role: String?): UserRole {
            if (role == null) return PASSENGER
            val clean = role.trim().lowercase()
            return when {
                clean == "driver" || clean == "wade" -> DRIVER
                clean == "reporter" || clean == "wariye" -> REPORTER
                clean == "support_agent" -> SUPPORT_AGENT
                clean == "user_manager" -> USER_MANAGER
                clean == "subscription_manager" -> SUBSCRIPTION_MANAGER
                clean == "traffic_manager" -> TRAFFIC_MANAGER
                clean == "reporter_manager" -> REPORTER_MANAGER
                clean == "super_admin" || clean == "admin" -> SUPER_ADMIN
                else -> PASSENGER
            }
        }
    }
}

enum class TrafficStatus(val somaliName: String, val hexColor: Long, val hexColorString: String) {
    FURAN("Furan", 0xFF16A34A, "#16A34A"),          // Furan - Cagaar (Green)
    NORMAL("Normal", 0xFFEAB308, "#EAB308"),        // Normal - Jaale (Yellow)
    JAAM("Jaam", 0xFFE50914, "#E50914"),            // Jaam (Culus) - Gaduud (Red)
    XERAN("Xeran", 0xFFE50914, "#E50914");           // Xeran / Xiran - Gaduud (Red)

    val color: androidx.compose.ui.graphics.Color
        get() = androidx.compose.ui.graphics.Color(hexColor)

    companion object {
        /** Parses one of the 4 live statuses (Furan, Normal, Jaam, Xeran),
         *  keeping legacy synonyms working during/after migration. */
        fun fromString(status: String?): TrafficStatus {
            if (status == null) return FURAN
            val clean = status.trim().lowercase()
            return when {
                clean.contains("furan") || clean.contains("clear") || clean.contains("open") -> FURAN
                clean.contains("normal") || clean.contains("dhexdhexaad") || clean.contains("medium") -> NORMAL
                clean.contains("jaam") || clean.contains("mashquul") || clean.contains("busy") -> JAAM
                clean.contains("xeran") || clean.contains("xiran") || clean.contains("closed") || clean.contains("blocked") || clean.contains("madow") -> XERAN
                clean.contains("saxmad") || clean.contains("heavy") || clean.contains("shil") || clean.contains("gaduud") -> XERAN
                else -> FURAN
            }
        }
    }
}

enum class SubscriptionStatus(val somaliName: String) {
    ACTIVE("Shaqaynaysa"),
    EXPIRED("Dhacday"),
    CANCELLED("La joojiyay"),
    PENDING("Sugaysa"),
    FAILED("Fashilantay");

    companion object {
        fun fromString(status: String?): SubscriptionStatus {
            return entries.find { it.name.equals(status, ignoreCase = true) } ?: PENDING
        }
    }
}

data class UserProfile(
    val id: String,
    val fullName: String,
    val phoneNumber: String,
    val role: UserRole = UserRole.DRIVER,
    val status: String = "active",
    val avatarUrl: String? = null,
    val createdAt: String? = null,
    val plateNumber: String? = null,
    val vehicleModel: String? = null
)

data class OtpSendResult(
    val phoneE164: String,
    val message: String,
    val isProviderDisabled: Boolean = false,
    val testOtpCode: String? = null
)
