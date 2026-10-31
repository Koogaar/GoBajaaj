package com.example.somalitraffic.data.model

import java.util.UUID

data class SavedAddress(
    val id: String = UUID.randomUUID().toString(),
    val digitalCode: String, // e.g. "MG-HODAN-125"
    val label: String = "Gurigeyga", // Gurigeyga, Shaqada, Baqaalka, etc.
    val district: String = "Hodan",
    val somaliDirections: String = "",
    val latitude: Double,
    val longitude: Double,
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullDisplayTitle: String
        get() = "$label ($digitalCode)"

    val fullFormattedAddress: String
        get() = if (somaliDirections.isNotBlank()) "[$digitalCode] $somaliDirections ($district)" else "[$digitalCode] $district"
}

data class DriverExpenseEntry(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "income" or "expense"
    val category: String, // "shidaal", "pass", "dayactir", "kiro", "cunto", "safar_gobajaaj", "safar_caadi", "kale"
    val categoryLabelSomali: String = when (category.lowercase()) {
        "shidaal" -> "Batrool / Shidaal"
        "pass" -> "GoBajaaj Driver Pass"
        "dayactir" -> "Dayactir & Farsamo"
        "kiro" -> "Kirada Bajaajta"
        "cunto" -> "Cunto & Shaah"
        "safar_gobajaaj" -> "Safar GoBajaaj"
        "safar_caadi" -> "Safar Caadi ah"
        else -> "Kharash / Dakhli Kale"
    },
    val amount: Double,
    val notes: String = "",
    val date: String, // "yyyy-MM-dd"
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isIncome: Boolean get() = type.equals("income", ignoreCase = true)
}

enum class FinanceTimeframe(val labelSomali: String, val subtitleSomali: String) {
    DAILY("Maalinle", "Xisaabta Maanta"),
    WEEKLY("Isbuucle", "7-dii Maalmood"),
    MONTHLY("Bille", "Bishan Socota"),
    YEARLY("Sanadle", "Sanadka Dhan")
}

