package com.example.somalitraffic.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.somalitraffic.data.model.UserProfile
import com.example.somalitraffic.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("gobajaaj_prefs", Context.MODE_PRIVATE)

    private val _currentUserFlow = MutableStateFlow(getCurrentUser())
    val currentUserFlow: StateFlow<UserProfile?> = _currentUserFlow.asStateFlow()

    fun isOnboardingCompleted(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    fun getCurrentUser(): UserProfile? {
        val id = prefs.getString(KEY_USER_ID, null) ?: return null
        val name = prefs.getString(KEY_USER_NAME, "") ?: ""
        val phone = prefs.getString(KEY_USER_PHONE, "") ?: ""
        val roleStr = prefs.getString(KEY_USER_ROLE, UserRole.DRIVER.code)
        val role = UserRole.fromString(roleStr)
        val status = prefs.getString(KEY_USER_STATUS, "active") ?: "active"
        val plate = prefs.getString(KEY_USER_PLATE, null)
        val vehicle = prefs.getString(KEY_USER_VEHICLE, null)
        return UserProfile(id, name, phone, role, status, null, null, plate, vehicle)
    }

    fun saveUserSession(user: UserProfile, token: String? = null) {
        val editor = prefs.edit()
            .putString(KEY_USER_ID, user.id)
            .putString(KEY_USER_NAME, user.fullName)
            .putString(KEY_USER_PHONE, user.phoneNumber)
            .putString(KEY_USER_ROLE, user.role.code)
            .putString(KEY_USER_STATUS, user.status)
        if (user.plateNumber != null) {
            editor.putString(KEY_USER_PLATE, user.plateNumber)
        }
        if (user.vehicleModel != null) {
            editor.putString(KEY_USER_VEHICLE, user.vehicleModel)
        }
        editor.apply()
        if (token != null) {
            prefs.edit().putString(KEY_AUTH_TOKEN, token).apply()
        }
        _currentUserFlow.value = user
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_AUTH_TOKEN, null)
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_PHONE)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_STATUS)
            .remove(KEY_USER_PLATE)
            .remove(KEY_USER_VEHICLE)
            .remove(KEY_AUTH_TOKEN)
            .apply()
        _currentUserFlow.value = null
    }

    fun getSupabaseUrl(): String {
        val buildUrl = BuildConfig.SUPABASE_URL
        if (buildUrl.isNotBlank() && !buildUrl.contains("example-traffic-somalia")) {
            return buildUrl
        }
        val saved = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
        if (saved.isNotBlank() && !saved.contains("example-traffic-somalia")) {
            return saved
        }
        return DEFAULT_SUPABASE_URL
    }

    fun getSupabaseKey(): String {
        val buildKey = BuildConfig.SUPABASE_ANON_KEY
        if (buildKey.isNotBlank() && !buildKey.contains("dummy_anon_key")) {
            return buildKey
        }
        val saved = prefs.getString(KEY_SUPABASE_KEY, "") ?: ""
        if (saved.isNotBlank() && !saved.contains("dummy_anon_key")) {
            return saved
        }
        return DEFAULT_SUPABASE_ANON_KEY
    }

    fun isSupabaseConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        return url.isNotBlank() && !url.contains("example-traffic-somalia") &&
               key.isNotBlank() && !key.contains("dummy_anon_key")
    }

    fun saveSupabaseConfig(url: String, key: String) {
        prefs.edit()
            .putString(KEY_SUPABASE_URL, url)
            .putString(KEY_SUPABASE_KEY, key)
            .apply()
    }

    fun getRegisteredDeviceId(): String? {
        return prefs.getString(KEY_REGISTERED_DEVICE_ID, null)
    }

    fun setRegisteredDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_REGISTERED_DEVICE_ID, deviceId).apply()
    }

    fun saveFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun getRegisteredPlates(): Set<String> {
        val defaultPlates = setOf("TR-1001", "TR-2020", "B-8801", "TR-4820")
        val saved = prefs.getStringSet(KEY_REGISTERED_PLATES, null)
        return if (saved.isNullOrEmpty()) {
            prefs.edit().putStringSet(KEY_REGISTERED_PLATES, defaultPlates).apply()
            defaultPlates
        } else {
            saved
        }
    }

    fun normalizePlate(plate: String): String {
        return plate.trim().uppercase().replace(" ", "").replace("-", "")
    }

    fun isPlateRegistered(plate: String): Boolean {
        val clean = normalizePlate(plate)
        if (clean.isBlank()) return false
        val allPlates = getRegisteredPlates().map { normalizePlate(it) }
        return allPlates.contains(clean)
    }

    fun registerPlate(plate: String): Boolean {
        val clean = plate.trim().uppercase()
        if (isPlateRegistered(clean)) {
            return false
        }
        val current = getRegisteredPlates().toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet(KEY_REGISTERED_PLATES, current).apply()
        return true
    }

    // --- Phone Number Registry & E.164 Normalization ---
    fun toE164(phone: String): String {
        val trimmed = phone.trim()
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isBlank()) return ""
        return when {
            digits.startsWith("252") && digits.length >= 10 -> "+$digits"
            digits.startsWith("0") -> "+252" + digits.removePrefix("0")
            else -> "+252$digits"
        }
    }

    fun formatDisplayPhone(phone: String): String {
        val e164 = toE164(phone)
        if (e164.startsWith("+252") && e164.length >= 7) {
            val rest = e164.removePrefix("+252")
            return "+252 $rest"
        }
        return if (phone.isNotBlank()) phone else ""
    }

    fun normalizePhone(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.startsWith("252") && digits.length >= 10 -> digits.removePrefix("252").removePrefix("0")
            digits.startsWith("0") -> digits.removePrefix("0")
            else -> digits
        }
    }

    fun getRegisteredPhones(): Set<String> {
        val defaultPhones = emptySet<String>()
        val saved = prefs.getStringSet(KEY_REGISTERED_PHONES, null)
        return saved ?: emptySet()
    }

    fun isPhoneRegistered(phone: String): Boolean {
        val clean = normalizePhone(phone)
        if (clean.isBlank()) return false
        val allPhones = getRegisteredPhones().map { normalizePhone(it) }
        return allPhones.contains(clean)
    }

    fun registerPhone(phone: String): Boolean {
        val clean = normalizePhone(phone)
        if (clean.isBlank()) return false
        val current = getRegisteredPhones().toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet(KEY_REGISTERED_PHONES, current).apply()
        return true
    }

    // --- Device Binding & Anti-Sharing Security ---
    fun getLocalDeviceId(): String {
        var id = prefs.getString(KEY_LOCAL_DEVICE_ID, null)
        if (id.isNullOrBlank()) {
            id = "DEV-" + android.os.Build.MODEL.filter { it.isLetterOrDigit() }.take(6).uppercase() + "-" + java.util.UUID.randomUUID().toString().take(6).uppercase()
            prefs.edit().putString(KEY_LOCAL_DEVICE_ID, id).apply()
        }
        return id
    }

    fun getLocalDeviceModel(): String {
        val manufacturer = android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }
        val model = android.os.Build.MODEL
        return "$manufacturer $model".trim().ifBlank { "Android Device" }
    }

    fun bindUserToCurrentDevice(identifier: String) {}
    fun getBoundDeviceForUser(identifier: String): Any? = null

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USER_STATUS = "user_status"
        private const val KEY_USER_PLATE = "user_plate"
        private const val KEY_USER_VEHICLE = "user_vehicle"
        private const val KEY_REGISTERED_PLATES = "registered_plates"
        private const val KEY_REGISTERED_PHONES = "registered_phone_numbers"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"
        private const val KEY_REGISTERED_DEVICE_ID = "registered_device_id"
        private const val KEY_LOCAL_DEVICE_ID = "local_device_hardware_id"
        private const val KEY_BOUND_DEVICE_PREFIX = "user_bound_dev_"
        private const val KEY_FCM_TOKEN = "fcm_token"

        // Default or user configurable Supabase endpoint
        const val DEFAULT_SUPABASE_URL = "https://lcigjbtprkloopvzpaxy.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM"
    }
}
