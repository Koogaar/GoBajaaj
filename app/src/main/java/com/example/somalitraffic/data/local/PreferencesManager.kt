package com.example.somalitraffic.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.somalitraffic.data.model.DriverExpenseEntry
import com.example.somalitraffic.data.model.SavedAddress
import com.example.somalitraffic.data.model.UserProfile
import com.example.somalitraffic.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

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
        val roleStr = prefs.getString(KEY_USER_ROLE, UserRole.PASSENGER.code)
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

    fun saveRefreshToken(refreshToken: String?) {
        if (refreshToken.isNullOrBlank()) return
        prefs.edit().putString(KEY_REFRESH_TOKEN, refreshToken).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    // --- Active ride watched by the global poller (survives process death) ---
    fun setActiveRideId(rideId: String?) {
        if (rideId == null) {
            prefs.edit().remove(KEY_ACTIVE_RIDE_ID).remove(KEY_RIDE_WATCH_STATUS).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_RIDE_ID, rideId).apply()
        }
    }

    fun getActiveRideId(): String? = prefs.getString(KEY_ACTIVE_RIDE_ID, null)

    /** Last observed status of the active ride, used to detect transitions. */
    fun getRideWatchStatus(): String? = prefs.getString(KEY_RIDE_WATCH_STATUS, null)

    fun setRideWatchStatus(status: String?) {
        if (status == null) prefs.edit().remove(KEY_RIDE_WATCH_STATUS).apply()
        else prefs.edit().putString(KEY_RIDE_WATCH_STATUS, status).apply()
    }

    // --- Subscription expiry stamp (global poller checks it every minute) ---
    fun getSubscriptionExpiryMs(): Long = prefs.getLong(KEY_SUB_EXPIRY_MS, 0L)

    fun setSubscriptionExpiryMs(ms: Long) {
        prefs.edit().putLong(KEY_SUB_EXPIRY_MS, ms).apply()
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
            .remove(KEY_REFRESH_TOKEN)
            .apply()
        _currentUserFlow.value = null
    }

    private fun isInvalidSupabaseUrl(url: String): Boolean {
        return url.isBlank() || 
               url.contains("example-traffic-somalia", ignoreCase = true) || 
               url.contains("YOUR_SUPABASE_PROJECT_REF", ignoreCase = true) ||
               url.contains("your_supabase_project_ref", ignoreCase = true) ||
               !url.startsWith("https://")
    }

    private fun isInvalidSupabaseKey(key: String): Boolean {
        return key.isBlank() || 
               key.contains("dummy_anon_key", ignoreCase = true) || 
               key.contains("YOUR_SUPABASE_ANON_KEY", ignoreCase = true)
    }

    fun getSupabaseUrl(): String {
        val buildUrl = BuildConfig.SUPABASE_URL
        if (!isInvalidSupabaseUrl(buildUrl)) {
            return buildUrl
        }
        val saved = prefs.getString(KEY_SUPABASE_URL, "") ?: ""
        if (!isInvalidSupabaseUrl(saved)) {
            return saved
        }
        return DEFAULT_SUPABASE_URL
    }

    fun getSupabaseKey(): String {
        val buildKey = BuildConfig.SUPABASE_ANON_KEY
        if (!isInvalidSupabaseKey(buildKey)) {
            return buildKey
        }
        val saved = prefs.getString(KEY_SUPABASE_KEY, "") ?: ""
        if (!isInvalidSupabaseKey(saved)) {
            return saved
        }
        return DEFAULT_SUPABASE_ANON_KEY
    }

    fun isSupabaseConfigured(): Boolean {
        val url = getSupabaseUrl()
        val key = getSupabaseKey()
        return !isInvalidSupabaseUrl(url) && !isInvalidSupabaseKey(key)
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

    // --- Saved Digital House Addresses (Guri Number MG-HODAN-125) ---
    fun getSavedAddresses(): List<SavedAddress> {
        val jsonStr = prefs.getString(KEY_SAVED_ADDRESSES, null) ?: return emptyList()
        val list = mutableListOf<SavedAddress>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedAddress(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        digitalCode = obj.optString("digitalCode", "MG-HODAN-101"),
                        label = obj.optString("label", "Gurigeyga"),
                        district = obj.optString("district", "Hodan"),
                        somaliDirections = obj.optString("somaliDirections", ""),
                        latitude = obj.optDouble("latitude", 2.0371),
                        longitude = obj.optDouble("longitude", 45.3283),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun saveAddress(address: SavedAddress) {
        val current = getSavedAddresses().toMutableList()
        current.removeAll { it.id == address.id || it.digitalCode.equals(address.digitalCode, ignoreCase = true) }
        current.add(0, address)
        val arr = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("digitalCode", item.digitalCode)
                put("label", item.label)
                put("district", item.district)
                put("somaliDirections", item.somaliDirections)
                put("latitude", item.latitude)
                put("longitude", item.longitude)
                put("createdAt", item.createdAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ADDRESSES, arr.toString()).apply()
    }

    fun deleteAddress(id: String) {
        val current = getSavedAddresses().filterNot { it.id == id }
        val arr = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("digitalCode", item.digitalCode)
                put("label", item.label)
                put("district", item.district)
                put("somaliDirections", item.somaliDirections)
                put("latitude", item.latitude)
                put("longitude", item.longitude)
                put("createdAt", item.createdAt)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_SAVED_ADDRESSES, arr.toString()).apply()
    }

    // --- Driver Expenses & Income Ledger (Buugga Dhaqaalaha) ---
    fun getDriverExpenses(): List<DriverExpenseEntry> {
        val jsonStr = prefs.getString(KEY_DRIVER_EXPENSES, null) ?: return emptyList()
        val list = mutableListOf<DriverExpenseEntry>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DriverExpenseEntry(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        type = obj.optString("type", "expense"),
                        category = obj.optString("category", "shidaal"),
                        amount = obj.optDouble("amount", 0.0),
                        notes = obj.optString("notes", ""),
                        date = obj.optString("date", ""),
                        timestampMs = obj.optLong("timestampMs", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedByDescending { it.timestampMs }
    }

    fun saveDriverExpense(entry: DriverExpenseEntry) {
        val current = getDriverExpenses().toMutableList()
        current.removeAll { it.id == entry.id }
        current.add(0, entry)
        val arr = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("category", item.category)
                put("amount", item.amount)
                put("notes", item.notes)
                put("date", item.date)
                put("timestampMs", item.timestampMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DRIVER_EXPENSES, arr.toString()).apply()
    }

    fun deleteDriverExpense(id: String) {
        val current = getDriverExpenses().filterNot { it.id == id }
        val arr = JSONArray()
        for (item in current) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("category", item.category)
                put("amount", item.amount)
                put("notes", item.notes)
                put("date", item.date)
                put("timestampMs", item.timestampMs)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DRIVER_EXPENSES, arr.toString()).apply()
    }

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
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_SUPABASE_URL = "supabase_url"
        private const val KEY_SUPABASE_KEY = "supabase_key"
        private const val KEY_REGISTERED_DEVICE_ID = "registered_device_id"
        private const val KEY_LOCAL_DEVICE_ID = "local_device_hardware_id"
        private const val KEY_BOUND_DEVICE_PREFIX = "user_bound_dev_"
        private const val KEY_FCM_TOKEN = "fcm_token"
        private const val KEY_ACTIVE_RIDE_ID = "active_ride_id"
        private const val KEY_RIDE_WATCH_STATUS = "ride_watch_status"
        private const val KEY_SUB_EXPIRY_MS = "sub_expiry_ms"
        private const val KEY_SAVED_ADDRESSES = "saved_addresses_json"
        private const val KEY_DRIVER_EXPENSES = "driver_expenses_json"

        // Default or user configurable Supabase endpoint
        const val DEFAULT_SUPABASE_URL = "https://lcigjbtprkloopvzpaxy.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxjaWdqYnRwcmtsb29wdnpwYXh5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODkyOTA5MDUsImV4cCI6MjEwNDg2NjkwNX0.5cx8A-3Hhh4Z5AuYDt0-AWdMYmAe_x3gz6DVqWY8IuM"
    }
}
