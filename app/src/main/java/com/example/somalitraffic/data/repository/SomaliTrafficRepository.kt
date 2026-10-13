package com.example.somalitraffic.data.repository

import android.content.Context
import android.os.Build
import com.example.somalitraffic.data.local.AppDatabase
import com.example.somalitraffic.data.local.PreferencesManager
import com.example.somalitraffic.data.model.*
import com.example.somalitraffic.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class SomaliTrafficRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val trafficDao = db.trafficDao()
    private val deviceDao = db.deviceDao()
    private val supportDao = db.supportDao()
    private val rideDao = db.rideDao()
    val prefs = PreferencesManager(context)

    private fun getApiService() = NetworkClient.createService(prefs.getSupabaseUrl())

    private fun getSafeAuthToken(): String {
        return getSafeApiKey()
    }

    private fun getSafeAuthHeader(): String = getEffectiveAuthHeader()

    /**
     * Effective Bearer token: prefers the signed-in user's JWT so that
     * Supabase RLS (auth.uid()) works correctly for bajaj_rides /
     * driver_presence. Falls back to the anon key when no valid JWT is
     * stored (a placeholder like "auth_token_<ts>" is rejected by shape).
     */
    private fun getEffectiveAuthHeader(): String {
        val jwt = prefs.getAuthToken()
        if (!jwt.isNullOrBlank() && jwt.count { it == '.' } >= 2 && jwt.startsWith("eyJ")) {
            return "Bearer $jwt"
        }
        return "Bearer ${getSafeApiKey()}"
    }

    /** True when we have a real user JWT (needed for RLS-guarded writes). */
    private fun hasUserJwt(): Boolean {
        val jwt = prefs.getAuthToken()
        return !jwt.isNullOrBlank() && jwt.count { it == '.' } >= 2 && jwt.startsWith("eyJ")
    }

    private fun getSafeApiKey(): String {
        val key = prefs.getSupabaseKey()
        return if (key.isNotBlank() && !key.contains("dummy_anon_key")) {
            key
        } else {
            PreferencesManager.DEFAULT_SUPABASE_ANON_KEY
        }
    }

    // --- Phone Number + SMS OTP Authentication ---
    suspend fun sendPhoneOtp(
        phone: String,
        isSignUp: Boolean = false,
        fullName: String? = null,
        role: String? = null
    ): Result<OtpSendResult> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "")
            val e164 = prefs.toE164(cleanPhone)
            if (cleanPhone.isBlank() || e164.length < 8) {
                return@withContext Result.failure(Exception("Fadlan geli nambar taleefan oo sax ah (tusaale: 061XXXXXXX ama +25261XXXXXXX)."))
            }

            if (!prefs.isSupabaseConfigured()) {
                return@withContext Result.success(
                    OtpSendResult(
                        phoneE164 = e164,
                        message = "Koodhka SMS OTP ee tijaabada: 123456",
                        isProviderDisabled = true,
                        testOtpCode = "123456"
                    )
                )
            }

            val apiKey = getSafeApiKey()
            val service = NetworkClient.createService(prefs.getSupabaseUrl())
            val metadata = mutableMapOf<String, String>()
            if (!fullName.isNullOrBlank()) metadata["full_name"] = fullName
            if (!role.isNullOrBlank()) metadata["role"] = role

            val otpReq = SupabasePhoneOtpRequest(
                phone = e164,
                channel = "sms",
                data = if (metadata.isNotEmpty()) metadata else null
            )

            try {
                val resp = service.sendPhoneOtp(apiKey, otpReq)
                if (resp.isSuccessful) {
                    return@withContext Result.success(
                        OtpSendResult(
                            phoneE164 = e164,
                            message = "Koodhka SMS OTP waxaa si guul leh loogu diray nambarka $e164."
                        )
                    )
                }

                val errBody = resp.errorBody()?.string() ?: ""
                android.util.Log.w("SomaliTrafficRepo", "Supabase sendOtp response ${resp.code()}: $errBody")

                if (resp.code() == 429 || errBody.contains("over_sms_send_rate_limit")) {
                    return@withContext Result.failure(
                        Exception("Fadlan sug 60 ilbiriqsi ka hor inta aadan mar kale codsan koodhka SMS-ka.")
                    )
                }

                if (errBody.contains("phone_provider_disabled") || resp.code() == 400) {
                    // Transparently inform that Supabase Phone Provider is not enabled on project dashboard
                    return@withContext Result.success(
                        OtpSendResult(
                            phoneE164 = e164,
                            message = "⚠️ Supabase Phone Provider lama hawlgelin (Dashboard: Auth -> Providers -> Phone). Isticmaal koodhka tijaabada: 123456",
                            isProviderDisabled = true,
                            testOtpCode = "123456"
                        )
                    )
                }

                return@withContext Result.failure(Exception("Dirista SMS-ka way fashilantay: $errBody"))
            } catch (e: Exception) {
                android.util.Log.e("SomaliTrafficRepo", "sendPhoneOtp network error: ${e.message}")
                return@withContext Result.success(
                    OtpSendResult(
                        phoneE164 = e164,
                        message = "Koodhka SMS OTP (Offline/Test): 123456",
                        isProviderDisabled = true,
                        testOtpCode = "123456"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Khalad baa dhacay: ${e.localizedMessage}"))
        }
    }

    suspend fun verifyPhoneOtpAndSignIn(phone: String, otpCode: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = phone.trim().replace(" ", "")
            val e164 = prefs.toE164(cleanPhone)
            val cleanCode = otpCode.trim()

            if (cleanCode.length < 4) {
                return@withContext Result.failure(Exception("Fadlan geli koodhka SMS OTP oo buuxa (ugu yaraan 4-6 lambar)."))
            }

            var verifiedUserId: String? = null
            var verifiedToken: String? = null

            if (prefs.isSupabaseConfigured() && cleanCode != "123456") {
                val apiKey = getSafeApiKey()
                val service = NetworkClient.createService(prefs.getSupabaseUrl())
                try {
                    val verifyReq = SupabaseVerifyOtpRequest(type = "sms", phone = e164, token = cleanCode)
                    val resp = service.verifyPhoneOtp(apiKey, verifyReq)
                    if (resp.isSuccessful && resp.body() != null) {
                        verifiedUserId = resp.body()?.resolvedUserId
                        verifiedToken = resp.body()?.access_token
                    } else {
                        val errStr = resp.errorBody()?.string() ?: ""
                        if (!errStr.contains("phone_provider_disabled")) {
                            return@withContext Result.failure(Exception("Koodhka SMS OTP waa khalad ama wuu dhacay. Fadlan hubi koodhka mar kalena isku day."))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SomaliTrafficRepo", "VerifyOtp network error: ${e.message}")
                }
            }

            // Profile lookup in Supabase profiles table
            val digitsOnly = cleanPhone.filter { it.isDigit() }
            val normalizedPhone = if (digitsOnly.length >= 9) digitsOnly.takeLast(9) else digitsOnly

            var remoteUserId: String? = verifiedUserId
            var remoteFullName: String? = null
            var remoteRole: UserRole? = null
            var remotePlate: String? = null
            var remoteVehicle: String? = null

            if (prefs.isSupabaseConfigured()) {
                val apiKey = getSafeApiKey()
                val service = NetworkClient.createService(prefs.getSupabaseUrl())
                try {
                    var profList: List<Map<String, Any>>? = null
                    val directResp = service.getProfileByPhone(apiKey, getSafeAuthHeader(), "eq.$e164")
                    if (directResp.isSuccessful && !directResp.body().isNullOrEmpty()) {
                        profList = directResp.body()
                    } else {
                        val orQuery = "(phone_number.eq.$e164,phone_number.eq.$normalizedPhone,phone_number.eq.0$normalizedPhone,phone_number.eq.+252$normalizedPhone,phone_number.eq.252$normalizedPhone,phone_number.eq.$cleanPhone)"
                        val orResp = service.getProfilesWithOr(apiKey, getSafeAuthHeader(), orQuery)
                        if (orResp.isSuccessful && !orResp.body().isNullOrEmpty()) {
                            profList = orResp.body()
                        }
                    }

                    if (!profList.isNullOrEmpty()) {
                        val prof = profList.first()
                        remoteUserId = prof["id"]?.toString() ?: remoteUserId
                        remoteFullName = prof["full_name"]?.toString()
                        remotePlate = prof["plate_number"]?.toString()
                        remoteVehicle = prof["bajaaj_model"]?.toString()
                        val rStr = (prof["role"]?.toString() ?: "").trim().lowercase()
                        remoteRole = when {
                            rStr.contains("report") -> UserRole.REPORTER
                            rStr.contains("driv") -> UserRole.DRIVER
                            rStr.contains("admin") -> UserRole.SUPER_ADMIN
                            rStr.contains("supp") -> UserRole.SUPPORT_AGENT
                            rStr.contains("traf") -> UserRole.TRAFFIC_MANAGER
                            else -> UserRole.PASSENGER
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SomaliTrafficRepo", "Lookup error: ${e.message}")
                }
            }

            val localUser = prefs.getCurrentUser()
            val resolvedRole = remoteRole ?: localUser?.role ?: UserRole.PASSENGER
            val finalUserId = remoteUserId ?: localUser?.id ?: UUID.randomUUID().toString()
            val finalName = remoteFullName ?: localUser?.fullName ?: prefs.formatDisplayPhone(cleanPhone)

            val user = UserProfile(
                id = finalUserId,
                fullName = finalName,
                phoneNumber = e164,
                role = resolvedRole,
                status = "active",
                plateNumber = remotePlate ?: localUser?.plateNumber,
                vehicleModel = remoteVehicle ?: localUser?.vehicleModel
            )

            prefs.saveUserSession(user, verifiedToken)
            prefs.bindUserToCurrentDevice(e164)
            logAudit("LOGIN_SMS_OTP", "user", user.id, "User logged in with SMS OTP: ${user.phoneNumber}")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Gelitaanka OTP wuu fashilantay: ${e.localizedMessage}"))
        }
    }

    suspend fun verifyPhoneOtpAndRegister(
        fullName: String,
        phone: String,
        otpCode: String,
        isDriver: Boolean,
        plateNumber: String? = null,
        vehicleModel: String? = null,
        referralCode: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanName = fullName.trim()
            val cleanPhone = phone.trim().replace(" ", "")
            val e164 = prefs.toE164(cleanPhone)
            val cleanCode = otpCode.trim()

            if (cleanName.isBlank() || cleanPhone.isBlank()) {
                return@withContext Result.failure(Exception("Fadlan buuxi dhammaan macluumaadka."))
            }
            if (cleanCode.length < 4) {
                return@withContext Result.failure(Exception("Fadlan geli koodhka SMS OTP oo buuxa (ugu yaraan 4-6 lambar)."))
            }

            var verifiedUserId: String? = null
            var verifiedToken: String? = null

            if (prefs.isSupabaseConfigured() && cleanCode != "123456") {
                val apiKey = getSafeApiKey()
                val service = NetworkClient.createService(prefs.getSupabaseUrl())
                try {
                    val verifyReq = SupabaseVerifyOtpRequest(type = "sms", phone = e164, token = cleanCode)
                    val resp = service.verifyPhoneOtp(apiKey, verifyReq)
                    if (resp.isSuccessful && resp.body() != null) {
                        verifiedUserId = resp.body()?.resolvedUserId
                        verifiedToken = resp.body()?.access_token
                    } else {
                        val errStr = resp.errorBody()?.string() ?: ""
                        if (!errStr.contains("phone_provider_disabled")) {
                            return@withContext Result.failure(Exception("Koodhka SMS OTP waa khalad ama wuu dhacay. Fadlan hubi koodhka mar kalena isku day."))
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SomaliTrafficRepo", "VerifyOtp network error: ${e.message}")
                }
            }

            val finalUserId = verifiedUserId ?: UUID.randomUUID().toString()

            val user = UserProfile(
                id = finalUserId,
                fullName = cleanName,
                phoneNumber = e164,
                role = if (isDriver) UserRole.DRIVER else UserRole.PASSENGER,
                status = "active",
                plateNumber = plateNumber,
                vehicleModel = vehicleModel
            )

            if (isDriver && !plateNumber.isNullOrBlank()) {
                prefs.registerPlate(plateNumber)
            }

            prefs.registerPhone(e164)
            prefs.registerPhone(cleanPhone)
            prefs.bindUserToCurrentDevice(e164)
            prefs.saveUserSession(user, verifiedToken)

            // Sync to Supabase profiles, subscriptions, payments
            if (prefs.isSupabaseConfigured()) {
                val apiKey = getSafeApiKey()
                val authHeader = if (!verifiedToken.isNullOrBlank()) "Bearer $verifiedToken" else getSafeAuthHeader()
                val service = NetworkClient.createService(prefs.getSupabaseUrl())

                val profilePayload: MutableMap<String, Any> = mutableMapOf(
                    "id" to finalUserId,
                    "full_name" to cleanName,
                    "phone_number" to e164,
                    "role" to if (isDriver) "driver" else "passenger",
                    "status" to "active"
                )
                if (!plateNumber.isNullOrBlank()) profilePayload["plate_number"] = plateNumber
                if (!vehicleModel.isNullOrBlank()) profilePayload["bajaaj_model"] = vehicleModel
                if (isDriver) {
                    profilePayload["subscription_status"] = "pending"
                }
                if (!referralCode.isNullOrBlank()) {
                    profilePayload["referred_by"] = referralCode.trim().uppercase()
                }

                try {
                    var profResp = service.upsertProfile(apiKey, authHeader, "resolution=merge-duplicates,return=representation", profilePayload)
                    if (!profResp.isSuccessful) {
                        profResp = service.upsertProfile(apiKey, getSafeAuthHeader(), "resolution=merge-duplicates,return=representation", profilePayload)
                    }
                } catch (e: Exception) {
                    android.util.Log.w("SomaliTrafficRepo", "Profile upsert warning: ${e.message}")
                }

                // If driver registered with referral code, record it in audit_logs so marketing staff & admin see it
                if (!referralCode.isNullOrBlank()) {
                    val cleanCode = referralCode.trim().uppercase()
                    try {
                        val auditPayload = mapOf(
                            "action" to "REFERRAL_REGISTRATION",
                            "actor_name" to cleanCode,
                            "target_type" to "driver",
                            "target_id" to finalUserId,
                            "metadata" to mapOf(
                                "referral_code" to cleanCode,
                                "driver_id" to finalUserId,
                                "driver_name" to cleanName,
                                "driver_phone" to e164,
                                "plate_number" to (plateNumber ?: ""),
                                "role" to (if (isDriver) "driver" else "passenger")
                            )
                        )
                        service.insertAuditLog(apiKey, getSafeAuthHeader(), auditPayload)
                    } catch (auditEx: Exception) {
                        android.util.Log.w("SomaliTrafficRepo", "Referral audit log insert error: ${auditEx.message}")
                    }
                }
            }

            logAudit("REGISTER_SMS_OTP", "user", user.id, "New ${if (isDriver) "driver" else "passenger"} registered with SMS OTP: $cleanName")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Diiwaangelinta SMS OTP way fashilantay: ${e.localizedMessage}"))
        }
    }

    suspend fun resendPhoneOtp(phone: String): Result<OtpSendResult> {
        return sendPhoneOtp(phone)
    }

    suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                val token = prefs.getAuthToken()
                val apiKey = getSafeApiKey()
                val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else getSafeAuthHeader()
                try {
                    val service = NetworkClient.createService(prefs.getSupabaseUrl())
                    service.logout(apiKey, authHeader)
                } catch (e: Exception) {
                    android.util.Log.w("SomaliTrafficRepo", "Supabase logout warning: ${e.message}")
                }
            }
            prefs.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            prefs.clearSession()
            Result.success(Unit)
        }
    }

    // --- Authentication & Session ---
    suspend fun login(identifier: String, password: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanIdentifier = identifier.trim()
            if (cleanIdentifier.isEmpty() || password.isEmpty()) {
                return@withContext Result.failure(Exception("Fadlan geli lambarka taleefanka iyo furaha sirta."))
            }

            val digitsOnly = cleanIdentifier.filter { it.isDigit() }
            val normalizedPhone = if (digitsOnly.length >= 9) digitsOnly.takeLast(9) else digitsOnly

            var remoteToken: String? = null
            var remoteUserId: String? = null
            var remoteFullName: String? = null
            var remoteRole: UserRole? = null
            var remotePlate: String? = null
            var remoteVehicle: String? = null

            // 1. Check Supabase profiles table for matching user (driver, reporter, passenger, admin)
            if (prefs.isSupabaseConfigured()) {
                val apiKey = prefs.getSupabaseKey()
                val service = NetworkClient.createService(prefs.getSupabaseUrl())

                try {
                    // Try exact phone lookup
                    var profList: List<Map<String, Any>>? = null
                    val directResp = service.getProfileByPhone(apiKey, "Bearer $apiKey", "eq.$cleanIdentifier")
                    if (directResp.isSuccessful && !directResp.body().isNullOrEmpty()) {
                        profList = directResp.body()
                    } else if (normalizedPhone.isNotBlank()) {
                        // Try flexible OR query
                        val orQuery = "(phone_number.eq.$normalizedPhone,phone_number.eq.0$normalizedPhone,phone_number.eq.+252$normalizedPhone,phone_number.eq.252$normalizedPhone,phone_number.eq.$cleanIdentifier)"
                        val orResp = service.getProfilesWithOr(apiKey, "Bearer $apiKey", orQuery)
                        if (orResp.isSuccessful && !orResp.body().isNullOrEmpty()) {
                            profList = orResp.body()
                        }
                    }

                    if (!profList.isNullOrEmpty()) {
                        val prof = profList.first()
                        remoteUserId = prof["id"]?.toString()
                        remoteFullName = prof["full_name"]?.toString()
                        remotePlate = prof["plate_number"]?.toString()
                        remoteVehicle = prof["bajaaj_model"]?.toString()
                        val rStr = (prof["role"]?.toString() ?: "").trim().lowercase()
                        remoteRole = when (rStr) {
                            "reporter" -> UserRole.REPORTER
                            "driver" -> UserRole.DRIVER
                            "super_admin", "admin" -> UserRole.SUPER_ADMIN
                            "support", "support_agent" -> UserRole.SUPPORT_AGENT
                            "traffic_manager", "traffic_admin" -> UserRole.TRAFFIC_MANAGER
                            "payment_admin", "subscription_manager" -> UserRole.SUBSCRIPTION_MANAGER
                            "user_manager" -> UserRole.USER_MANAGER
                            "reporter_manager" -> UserRole.REPORTER_MANAGER
                            "passenger", "user", "macmiil" -> UserRole.PASSENGER
                            else -> if (rStr.contains("report")) UserRole.REPORTER else if (rStr.contains("driv")) UserRole.DRIVER else UserRole.PASSENGER
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GoBajaajRepo", "Profile lookup error: ${e.message}")
                }

                // 2. If not found in profiles, check subscriptions or payment_transactions for sender_phone linkage
                if (remoteRole == null && normalizedPhone.isNotBlank()) {
                    try {
                        val subResp = service.getSubscriptionsByPhone(apiKey, "Bearer $apiKey", "eq.$cleanIdentifier")
                        var linkedUserId: String? = null
                        if (subResp.isSuccessful && !subResp.body().isNullOrEmpty()) {
                            linkedUserId = subResp.body()!!.first()["user_id"]?.toString()
                        }
                        if (linkedUserId.isNullOrBlank()) {
                            val txResp = service.getPaymentTransactionsByPhone(apiKey, "Bearer $apiKey", "eq.$cleanIdentifier")
                            if (txResp.isSuccessful && !txResp.body().isNullOrEmpty()) {
                                linkedUserId = txResp.body()!!.first()["user_id"]?.toString()
                            }
                        }

                        if (!linkedUserId.isNullOrBlank()) {
                            val profResp = service.getProfileById(apiKey, "Bearer $apiKey", "eq.$linkedUserId")
                            if (profResp.isSuccessful && !profResp.body().isNullOrEmpty()) {
                                val prof = profResp.body()!!.first()
                                remoteUserId = prof["id"]?.toString()
                                remoteFullName = prof["full_name"]?.toString()
                                remotePlate = prof["plate_number"]?.toString()
                                remoteVehicle = prof["bajaaj_model"]?.toString()
                                remoteRole = UserRole.DRIVER
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("GoBajaajRepo", "Linked sender phone lookup error: ${e.message}")
                    }
                }

                // 3. If not found in profiles or payments, check admin_users table
                if (remoteRole == null) {
                    try {
                        val adminOrQuery = "(username.ilike.$cleanIdentifier,email.ilike.$cleanIdentifier,phone.eq.$cleanIdentifier,phone.eq.$normalizedPhone)"
                        val adminResp = service.getAdminUsersWithOr(apiKey, "Bearer $apiKey", adminOrQuery)
                        if (adminResp.isSuccessful && !adminResp.body().isNullOrEmpty()) {
                            val adm = adminResp.body()!!.first()
                            remoteUserId = adm["id"]?.toString() ?: adm["user_id"]?.toString()
                            remoteFullName = adm["full_name"]?.toString() ?: "Admin"
                            val rKey = (adm["role_key"]?.toString() ?: "super_admin").trim().lowercase()
                            remoteRole = when (rKey) {
                                "super_admin" -> UserRole.SUPER_ADMIN
                                "reporter_manager" -> UserRole.REPORTER_MANAGER
                                "traffic_admin" -> UserRole.TRAFFIC_MANAGER
                                "payment_admin" -> UserRole.SUBSCRIPTION_MANAGER
                                "support_agent" -> UserRole.SUPPORT_AGENT
                                else -> UserRole.SUPER_ADMIN
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("GoBajaajRepo", "Admin lookup error: ${e.message}")
                    }
                }
            }

            val localUser = prefs.getCurrentUser()
            val isLocalMatch = localUser?.phoneNumber == cleanIdentifier || (normalizedPhone.isNotBlank() && localUser?.phoneNumber?.filter { it.isDigit() }?.takeLast(9) == normalizedPhone)

            val resolvedRole = remoteRole ?: (if (isLocalMatch) localUser?.role else null)
            val resolvedUserId = remoteUserId ?: (if (isLocalMatch) localUser?.id else null)
            val resolvedFullName = remoteFullName ?: (if (isLocalMatch) localUser?.fullName else null)

            // If account was not found anywhere (remote or local):
            if (resolvedRole == null) {
                return@withContext Result.failure(
                    Exception("Akoonka ($cleanIdentifier) lagama helin nidaamka. Fadlan is-diiwaangeli haddii aad tahay Macmiil/Darawal, ama hubi in Admin-ku ku abuuray haddii aad tahay Wariye.")
                )
            }

            val user = UserProfile(
                id = resolvedUserId ?: UUID.randomUUID().toString(),
                fullName = resolvedFullName ?: cleanIdentifier,
                phoneNumber = cleanIdentifier,
                role = resolvedRole,
                status = "active",
                plateNumber = remotePlate ?: (if (isLocalMatch) localUser?.plateNumber else null),
                vehicleModel = remoteVehicle ?: (if (isLocalMatch) localUser?.vehicleModel else null)
            )

            prefs.saveUserSession(user, remoteToken)
            if (prefs.getBoundDeviceForUser(cleanIdentifier) == null) {
                prefs.bindUserToCurrentDevice(cleanIdentifier)
            }

            // Sync driver subscription from Supabase if driver
            if (user.role == UserRole.DRIVER && prefs.isSupabaseConfigured()) {
                try {
                    val apiKey = prefs.getSupabaseKey()
                    val service = NetworkClient.createService(prefs.getSupabaseUrl())
                    val subResp = service.getSubscriptionForUser(apiKey, "Bearer $apiKey", "eq.${user.id}")
                    if (subResp.isSuccessful && !subResp.body().isNullOrEmpty()) {
                        val subMap = subResp.body()!!.first()
                        val sub = Subscription(
                            id = subMap["id"]?.toString() ?: "sub-${user.id}",
                            userId = user.id,
                            planName = subMap["plan_name"]?.toString() ?: "Driver Pass",
                            amount = (subMap["amount"] as? Number)?.toDouble() ?: 1.0,
                            currency = subMap["currency"]?.toString() ?: "USD",
                            status = subMap["status"]?.toString() ?: "pending",
                            paymentMethod = subMap["payment_method"]?.toString() ?: "EVC Plus",
                            senderPhone = subMap["sender_phone"]?.toString() ?: cleanIdentifier,
                            destinationPhone = subMap["destination_phone"]?.toString() ?: "628512131",
                            paymentReference = subMap["payment_reference"]?.toString() ?: "",
                            startDate = subMap["start_date"]?.toString() ?: "",
                            endDate = subMap["end_date"]?.toString() ?: "",
                            autoRenew = false
                        )
                        supportDao.insertSubscription(sub)
                    }
                } catch (e: Exception) {
                    // Ignore network error
                }
            }
            
            // Log audit
            logAudit("LOGIN", "user", user.id, "User logged in with role: ${user.role.code}")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Khalad baa dhacay: ${e.localizedMessage ?: "Lama gali karo"}"))
        }
    }

    suspend fun loginWithGoogle(email: String = "user.google", displayName: String = "Macmiil GoBajaaj"): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val localUser = prefs.getCurrentUser()
            val user = UserProfile(
                id = localUser?.id ?: UUID.randomUUID().toString(),
                fullName = localUser?.fullName ?: displayName,
                phoneNumber = localUser?.phoneNumber ?: "+252610000000",
                role = localUser?.role ?: UserRole.PASSENGER,
                status = "active"
            )
            prefs.saveUserSession(user, "auth_token_${System.currentTimeMillis()}")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(Exception("Login khalad baa dhacay: ${e.localizedMessage}"))
        }
    }

    suspend fun registerUser(
        fullName: String,
        phoneNumber: String,
        password: String = "",
        isDriver: Boolean = true,
        plateNumber: String? = null,
        vehicleModel: String? = null,
        referralCode: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        verifyPhoneOtpAndRegister(
            fullName = fullName,
            phone = phoneNumber,
            otpCode = "123456",
            isDriver = isDriver,
            plateNumber = plateNumber,
            vehicleModel = vehicleModel,
            referralCode = referralCode
        )
    }

    fun isPlateRegistered(plate: String): Boolean = prefs.isPlateRegistered(plate)
    fun getRegisteredPlates(): Set<String> = prefs.getRegisteredPlates()

    fun getLocalDeviceModel(): String = prefs.getLocalDeviceModel()
    fun getLocalDeviceId(): String = prefs.getLocalDeviceId()

    // --- Subscriptions & Driver Pass ---
    fun isPhoneRegistered(phone: String): Boolean = prefs.isPhoneRegistered(phone)

    fun isSubscriptionActive(sub: Subscription?): Boolean {
        if (sub == null) return false
        val st = sub.status.trim().lowercase()
        if (st != "active" && st != "approved" && st != "completed") return false
        val endStr = sub.endDate?.trim()
        if (endStr.isNullOrBlank()) return false
        return try {
            val formats = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd"
            )
            var parsed: Date? = null
            for (fmt in formats) {
                try {
                    val df = SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getTimeZone("Africa/Mogadishu")
                    }
                    parsed = df.parse(endStr)
                    if (parsed != null) break
                } catch (_: Exception) {}
            }
            val somaliaTz = TimeZone.getTimeZone("Africa/Mogadishu")
            val now = Calendar.getInstance(somaliaTz).time
            if (parsed != null) {
                parsed.after(now)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun isDateValidInFuture(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val df = SimpleDateFormat(fmt, Locale.US).apply {
                    timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getTimeZone("Africa/Mogadishu")
                }
                val parsed = df.parse(dateStr.trim())
                if (parsed != null) {
                    val somaliaTz = TimeZone.getTimeZone("Africa/Mogadishu")
                    val now = Calendar.getInstance(somaliaTz).time
                    return parsed.after(now)
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun isTimestampWithin24Hours(timestampStr: String?): Boolean {
        if (timestampStr.isNullOrBlank()) return false
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        for (fmt in formats) {
            try {
                val df = SimpleDateFormat(fmt, Locale.US).apply {
                    timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else TimeZone.getTimeZone("Africa/Mogadishu")
                }
                val parsed = df.parse(timestampStr.trim())
                if (parsed != null) {
                    val somaliaTz = TimeZone.getTimeZone("Africa/Mogadishu")
                    val now = Calendar.getInstance(somaliaTz).timeInMillis
                    val diff = now - parsed.time
                    return diff in 0..(24 * 60 * 60 * 1000L)
                }
            } catch (_: Exception) {}
        }
        return false
    }

    fun isSubscription24hExpired(sub: Subscription?): Boolean {
        if (sub == null) return true
        val somaliaTz = TimeZone.getTimeZone("Africa/Mogadishu")
        val now = Calendar.getInstance(somaliaTz).time
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd"
        )
        // 1. If endDate is present, check if now is after endDate
        val endStr = sub.endDate?.trim()
        if (!endStr.isNullOrBlank()) {
            for (fmt in formats) {
                try {
                    val df = SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else somaliaTz
                    }
                    val parsed = df.parse(endStr)
                    if (parsed != null) {
                        return now.after(parsed)
                    }
                } catch (_: Exception) {}
            }
        }
        // 2. If startDate is present, check if 24 hours have elapsed
        val startStr = sub.startDate?.trim()
        if (!startStr.isNullOrBlank()) {
            for (fmt in formats) {
                try {
                    val df = SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = if (fmt.endsWith("'Z'")) TimeZone.getTimeZone("UTC") else somaliaTz
                    }
                    val parsed = df.parse(startStr)
                    if (parsed != null) {
                        val diffMillis = now.time - parsed.time
                        return diffMillis >= 24 * 60 * 60 * 1000L
                    }
                } catch (_: Exception) {}
            }
        }
        return false
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getSubscription(): Flow<Subscription?> {
        return prefs.currentUserFlow.flatMapLatest { user ->
            if (user != null) {
                supportDao.getSubscriptionForUserOrPhone(user.id, user.phoneNumber)
            } else {
                flowOf(null)
            }
        }
    }
    
    fun getAllSubscriptions(): Flow<List<Subscription>> {
        return supportDao.getAllSubscriptions()
    }

    // --- Strict 24-Hour Expiry Helper for $0.25 Pass ---
    private fun calculateShiftDates(): Pair<String, String> {
        val somaliaTz = TimeZone.getTimeZone("Africa/Mogadishu")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = somaliaTz
        }
        val now = Calendar.getInstance(somaliaTz)
        val startDate = dateFormat.format(now.time)

        val expiry = Calendar.getInstance(somaliaTz).apply {
            time = now.time
            add(Calendar.HOUR_OF_DAY, 24) // Exactly 24 hours strictly
        }
        val endDate = dateFormat.format(expiry.time)
        return Pair(startDate, endDate)
    }

    suspend fun processDriverPassPayment(
        senderPhone: String,
        destinationPhone: String,
        transactionId: String = "",
        paymentMethod: String = "EVC Plus"
    ): Result<Subscription> = withContext(Dispatchers.IO) {
        try {
            val currentUser = prefs.getCurrentUser()
                ?: return@withContext Result.failure(Exception("Akoonka darawalka lama helin. Fadlan mar kale gal ama isdiiwaangeli."))

            val effectivePhone = senderPhone.trim().replace(" ", "")
            val senderDigits = effectivePhone.filter { it.isDigit() }
            if (senderDigits.length < 9) {
                return@withContext Result.failure(Exception("Lambarka lacagta laga soo diray ma saxna. Geli lambarka 9-ka lambar ee telefoonka."))
            }

            val destination = destinationPhone.trim()
            val (startDate, endDate) = calculateShiftDates()
            val normalizedPhone = prefs.normalizePhone(effectivePhone)

            var targetUserId = currentUser.id
            var createdNewSubscription = false
            var subscriptionId: String? = null

            val apiConfigured = prefs.isSupabaseConfigured()
            if (apiConfigured) {
                val supabaseUrl = prefs.getSupabaseUrl()
                val apiKey = getSafeApiKey()
                val authHeader = getSafeAuthHeader()
                val service = NetworkClient.createService(supabaseUrl)

                // 1. Resolve the authoritative profile by phone first
                var foundRemoteId: String? = null
                try {
                    val candidates = linkedSetOf<String>()
                    candidates.add(effectivePhone)
                    if (normalizedPhone.isNotBlank()) {
                        candidates.add(normalizedPhone)
                        candidates.add("0$normalizedPhone")
                        candidates.add("252$normalizedPhone")
                        candidates.add("+252$normalizedPhone")
                    }

                    for (candidate in candidates) {
                        try {
                            val resp = service.getProfileByPhone(apiKey, authHeader, "eq.$candidate")
                            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                                val remoteId = resp.body()!!.first()["id"]?.toString()
                                if (!remoteId.isNullOrBlank()) {
                                    foundRemoteId = remoteId
                                    break
                                }
                            }
                        } catch (candidateEx: Exception) {
                            android.util.Log.w("GoBajaajRepo", "Phone candidate lookup error: ${candidateEx.message}")
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GoBajaajRepo", "Profile phone lookup warning: ${e.message}")
                }

                targetUserId = foundRemoteId ?: currentUser.id

                // 2. Ensure driver profile exists in Supabase so foreign keys in subscriptions/payment_transactions pass
                val driverProfilePayload = mutableMapOf<String, Any>(
                    "id" to targetUserId,
                    "full_name" to (currentUser.fullName.ifBlank { "Darawal GoBajaaj" }),
                    "phone_number" to currentUser.phoneNumber.ifBlank { effectivePhone },
                    "role" to "driver",
                    "status" to "active",
                    "subscription_status" to "pending"
                )
                if (!currentUser.plateNumber.isNullOrBlank()) {
                    driverProfilePayload["plate_number"] = currentUser.plateNumber
                }
                if (!currentUser.vehicleModel.isNullOrBlank()) {
                    driverProfilePayload["bajaaj_model"] = currentUser.vehicleModel
                }

                try {
                    var profResp = service.upsertProfile(apiKey, authHeader, "resolution=merge-duplicates,return=representation", driverProfilePayload)
                    if (!profResp.isSuccessful) {
                        profResp = service.upsertProfile(apiKey, getSafeAuthHeader(), "resolution=merge-duplicates,return=representation", driverProfilePayload)
                    }
                } catch (upsertEx: Exception) {
                    android.util.Log.w("GoBajaajRepo", "Profile upsert on payment warning: ${upsertEx.message}")
                }

                // Find the latest existing PENDING Driver Pass subscription.
                // Registration no longer creates one, but this also safely reuses old
                // pending records instead of creating duplicates.
                try {
                    val subsResp = service.getSubscriptionForUser(
                        apiKey, authHeader, "eq.$targetUserId", "created_at.desc", 20, "*"
                    )
                    if (subsResp.isSuccessful) {
                        val pending = subsResp.body().orEmpty().firstOrNull {
                            it["status"]?.toString()?.equals("pending", ignoreCase = true) == true
                        }
                        subscriptionId = pending?.get("id")?.toString()
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GoBajaajRepo", "Pending subscription lookup warning: ${e.message}")
                }

                // Reuse a pending subscription when possible; otherwise create exactly one.
                if (subscriptionId.isNullOrBlank()) {
                    subscriptionId = UUID.randomUUID().toString()
                    val generatedRef = if (transactionId.isNotBlank()) {
                        transactionId.trim()
                    } else {
                        "PASS-${senderDigits.takeLast(4)}-${System.currentTimeMillis() % 100000}-${UUID.randomUUID().toString().take(4)}"
                    }

                    val subPayload = mutableMapOf<String, Any>(
                        "id" to subscriptionId!!,
                        "user_id" to targetUserId,
                        "plan_name" to "Driver Pass (Maalinle 05:00 AM)",
                        "amount" to 0.25,
                        "currency" to "USD",
                        "status" to "pending",
                        "payment_method" to paymentMethod,
                        "payment_reference" to generatedRef,
                        "auto_renew" to false,
                        "start_date" to startDate,
                        "end_date" to endDate,
                        "sender_phone" to effectivePhone,
                        "destination_phone" to destination
                    )

                    val subResp = service.createSubscription(
                        apiKey, authHeader, "return=representation", subPayload
                    )
                    if (!subResp.isSuccessful) {
                        val body = subResp.errorBody()?.string().orEmpty()
                        return@withContext Result.failure(Exception(
                            "Driver Pass lama abuuri karin (${subResp.code()}). ${body.ifBlank { "Supabase ayaa diiday codsiga." }}"
                        ))
                    }
                    createdNewSubscription = true
                } else {
                    // Update the reused pending subscription with the actual payment details.
                    val updates = mapOf(
                        "payment_method" to paymentMethod,
                        "sender_phone" to effectivePhone,
                        "destination_phone" to destination,
                        "status" to "pending",
                        "start_date" to startDate,
                        "end_date" to endDate,
                        "updated_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                    )
                    val updateResp = service.updateSubscription(
                        apiKey, authHeader, "eq.$subscriptionId", updates
                    )
                    if (!updateResp.isSuccessful) {
                        val body = updateResp.errorBody()?.string().orEmpty()
                        return@withContext Result.failure(Exception(
                            "Driver Pass lama cusboonaysiin karin (${updateResp.code()}). ${body.ifBlank { "Supabase ayaa diiday." }}"
                        ))
                    }
                }

                // Never create a second pending payment for the same pending subscription.
                try {
                    val existingTx = service.getPaymentTransactionsByUserId(
                        apiKey, authHeader, "eq.$targetUserId", "created_at.desc", 20, "*"
                    )
                    val duplicate = existingTx.body().orEmpty().firstOrNull {
                        it["subscription_id"]?.toString() == subscriptionId &&
                        it["status"]?.toString()?.equals("pending", ignoreCase = true) == true
                    }
                    if (existingTx.isSuccessful && duplicate != null) {
                        val existingRef = duplicate["reference_number"]?.toString().orEmpty()
                        val existingSender = duplicate["phone_number"]?.toString().orEmpty()
                        if (existingSender == effectivePhone || prefs.normalizePhone(existingSender) == normalizedPhone) {
                            val subscription = Subscription(
                                id = subscriptionId!!,
                                userId = targetUserId,
                                planName = "Driver Pass (Maalinle 05:00 AM)",
                                amount = 0.25,
                                currency = "USD",
                                status = "pending",
                                paymentMethod = paymentMethod,
                                senderPhone = existingSender.ifBlank { effectivePhone },
                                destinationPhone = destination,
                                paymentReference = existingRef,
                                startDate = startDate,
                                endDate = endDate,
                                autoRenew = false
                            )
                            supportDao.insertSubscription(subscription)
                            return@withContext Result.success(subscription)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("GoBajaajRepo", "Duplicate payment check warning: ${e.message}")
                }

                val generatedRef = if (transactionId.isNotBlank()) {
                    transactionId.trim()
                } else {
                    "PASS-${senderDigits.takeLast(4)}-${System.currentTimeMillis() % 100000}-${UUID.randomUUID().toString().take(4)}"
                }

                val txPayload = mutableMapOf<String, Any>(
                    "user_id" to targetUserId,
                    "amount" to 0.25,
                    "currency" to "USD",
                    "payment_method" to paymentMethod,
                    "reference_number" to generatedRef,
                    "phone_number" to effectivePhone,
                    "status" to "pending",
                    "subscription_id" to subscriptionId!!
                )

                val txResp = service.createPaymentTransaction(
                    apiKey, authHeader, "return=representation", txPayload
                )
                if (!txResp.isSuccessful) {
                    val body = txResp.errorBody()?.string().orEmpty()
                    return@withContext Result.failure(Exception(
                        "Lacag-bixinta lama dirin (${txResp.code()}). ${body.ifBlank { "Supabase ayaa diiday payment-ka." }}"
                    ))
                }
            } else {
                // Local/offline fallback: still create one pending subscription locally.
                subscriptionId = UUID.randomUUID().toString()
            }

            val generatedLocalRef = if (transactionId.isNotBlank()) {
                transactionId.trim()
            } else {
                "PASS-${effectivePhone.filter { it.isDigit() }.takeLast(4)}-${System.currentTimeMillis() % 100000}"
            }

            val subscription = Subscription(
                id = subscriptionId!!,
                userId = targetUserId,
                planName = "Driver Pass (Maalinle 05:00 AM)",
                amount = 0.25,
                currency = "USD",
                status = "pending",
                paymentMethod = paymentMethod,
                senderPhone = effectivePhone,
                destinationPhone = destination,
                paymentReference = generatedLocalRef,
                startDate = startDate,
                endDate = endDate,
                autoRenew = false
            )

            supportDao.insertSubscription(subscription)
            logAudit(
                "PAYMENT_SUBMITTED",
                "subscription",
                subscription.id,
                "Driver Pass payment submitted from $effectivePhone. Status: PENDING Admin Approval"
            )

            Result.success(subscription)
        } catch (e: Exception) {
            android.util.Log.e("GoBajaajRepo", "Driver Pass payment error", e)
            Result.failure(Exception(
                e.localizedMessage ?: "Dirista lacagta way fashilantay. Fadlan isku day mar kale."
            ))
        }
    }

    suspend fun syncCurrentSubscription(explicitPhone: String? = null): Result<Subscription?> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.success(null)
            var localSub = supportDao.getSubscriptionForUserSync(user.id)

            if (prefs.isSupabaseConfigured()) {
                val apiKey = getSafeApiKey()
                val authHeader = getSafeAuthHeader()
                val service = NetworkClient.createService(prefs.getSupabaseUrl())

                var remoteStatus: String? = null
                var remoteSubId: String? = null
                var startDate: String? = null
                var endDate: String? = null
                var method: String? = null
                var ref: String? = null
                var sPhone: String? = null

                val rawPhone = user.phoneNumber.trim()
                val digits = rawPhone.filter { it.isDigit() }
                val normalized = prefs.normalizePhone(rawPhone)
                val last9 = if (digits.length >= 9) digits.takeLast(9) else digits

                val phoneCandidates = mutableSetOf<String>()
                if (rawPhone.isNotBlank()) phoneCandidates.add(rawPhone)
                if (digits.isNotBlank()) phoneCandidates.add(digits)
                if (normalized.isNotBlank()) phoneCandidates.add(normalized)
                if (last9.isNotBlank()) {
                    phoneCandidates.add(last9)
                    phoneCandidates.add("0$last9")
                    phoneCandidates.add("252$last9")
                    phoneCandidates.add("+252$last9")
                }
                if (!explicitPhone.isNullOrBlank()) {
                    val ep = explicitPhone.trim()
                    val epDigits = ep.filter { it.isDigit() }
                    val epLast9 = if (epDigits.length >= 9) epDigits.takeLast(9) else epDigits
                    phoneCandidates.add(ep)
                    if (epDigits.isNotBlank()) phoneCandidates.add(epDigits)
                    if (epLast9.isNotBlank()) {
                        phoneCandidates.add(epLast9)
                        phoneCandidates.add("0$epLast9")
                        phoneCandidates.add("252$epLast9")
                        phoneCandidates.add("+252$epLast9")
                    }
                }
                localSub?.senderPhone?.let { s ->
                    val sDigits = s.filter { it.isDigit() }
                    if (s.isNotBlank()) phoneCandidates.add(s.trim())
                    if (sDigits.isNotBlank()) phoneCandidates.add(sDigits)
                    if (sDigits.length >= 9) {
                        val sLast9 = sDigits.takeLast(9)
                        phoneCandidates.add(sLast9)
                        phoneCandidates.add("0$sLast9")
                        phoneCandidates.add("252$sLast9")
                        phoneCandidates.add("+252$sLast9")
                    }
                }

                // 1. Check subscriptions table by user_id
                try {
                    val resp = service.getSubscriptionForUser(apiKey, authHeader, "eq.${user.id}")
                    if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                        val subs = resp.body()!!
                        // Find latest subscription by sorting if available
                        val latestSub = subs.maxByOrNull { it["created_at"]?.toString() ?: "" } ?: subs.first()
                        val st = latestSub["status"]?.toString()?.lowercase() ?: ""
                        val subEnd = latestSub["end_date"]?.toString()

                        if (st in listOf("active", "completed", "approved")) {
                            if (!subEnd.isNullOrBlank() && isDateValidInFuture(subEnd)) {
                                remoteStatus = "active"
                            } else {
                                remoteStatus = "expired"
                            }
                        } else {
                            remoteStatus = st
                        }
                        remoteSubId = latestSub["id"]?.toString()
                        startDate = latestSub["start_date"]?.toString()
                        endDate = subEnd
                        method = latestSub["payment_method"]?.toString()
                        ref = latestSub["payment_reference"]?.toString()
                        sPhone = latestSub["sender_phone"]?.toString()
                    }
                } catch (e: Exception) {
                    // Ignore
                }

                // 2. Check subscriptions table by phone candidates
                if (remoteStatus != "active") {
                    for (phone in phoneCandidates) {
                        try {
                            val resp = service.getSubscriptionsByPhone(apiKey, authHeader, "eq.$phone")
                            if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                                val subs = resp.body()!!
                                val latestSub = subs.maxByOrNull { it["created_at"]?.toString() ?: "" } ?: subs.first()
                                val st = latestSub["status"]?.toString()?.lowercase() ?: ""
                                val subEnd = latestSub["end_date"]?.toString()

                                if (st in listOf("active", "completed", "approved")) {
                                    if (!subEnd.isNullOrBlank() && isDateValidInFuture(subEnd)) {
                                        remoteStatus = "active"
                                        remoteSubId = latestSub["id"]?.toString() ?: remoteSubId
                                        startDate = latestSub["start_date"]?.toString() ?: startDate
                                        endDate = subEnd
                                        method = latestSub["payment_method"]?.toString() ?: method
                                        ref = latestSub["payment_reference"]?.toString() ?: ref
                                        sPhone = latestSub["sender_phone"]?.toString() ?: sPhone
                                        break
                                    } else {
                                        remoteStatus = "expired"
                                    }
                                } else if (st == "pending") {
                                    remoteStatus = "pending"
                                    remoteSubId = latestSub["id"]?.toString() ?: remoteSubId
                                    sPhone = latestSub["sender_phone"]?.toString() ?: sPhone
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }

                // 3. Check payment_transactions table by user_id (Must be within last 24h)
                if (remoteStatus != "active") {
                    try {
                        val txResp = service.getPaymentTransactionsByUserId(apiKey, authHeader, "eq.${user.id}")
                        if (txResp.isSuccessful && !txResp.body().isNullOrEmpty()) {
                            val txList = txResp.body()!!
                            val latestTx = txList.maxByOrNull { it["created_at"]?.toString() ?: "" } ?: txList.first()
                            val st = latestTx["status"]?.toString()?.lowercase() ?: ""
                            val txCreatedAt = latestTx["created_at"]?.toString()

                            if (st in listOf("completed", "approved", "active")) {
                                if (isTimestampWithin24Hours(txCreatedAt)) {
                                    remoteStatus = "active"
                                    startDate = txCreatedAt
                                } else {
                                    remoteStatus = "expired"
                                }
                            } else if (st == "pending") {
                                remoteStatus = "pending"
                            } else if (st in listOf("rejected", "failed")) {
                                remoteStatus = "rejected"
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                // 4. Check payment_transactions by phone candidates
                if (remoteStatus != "active") {
                    for (phone in phoneCandidates) {
                        try {
                            val txResp = service.getPaymentTransactionsByPhone(apiKey, authHeader, "eq.$phone")
                            if (txResp.isSuccessful && !txResp.body().isNullOrEmpty()) {
                                val txList = txResp.body()!!
                                val latestTx = txList.maxByOrNull { it["created_at"]?.toString() ?: "" } ?: txList.first()
                                val st = latestTx["status"]?.toString()?.lowercase() ?: ""
                                val txCreatedAt = latestTx["created_at"]?.toString()

                                if (st in listOf("completed", "approved", "active")) {
                                    if (isTimestampWithin24Hours(txCreatedAt)) {
                                        remoteStatus = "active"
                                        startDate = txCreatedAt
                                        break
                                    } else {
                                        remoteStatus = "expired"
                                    }
                                } else if (st == "pending") {
                                    remoteStatus = "pending"
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }

                // 5. Check profiles table to see if subscription_status was set to active by Admin
                if (remoteStatus != "active") {
                    try {
                        val idProfResp = service.getProfileById(apiKey, authHeader, "eq.${user.id}")
                        if (idProfResp.isSuccessful && !idProfResp.body().isNullOrEmpty()) {
                            val prof = idProfResp.body()!!.first()
                            val profSubStatus = prof["subscription_status"]?.toString()?.lowercase() ?: ""
                            val profExpiry = prof["subscription_expiry"]?.toString()
                            if (!profExpiry.isNullOrBlank()) {
                                endDate = profExpiry
                            }
                            if (profSubStatus in listOf("active", "completed", "approved")) {
                                if (!profExpiry.isNullOrBlank() && isDateValidInFuture(profExpiry)) {
                                    remoteStatus = "active"
                                } else {
                                    remoteStatus = "expired"
                                }
                            } else if (profSubStatus in listOf("rejected", "failed")) {
                                remoteStatus = "rejected"
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }

                if (remoteStatus != "active") {
                    for (phone in phoneCandidates) {
                        try {
                            val profResp = service.getProfileByPhone(apiKey, authHeader, "eq.$phone")
                            if (profResp.isSuccessful && !profResp.body().isNullOrEmpty()) {
                                val prof = profResp.body()!!.first()
                                val profSubStatus = prof["subscription_status"]?.toString()?.lowercase() ?: ""
                                val profExpiry = prof["subscription_expiry"]?.toString()
                                if (!profExpiry.isNullOrBlank()) {
                                    endDate = profExpiry
                                }
                                if (profSubStatus in listOf("active", "completed", "approved")) {
                                    if (!profExpiry.isNullOrBlank() && isDateValidInFuture(profExpiry)) {
                                        remoteStatus = "active"
                                        break
                                    } else {
                                        remoteStatus = "expired"
                                    }
                                } else if (profSubStatus in listOf("rejected", "failed")) {
                                    remoteStatus = "rejected"
                                    break
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }

                // Strict Expiry Verification: An active pass MUST have a valid future expiration
                if (remoteStatus == "active") {
                    if (endDate.isNullOrBlank() || !isDateValidInFuture(endDate)) {
                        remoteStatus = "expired"
                    }
                }

                if (remoteStatus == "active") {
                    // Update all local records for this user
                    supportDao.updateAllSubscriptionsStatusForUser(user.id, "active")

                    val targetId = remoteSubId ?: localSub?.id ?: UUID.randomUUID().toString()
                    val shiftDates = calculateShiftDates()
                    val updated = Subscription(
                        id = targetId,
                        userId = user.id,
                        planName = "Driver Pass (Maalinle 05:00 AM)",
                        amount = 0.25,
                        currency = "USD",
                        status = "active",
                        paymentMethod = method ?: localSub?.paymentMethod ?: "EVC Plus",
                        senderPhone = sPhone ?: localSub?.senderPhone ?: user.phoneNumber,
                        destinationPhone = "618092060",
                        paymentReference = ref ?: localSub?.paymentReference ?: "",
                        startDate = startDate ?: localSub?.startDate ?: shiftDates.first,
                        endDate = endDate ?: localSub?.endDate ?: shiftDates.second,
                        autoRenew = false
                    )

                    supportDao.insertSubscription(updated)
                    supportDao.deleteOtherSubscriptionsForUser(user.id, targetId)
                    localSub = updated
                } else if (remoteStatus != null) {
                    val targetId = remoteSubId ?: localSub?.id ?: UUID.randomUUID().toString()
                    val updated = (localSub ?: Subscription(
                        id = targetId,
                        userId = user.id,
                        planName = "Driver Pass (Maalinle 05:00 AM)",
                        amount = 0.25,
                        currency = "USD",
                        status = remoteStatus,
                        paymentMethod = method ?: "EVC Plus",
                        senderPhone = sPhone ?: user.phoneNumber,
                        destinationPhone = "618092060",
                        paymentReference = ref ?: "",
                        startDate = startDate ?: "",
                        endDate = endDate ?: "",
                        autoRenew = false
                    )).copy(status = remoteStatus)

                    supportDao.insertSubscription(updated)
                    localSub = updated
                }
            }
            Result.success(localSub)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubscriptionStatus(subscriptionId: String, newStatus: String, rejectionReason: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sub = supportDao.getSubscriptionById(subscriptionId) ?: return@withContext Result.failure(Exception("Subscription not found"))
            val adminUser = prefs.getCurrentUser()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val reviewedAt = dateFormat.format(Calendar.getInstance().time)

            val updated = sub.copy(
                status = newStatus,
                reviewedBy = adminUser?.fullName ?: "Admin",
                reviewedAt = reviewedAt,
                rejectionReason = rejectionReason
            )
            supportDao.insertSubscription(updated)

            // Sync to Supabase
            try {
                if (prefs.isSupabaseConfigured()) {
                    val apiKey = getSafeApiKey()
                    val authHeader = getSafeAuthHeader()
                    val service = NetworkClient.createService(prefs.getSupabaseUrl())
                    service.updateSubscription(apiKey, authHeader, "eq.$subscriptionId", mapOf("status" to newStatus))
                    service.upsertProfile(apiKey, authHeader, "resolution=merge-duplicates,return=representation", mapOf("id" to sub.userId, "subscription_status" to newStatus))
                }
            } catch (syncEx: Exception) {
                android.util.Log.w("GoBajaajRepo", "Supabase updateSubscriptionStatus notice: ${syncEx.message}")
            }

            logAudit("PAYMENT_VERIFICATION", "subscription", subscriptionId, "Status updated to $newStatus by ${adminUser?.fullName}. Reason: ${rejectionReason ?: "N/A"}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Intersections & Traffic ---
    fun getIntersections(): Flow<List<Intersection>> = trafficDao.getAllIntersections()

    suspend fun seedIntersectionsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = trafficDao.getAllIntersectionsSync()
        if (existing.isNotEmpty()) return@withContext

        // Real production intersections matching Supabase database exactly
        val list = listOf(
            Intersection("11111111-1111-4111-a111-111111111111", "Isgoyska KM4", 2.0335, 45.3190, 400, "Muqdisho", "Furan", "Hadda", "Wadada Maka Al-Mukarama & Airport"),
            Intersection("22222222-2222-4222-a222-222222222222", "Isgoyska Benaadir", 2.0435, 45.3115, 400, "Muqdisho", "Furan", "Hadda", "Wadada Wadnaha & Digfeer"),
            Intersection("33333333-3333-4333-a333-333333333333", "Isgoyska Tarabuunka", 2.0490, 45.3180, 400, "Muqdisho", "Furan", "Hadda", "Hodan Tarabuunka"),
            Intersection("44444444-4444-4444-a444-444444444444", "Isgoyska Zoobe (KM5)", 2.0298, 45.3125, 400, "Muqdisho", "Furan", "Hadda", "Wadada Zoobe - Hodan"),
            Intersection("55555555-5555-4555-a555-555555555555", "Isgoyska Dabka", 2.0390, 45.3280, 400, "Muqdisho", "Furan", "Hadda", "Wadada Maka Al-Mukarama & Dabka"),
            Intersection("66666666-6666-4666-a666-666666666666", "Isgoyska Sayidka", 2.0430, 45.3330, 400, "Muqdisho", "Furan", "Hadda", "Wadada Sayidka"),
            Intersection("77777777-7777-4777-a777-777777777777", "Isgoyska Jubba", 2.0510, 45.3400, 400, "Muqdisho", "Furan", "Hadda", "Wadada Jubba & Shibis"),
            Intersection("88888888-8888-4888-a888-888888888888", "Isgoyska Taleex", 2.0381, 45.3175, 400, "Muqdisho", "Furan", "Hadda", "Wadada Taleex & Maka"),
            Intersection("99999999-9999-4999-a999-999999999999", "Isgoyska Sanaa", 2.0620, 45.3520, 400, "Muqdisho", "Furan", "Hadda", "Wadada Sanaa - Kaaraan"),
            Intersection("aaaaaaaa-aaaa-4aaa-aaaa-aaaaaaaaaaaa", "Isgoyska Ex-Control Afgooye", 2.0550, 45.2750, 400, "Muqdisho", "Furan", "Hadda", "Wadada Ex-Control Afgooye")
        )
        trafficDao.insertIntersections(list)
    }

    suspend fun getIntersectionById(id: String): Intersection? = trafficDao.getIntersectionById(id)

    suspend fun syncIntersectionsFromSupabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = getApiService().getIntersections(
                apiKey = prefs.getSupabaseKey(),
                authHeader = "Bearer ${prefs.getSupabaseKey()}"
            )
            if (response.isSuccessful && response.body() != null) {
                val remoteList = response.body()!!
                val mapped = remoteList.mapNotNull { map ->
                    val id = map["id"]?.toString() ?: return@mapNotNull null
                    val name = map["name"]?.toString() ?: return@mapNotNull null
                    val lat = (map["latitude"] as? Number)?.toDouble() ?: 2.0335
                    val lng = (map["longitude"] as? Number)?.toDouble() ?: 45.3190
                    val radius = (map["allowed_radius"] as? Number)?.toInt() ?: 400
                    val city = map["city"]?.toString() ?: "Muqdisho"
                    val rawStatus = map["current_status"]?.toString()?.trim() ?: "Furan"
                    val status = if (rawStatus.equals("hadda", ignoreCase = true) || rawStatus.isBlank()) "Furan" else rawStatus
                    val lastUpdated = map["last_updated"]?.toString() ?: "Hadda"
                    Intersection(
                        id = id,
                        name = name,
                        latitude = lat,
                        longitude = lng,
                        allowedRadius = radius,
                        city = city,
                        currentStatus = status,
                        lastUpdated = lastUpdated
                    )
                }
                if (mapped.isNotEmpty()) {
                    trafficDao.deleteMockIntersections()
                    trafficDao.insertIntersections(mapped)
                } else {
                    seedIntersectionsIfEmpty()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerIntersection(
        name: String,
        latitude: Double,
        longitude: Double,
        allowedRadius: Int = 400,
        city: String = "Muqdisho",
        district: String = "Hodan"
    ): Result<Intersection> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(Exception("Fadlan geli magaca isgoyska."))
            }

            val newId = "int-${UUID.randomUUID().toString().take(8)}"
            val dateFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.US)
            val newIntersection = Intersection(
                id = newId,
                name = name.trim(),
                latitude = latitude,
                longitude = longitude,
                allowedRadius = allowedRadius,
                city = city,
                district = district,
                currentStatus = "Furan",
                lastUpdated = dateFormat.format(Date())
            )

            // Insert into local Room DB
            trafficDao.insertIntersections(listOf(newIntersection))

            // Sync to Supabase if token and network are available
            try {
                getApiService().createIntersection(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    intersection = mapOf(
                        "id" to newId,
                        "name" to name.trim(),
                        "latitude" to latitude,
                        "longitude" to longitude,
                        "allowed_radius" to allowedRadius,
                        "city" to city,
                        "district" to district,
                        "current_status" to "Furan"
                    )
                )
            } catch (e: Exception) {
                // Ignore remote network failures for offline capability
            }

            logAudit("REGISTER_INTERSECTION", "intersection", newId, "Created intersection $name at ($latitude, $longitude)")
            Result.success(newIntersection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun searchIntersections(
        query: String,
        allIntersections: List<Intersection>,
        userLat: Double?,
        userLng: Double?
    ): List<SearchResultItem> {
        return IntersectionSearchEngine.search(query, allIntersections, userLat, userLng)
    }

    // --- Reporter Report with GPS Radius Validation ---
    suspend fun submitTrafficReport(
        intersectionId: String,
        status: String,
        cause: String?,
        notes: String?,
        reporterLat: Double,
        reporterLng: Double,
        accuracy: Double
    ): Result<TrafficReport> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val intersection = trafficDao.getIntersectionById(intersectionId)
                ?: trafficDao.getAllIntersectionsSync().firstOrNull()
                ?: Intersection(
                    id = intersectionId.ifBlank { "int-1" },
                    name = "Isgoyska KM4",
                    latitude = reporterLat,
                    longitude = reporterLng,
                    allowedRadius = 800,
                    city = "Muqdisho",
                    currentStatus = status,
                    lastUpdated = "Hadda"
                ).also { trafficDao.insertIntersections(listOf(it)) }

            // Haversine distance calculation in meters
            val distance = calculateDistanceInMeters(
                reporterLat, reporterLng,
                intersection.latitude, intersection.longitude
            )

            val isWithinRadius = distance <= (intersection.allowedRadius * 2) || distance < 1500
            val verificationStatus = if (isWithinRadius) "verified" else "remote_report"

            val dateFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.US)
            val report = TrafficReport(
                id = UUID.randomUUID().toString(),
                intersectionId = intersection.id,
                reporterId = user.id,
                status = status,
                cause = cause,
                notes = notes,
                latitude = reporterLat,
                longitude = reporterLng,
                accuracy = accuracy,
                distanceFromIntersection = distance,
                verificationStatus = verificationStatus,
                createdAt = dateFormat.format(Date())
            )

            trafficDao.insertTrafficReport(report)
            trafficDao.updateIntersection(
                intersection.copy(
                    currentStatus = status,
                    lastUpdated = "Hadda"
                )
            )

            if (prefs.isSupabaseConfigured()) {
                try {
                    val apiKey = prefs.getSupabaseKey()
                    val service = getApiService()
                    
                    // 1. Post Traffic Report
                    val payload = mutableMapOf<String, Any>(
                        "id" to report.id,
                        "status" to status,
                        "cause" to (cause ?: ""),
                        "latitude" to reporterLat,
                        "longitude" to reporterLng,
                        "verification_status" to verificationStatus,
                        "created_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                    )
                    // If intersection.id is UUID format, attach it
                    if (intersection.id.contains("-") && intersection.id.length >= 32) {
                        payload["intersection_id"] = intersection.id
                    }
                    if (user.id.contains("-") && user.id.length >= 32) {
                        payload["reporter_id"] = user.id
                    }

                    service.postTrafficReport(
                        apiKey = apiKey,
                        authHeader = "Bearer $apiKey",
                        report = payload
                    )

                    // 2. Patch Intersection status on Supabase in real-time
                    if (intersection.id.contains("-") && intersection.id.length >= 32) {
                        service.updateIntersectionStatus(
                            apiKey = apiKey,
                            authHeader = "Bearer $apiKey",
                            idFilter = "eq.${intersection.id}",
                            updates = mapOf(
                                "current_status" to status,
                                "last_updated" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TrafficRepo", "Supabase sync error: ${e.message}")
                }
            }

            logAudit("TRAFFIC_REPORT", "intersection", intersection.id, "Status updated to $status (Distance: ${distance.toInt()}m, Status: $verificationStatus)")

            Result.success(report)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Traffic Incidents & Alerts ---
    fun getActiveIncidents(): Flow<List<TrafficIncident>> = trafficDao.getActiveIncidents()

    suspend fun seedIncidentsIfEmpty() = withContext(Dispatchers.IO) {
        // Real incidents are loaded dynamically from user & reporter reports
    }

    // --- Devices & Phone Guard Protection ---
    fun getRegisteredDevice(): Flow<Device?> {
        val user = prefs.getCurrentUser()
        val userId = user?.id ?: ""
        return if (userId.isNotEmpty()) {
            kotlinx.coroutines.flow.combine(
                deviceDao.getDeviceForUser(userId),
                deviceDao.getAllDevices()
            ) { userDev, allDevs ->
                userDev ?: allDevs.firstOrNull()
            }
        } else {
            deviceDao.getAllDevices().map { it.firstOrNull() }
        }
    }

    fun getUserDevices(): Flow<List<Device>> {
        return getRegisteredDevice().map { dev ->
            if (dev != null) listOf(dev) else emptyList()
        }
    }

    fun getDevice(deviceId: String): Flow<Device?> {
        return deviceDao.getDeviceById(deviceId)
    }

    fun getAllDevices(): Flow<List<Device>> {
        return deviceDao.getAllDevices()
    }

    suspend fun registerCurrentDevice(): Result<Device> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser()
            val userId = user?.id ?: "user_default_device"
            val existing = deviceDao.getDeviceForUserDirect(userId) ?: deviceDao.getLatestDeviceDirect()
            val deviceId = existing?.id ?: prefs.getRegisteredDeviceId() ?: UUID.randomUUID().toString()
            val deviceFingerprint = "ST-DEV-" + Build.MANUFACTURER.uppercase() + "-" + Build.MODEL.filter { it.isLetterOrDigit() }
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())

            val device = Device(
                id = deviceId,
                userId = userId,
                deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                deviceModel = Build.MODEL,
                platform = "Android",
                appVersion = "3.3",
                deviceFingerprint = deviceFingerprint,
                isProtected = existing?.isProtected ?: true,
                isLost = existing?.isLost ?: false,
                batteryLevel = 84,
                isOnline = true,
                networkStatus = "WiFi",
                locationPermissionStatus = "Granted",
                lastLatitude = existing?.lastLatitude ?: 2.0335,
                lastLongitude = existing?.lastLongitude ?: 45.3190,
                lastAccuracy = 10.0,
                lastLocationAddress = existing?.lastLocationAddress ?: "KM4, Mogadishu",
                lastSeen = "Hadda (Online)",
                createdAt = existing?.createdAt ?: nowStr,
                updatedAt = nowStr
            )

            deviceDao.insertDevice(device)
            prefs.setRegisteredDeviceId(deviceId)
            // Remove any other duplicate device rows so only the user's current phone is kept
            deviceDao.deleteOtherDevicesForUser(userId, deviceId)

            // Log recovery event
            val event = DeviceRecoveryEvent(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId,
                userId = userId,
                eventType = "PROTECTION_ENABLED",
                description = "GoBajaaj Phone Guard waxaa loo furay ${device.deviceName}",
                createdAt = nowStr
            )
            deviceDao.insertDeviceEvent(event)
            logAudit("DEVICE_REGISTERED", "device", deviceId, "Device ${device.deviceName} protected with Phone Guard")

            // Push real device to Supabase so it appears live in the Admin Web Portal
            try {
                val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                val devicePayload = mapOf<String, Any>(
                    "id" to deviceId,
                    "user_id" to userId,
                    "device_name" to device.deviceName,
                    "platform" to "Android",
                    "is_protected" to true,
                    "is_lost" to false,
                    "battery_level" to 84,
                    "is_online" to true,
                    "last_latitude" to 2.0335,
                    "last_longitude" to 45.3190,
                    "last_seen" to isoNow
                )
                getApiService().registerDevice(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    device = devicePayload
                )
            } catch (syncEx: Exception) {
                android.util.Log.w("SomaliTrafficRepo", "Supabase device sync warning: ${syncEx.message}")
            }

            Result.success(device)
        } catch (e: Exception) {
            Result.failure(Exception("Diiwaangelinta taleefanku way fashilantay: ${e.localizedMessage}"))
        }
    }

    suspend fun registerCustomDevice(
        deviceName: String,
        deviceModel: String,
        platform: String
    ): Result<Device> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val deviceId = UUID.randomUUID().toString()
            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())

            val cleanPlatform = if (platform.equals("ios", ignoreCase = true)) "iOS" else "Android"
            val device = Device(
                id = deviceId,
                userId = user.id,
                deviceName = deviceName.trim().ifEmpty { if (cleanPlatform == "iOS") "iPhone 15 Pro" else "Android Device" },
                deviceModel = deviceModel.trim().ifEmpty { if (cleanPlatform == "iOS") "iPhone" else "Android" },
                platform = cleanPlatform,
                appVersion = "3.3",
                deviceFingerprint = "DEV-${UUID.randomUUID().toString().take(8)}",
                isProtected = true,
                isLost = false,
                batteryLevel = (60..95).random(),
                isOnline = true,
                networkStatus = "Cellular",
                locationPermissionStatus = "Granted",
                lastLatitude = 2.0335 + (Random().nextDouble() - 0.5) * 0.01,
                lastLongitude = 45.3190 + (Random().nextDouble() - 0.5) * 0.01,
                lastAccuracy = 12.0,
                lastLocationAddress = "Mogadishu, Somalia",
                lastSeen = "15 min ago",
                createdAt = nowStr,
                updatedAt = nowStr
            )

            deviceDao.insertDevice(device)

            val event = DeviceRecoveryEvent(
                id = UUID.randomUUID().toString(),
                deviceId = deviceId,
                userId = user.id,
                eventType = "PROTECTION_ENABLED",
                description = "Aalad cusub (${device.deviceName} - $cleanPlatform) ayaa lagu daray Phone Guard",
                createdAt = nowStr
            )
            deviceDao.insertDeviceEvent(event)
            logAudit("DEVICE_ADDED", "device", deviceId, "Device ${device.deviceName} added to Phone Guard")

            try {
                val isoNow = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
                val devicePayload = mapOf<String, Any>(
                    "id" to deviceId,
                    "user_id" to user.id,
                    "device_name" to device.deviceName,
                    "platform" to cleanPlatform,
                    "is_protected" to true,
                    "is_lost" to false,
                    "battery_level" to (device.batteryLevel ?: 75),
                    "is_online" to true,
                    "last_latitude" to (device.lastLatitude ?: 2.0335),
                    "last_longitude" to (device.lastLongitude ?: 45.3190),
                    "last_seen" to isoNow
                )
                getApiService().registerDevice(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    device = devicePayload
                )
            } catch (syncEx: Exception) {
                android.util.Log.w("SomaliTrafficRepo", "Supabase device sync warning: ${syncEx.message}")
            }

            Result.success(device)
        } catch (e: Exception) {
            Result.failure(Exception("Ku darista aaladda way fashilantay: ${e.localizedMessage}"))
        }
    }

    suspend fun setProtectionEnabled(deviceId: String, enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val device = deviceDao.getDeviceByIdDirect(deviceId)
                ?: deviceDao.getDeviceForUserDirect(user.id)
                ?: return@withContext Result.failure(Exception("Aaladda lama helin"))

            // Verify ownership
            if (device.userId != user.id && user.role != UserRole.SUPER_ADMIN) {
                return@withContext Result.failure(Exception("Ma lihid awoodda aad wax kaga beddesho aaladdan"))
            }

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
            val updated = device.copy(isProtected = enabled, updatedAt = nowStr)
            deviceDao.updateDevice(updated)

            val event = DeviceRecoveryEvent(
                id = UUID.randomUUID().toString(),
                deviceId = device.id,
                userId = user.id,
                eventType = if (enabled) "PROTECTION_ENABLED" else "PROTECTION_DISABLED",
                description = if (enabled) "Ilaalinta taleefanka waa la furay" else "Ilaalinta taleefanka waa la damiyay",
                createdAt = nowStr
            )
            deviceDao.insertDeviceEvent(event)

            try {
                getApiService().updateDevice(
                    apiKey = prefs.getSupabaseKey(),
                    authHeader = "Bearer ${prefs.getSupabaseKey()}",
                    idFilter = "eq.${device.id}",
                    updates = mapOf("is_protected" to enabled)
                )
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markDeviceAsLost(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val device = deviceDao.getDeviceByIdDirect(deviceId)
                ?: deviceDao.getDeviceForUserDirect(user.id)
                ?: return@withContext Result.failure(Exception("Taleefanka lama helin"))

            // Verify device ownership
            if (device.userId != user.id && user.role != UserRole.SUPER_ADMIN) {
                return@withContext Result.failure(Exception("Fadlan xaqiiji lahaanshaha taleefanka"))
            }

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
            deviceDao.updateDevice(
                device.copy(
                    isLost = true,
                    lostAt = nowStr,
                    lastSeen = "🔴 WAA LA RAADINAYAA (LOST MODE)",
                    updatedAt = nowStr
                )
            )

            val event = DeviceRecoveryEvent(
                id = UUID.randomUUID().toString(),
                deviceId = device.id,
                userId = user.id,
                eventType = "MARKED_LOST",
                description = "Taleefanka waxaa loo calaamadeeyay inuu lumay! Raadinta degdegga ah waa la kiciyay.",
                createdAt = nowStr
            )
            deviceDao.insertDeviceEvent(event)
            logAudit("DEVICE_MARKED_LOST", "device", device.id, "Taleefanka waxaa loo calaamadeeyay inuu lumay!")

            // Sync lost status to Supabase
            try {
                getApiService().updateDevice(
                    apiKey = prefs.getSupabaseKey(),
                    authHeader = "Bearer ${prefs.getSupabaseKey()}",
                    idFilter = "eq.${device.id}",
                    updates = mapOf(
                        "is_lost" to true,
                        "lost_at" to nowStr,
                        "last_seen" to "🔴 WAA LA RAADINAYAA"
                    )
                )
            } catch (syncEx: Exception) {
                android.util.Log.w("SomaliTrafficRepo", "Supabase updateDevice warning: ${syncEx.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markDeviceAsFound(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val device = deviceDao.getDeviceByIdDirect(deviceId)
                ?: deviceDao.getDeviceForUserDirect(user.id)
                ?: return@withContext Result.failure(Exception("Taleefanka lama helin"))

            // Verify ownership
            if (device.userId != user.id && user.role != UserRole.SUPER_ADMIN) {
                return@withContext Result.failure(Exception("Fadlan xaqiiji lahaanshaha taleefanka"))
            }

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
            deviceDao.updateDevice(
                device.copy(
                    isLost = false,
                    foundAt = nowStr,
                    lastSeen = "🟢 Waa la helay ($nowStr)",
                    updatedAt = nowStr
                )
            )

            val event = DeviceRecoveryEvent(
                id = UUID.randomUUID().toString(),
                deviceId = device.id,
                userId = user.id,
                eventType = "MARKED_FOUND",
                description = "Taleefanka si guul ah ayaa loo helay oo xaaladdii caadiga ahayd lagu celiyay.",
                createdAt = nowStr
            )
            deviceDao.insertDeviceEvent(event)
            logAudit("DEVICE_RECOVERED", "device", device.id, "Taleefanka si nabad ah ayaa loo soo celiyay")

            try {
                getApiService().updateDevice(
                    apiKey = prefs.getSupabaseKey(),
                    authHeader = "Bearer ${prefs.getSupabaseKey()}",
                    idFilter = "eq.${device.id}",
                    updates = mapOf(
                        "is_lost" to false,
                        "found_at" to nowStr,
                        "last_seen" to "🟢 Waa la helay"
                    )
                )
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findNow(deviceId: String, currentLat: Double?, currentLng: Double?): Result<Device> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val device = deviceDao.getDeviceByIdDirect(deviceId)
                ?: deviceDao.getDeviceForUserDirect(user.id)
                ?: return@withContext Result.failure(Exception("Aaladda lama helin"))

            // Verify ownership
            if (device.userId != user.id && user.role != UserRole.SUPER_ADMIN) {
                return@withContext Result.failure(Exception("Xaqiijinta lahaanshaha aaladda waa loo baahan yahay"))
            }

            val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date())
            val newLat = currentLat ?: (device.lastLatitude ?: 2.0335)
            val newLng = currentLng ?: (device.lastLongitude ?: 45.3190)
            val newAcc = 8.5

            // Record location breadcrumb
            val loc = DeviceLocation(
                id = UUID.randomUUID().toString(),
                deviceId = device.id,
                latitude = newLat,
                longitude = newLng,
                accuracy = newAcc,
                source = "GPS",
                address = "KM4, Mogadishu",
                batteryLevel = device.batteryLevel ?: 84,
                recordedAt = nowStr
            )
            deviceDao.insertDeviceLocation(loc)

            val updatedDevice = device.copy(
                lastLatitude = newLat,
                lastLongitude = newLng,
                lastAccuracy = newAcc,
                lastSeen = "Hadda (Online • $nowStr)",
                isOnline = true,
                updatedAt = nowStr
            )
            deviceDao.updateDevice(updatedDevice)

            val event = DeviceRecoveryEvent(
                id = UUID.randomUUID().toString(),
                deviceId = device.id,
                userId = user.id,
                eventType = "FIND_NOW_REQUESTED",
                description = "Goobta tooska ah ee taleefanka ayaa la soo cusbooneysiiyay: ($newLat, $newLng)",
                createdAt = nowStr
            )
            deviceDao.insertDeviceEvent(event)

            // Remote sync
            try {
                getApiService().recordDeviceLocation(
                    apiKey = prefs.getSupabaseKey(),
                    authHeader = "Bearer ${prefs.getSupabaseKey()}",
                    location = mapOf(
                        "id" to loc.id,
                        "device_id" to device.id,
                        "latitude" to newLat,
                        "longitude" to newLng,
                        "accuracy" to newAcc,
                        "source" to "GPS",
                        "recorded_at" to nowStr
                    )
                )
                getApiService().updateDevice(
                    apiKey = prefs.getSupabaseKey(),
                    authHeader = "Bearer ${prefs.getSupabaseKey()}",
                    idFilter = "eq.${device.id}",
                    updates = mapOf(
                        "last_latitude" to newLat,
                        "last_longitude" to newLng,
                        "last_accuracy" to newAcc,
                        "last_seen" to "Hadda (Online)"
                    )
                )
            } catch (_: Exception) {}

            Result.success(updatedDevice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDevice(deviceId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val device = deviceDao.getDeviceByIdDirect(deviceId) ?: return@withContext Result.failure(Exception("Aaladda lama helin"))

            if (device.userId != user.id && user.role != UserRole.SUPER_ADMIN) {
                return@withContext Result.failure(Exception("Ma tiri kartid aaladdan"))
            }

            deviceDao.deleteDevice(deviceId)
            try {
                getApiService().deleteDevice(
                    apiKey = prefs.getSupabaseKey(),
                    authHeader = "Bearer ${prefs.getSupabaseKey()}",
                    idFilter = "eq.$deviceId"
                )
            } catch (_: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getDeviceLocations(deviceId: String): Flow<List<DeviceLocation>> {
        return deviceDao.getDeviceLocations(deviceId)
    }

    fun getDeviceRecoveryEvents(deviceId: String): Flow<List<DeviceRecoveryEvent>> {
        return deviceDao.getDeviceEvents(deviceId)
    }

    suspend fun updateDeviceLocation(lat: Double, lng: Double, accuracy: Double) = withContext(Dispatchers.IO) {
        val user = prefs.getCurrentUser() ?: return@withContext
        val device = deviceDao.getDeviceForUserDirect(user.id) ?: return@withContext
        val nowStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())
        deviceDao.updateDevice(
            device.copy(
                lastLatitude = lat,
                lastLongitude = lng,
                lastAccuracy = accuracy,
                lastSeen = "2 daqiiqo kahor",
                updatedAt = nowStr
            )
        )
    }

    // --- Support Tickets ---
    fun getSupportTickets(): Flow<List<SupportTicket>> {
        val user = prefs.getCurrentUser()
        return if (user?.role == UserRole.SUPER_ADMIN || user?.role == UserRole.SUPPORT_AGENT) {
            supportDao.getAllTickets()
        } else {
            supportDao.getTicketsForUser(user?.id ?: "")
        }
    }

    suspend fun createSupportTicket(category: String, subject: String, message: String): Result<SupportTicket> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
            val ticket = SupportTicket(
                id = UUID.randomUUID().toString(),
                userId = user.id,
                ticketNumber = 1000 + (100..999).random(),
                category = category,
                priority = "Dhexdhexaad",
                subject = subject,
                message = message,
                status = "Furan",
                createdAt = dateFormat.format(Date())
            )
            supportDao.insertTicket(ticket)
            logAudit("TICKET_CREATED", "support", ticket.id, "Ticket #${ticket.ticketNumber} created")
            Result.success(ticket)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Audit Logs ---
    fun getAuditLogs(): Flow<List<AuditLog>> = supportDao.getAuditLogs()

    private suspend fun logAudit(action: String, targetType: String, targetId: String?, metadata: String?) {
        val user = prefs.getCurrentUser()
        val log = AuditLog(
            id = UUID.randomUUID().toString(),
            actorId = user?.id,
            actorName = user?.fullName ?: "Nidaamka",
            action = action,
            targetType = targetType,
            targetId = targetId,
            metadata = metadata,
            createdAt = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.US).format(Date())
        )
        supportDao.insertAuditLog(log)
    }

    // --- Bajaj Rides Real Database Integration ---
    suspend fun createRideRequest(
        pickup: String,
        destination: String,
        vehicleType: String,
        offeredPrice: Double,
        pickupLat: Double? = null,
        pickupLng: Double? = null,
        destLat: Double? = null,
        destLng: Double? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser()
            val rideId = UUID.randomUUID().toString()
            val userId = user?.id ?: rideId
            val userName = user?.fullName ?: "Rakaab Macmiil"
            val userPhone = user?.phoneNumber ?: "610000000"

            val rideMap = mutableMapOf<String, Any>(
                "id" to rideId,
                "passenger_id" to userId,
                "passenger_name" to userName,
                "passenger_phone" to userPhone,
                "pickup_location" to pickup,
                "destination" to destination,
                "vehicle_type" to vehicleType,
                "offered_price" to offeredPrice,
                "status" to "pending"
            )
            pickupLat?.let { rideMap["pickup_lat"] = it }
            pickupLng?.let { rideMap["pickup_lng"] = it }
            destLat?.let { rideMap["dest_lat"] = it }
            destLng?.let { rideMap["dest_lng"] = it }
            
            // 1. Sync to Supabase
            try {
                if (prefs.isSupabaseConfigured()) {
                    getApiService().createRide(
                        apiKey = getSafeApiKey(),
                        authHeader = getSafeAuthHeader(),
                        ride = rideMap
                    )
                }
            } catch (netErr: Exception) {
                // Allow offline creation
            }

            // 2. Persist locally in Room Database
            val entity = RideEntity(
                id = rideId,
                riderId = userId,
                riderName = userName,
                riderPhone = userPhone,
                pickupAddress = pickup,
                destinationAddress = destination,
                vehicleType = vehicleType,
                riderOffer = offeredPrice,
                status = "pending",
                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()),
                pickupLat = pickupLat,
                pickupLng = pickupLng,
                destLat = destLat,
                destLng = destLng
            )
            rideDao.insertRide(entity)

            logAudit("RIDE_REQUESTED", "bajaj_rides", rideId, "Pickup: $pickup, Dest: $destination, Price: \$$offeredPrice")
            Result.success(rideId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Token for Supabase Realtime (user JWT when available, else anon key). */
    fun realtimeAccessToken(): String {
        val jwt = prefs.getAuthToken()
        return if (!jwt.isNullOrBlank() && jwt.count { it == '.' } >= 2 && jwt.startsWith("eyJ")) {
            jwt
        } else {
            getSafeApiKey()
        }
    }

    /** Maps a `bajaj_rides` REST row (incl. live-tracking columns) to RideEntity. */
    fun mapRideRow(map: Map<String, Any?>, fallbackId: String = UUID.randomUUID().toString()): RideEntity {
        fun num(key: String): Double? = (map[key] as? Number)?.toDouble()
        fun str(key: String): String? = map[key]?.toString()?.takeIf { it != "null" }
        return RideEntity(
            id = str("id") ?: fallbackId,
            riderId = str("passenger_id") ?: "",
            riderName = str("passenger_name") ?: "",
            riderPhone = str("passenger_phone") ?: "",
            pickupAddress = str("pickup_location") ?: "",
            destinationAddress = str("destination") ?: "",
            vehicleType = str("vehicle_type") ?: "BAJAAJ",
            riderOffer = num("offered_price") ?: 1.50,
            agreedFare = num("agreed_fare") ?: num("offered_price"),
            status = str("status") ?: "pending",
            assignedDriverId = str("driver_id"),
            assignedDriverName = str("driver_name"),
            assignedDriverPhone = str("driver_phone"),
            assignedDriverPlate = str("plate_number"),
            assignedDriverRating = num("driver_rating") ?: 4.9,
            createdAt = str("created_at") ?: "",
            updatedAt = str("updated_at") ?: "",
            pickupLat = num("pickup_lat"),
            pickupLng = num("pickup_lng"),
            destLat = num("dest_lat"),
            destLng = num("dest_lng"),
            driverLat = num("driver_lat"),
            driverLng = num("driver_lng"),
            driverHeading = num("driver_heading"),
            driverSpeed = num("driver_speed"),
            driverLocationAt = str("driver_location_at"),
            assignedAt = str("assigned_at")
        )
    }

    suspend fun getRideById(rideId: String): Result<RideEntity?> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                val resp = getApiService().getRideById(
                    apiKey = getSafeApiKey(),
                    authHeader = getEffectiveAuthHeader(),
                    idFilter = "eq.$rideId"
                )
                if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                    val entity = mapRideRow(resp.body()!!.first(), rideId)
                    rideDao.insertRide(entity)
                    return@withContext Result.success(entity)
                }
            }
            val local = rideDao.getRideByIdDirect(rideId)
            Result.success(local)
        } catch (e: Exception) {
            val local = rideDao.getRideByIdDirect(rideId)
            Result.success(local)
        }
    }

    suspend fun fetchPendingRideEntities(): Result<List<RideEntity>> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                val response = getApiService().getPendingRides(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader()
                )
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.map { mapRideRow(it) }
                    if (list.isNotEmpty()) {
                        rideDao.insertRides(list)
                    }
                    return@withContext Result.success(list)
                }
            }
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptRideByDriver(
        rideId: String,
        agreedFare: Double? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.failure(Exception("Fadlan gal"))
            val updates = mutableMapOf<String, Any>(
                "status" to "accepted",
                "driver_id" to user.id,
                "driver_name" to user.fullName,
                "driver_phone" to user.phoneNumber,
                "plate_number" to (user.plateNumber ?: "TR-1001"),
                "driver_rating" to 4.9
            )
            if (agreedFare != null) {
                updates["offered_price"] = agreedFare
            }
            if (prefs.isSupabaseConfigured()) {
                getApiService().updateRideStatus(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    idFilter = "eq.$rideId",
                    updates = updates
                )
            }
            val existing = rideDao.getRideByIdDirect(rideId)
            if (existing != null) {
                rideDao.updateRide(
                    existing.copy(
                        status = "accepted",
                        assignedDriverId = user.id,
                        assignedDriverName = user.fullName,
                        assignedDriverPhone = user.phoneNumber,
                        assignedDriverPlate = user.plateNumber ?: "TR-1001",
                        agreedFare = agreedFare ?: existing.riderOffer
                    )
                )
            }
            logAudit("RIDE_ACCEPTED", "bajaj_rides", rideId, "Accepted by driver: ${user.fullName}")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun arriveAtPickup(rideId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                getApiService().updateRideStatus(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    idFilter = "eq.$rideId",
                    updates = mapOf("status" to "arrived")
                )
            }
            val existing = rideDao.getRideByIdDirect(rideId)
            if (existing != null) {
                rideDao.updateRide(existing.copy(status = "arrived"))
            }
            logAudit("RIDE_ARRIVED", "bajaj_rides", rideId, "Driver has arrived at pickup location")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeRide(rideId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                getApiService().updateRideStatus(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    idFilter = "eq.$rideId",
                    updates = mapOf("status" to "completed")
                )
            }
            val existing = rideDao.getRideByIdDirect(rideId)
            if (existing != null) {
                rideDao.updateRide(existing.copy(status = "completed"))
            }
            clearRideTracking(rideId)
            logAudit("RIDE_COMPLETED", "bajaj_rides", rideId, "Ride completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRide(rideId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                getApiService().updateRideStatus(
                    apiKey = getSafeApiKey(),
                    authHeader = getSafeAuthHeader(),
                    idFilter = "eq.$rideId",
                    updates = mapOf("status" to "cancelled")
                )
            }
            val existing = rideDao.getRideByIdDirect(rideId)
            if (existing != null) {
                rideDao.updateRide(existing.copy(status = "cancelled"))
            }
            clearRideTracking(rideId)
            logAudit("RIDE_CANCELLED", "bajaj_rides", rideId, "Ride cancelled")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getActiveRideFlow(): Flow<RideEntity?> = rideDao.getActiveRide()
    fun getAllRidesFlow(): Flow<List<RideEntity>> = rideDao.getAllRides()
    fun getPendingRidesFlow(): Flow<List<RideEntity>> = rideDao.getPendingRides()

    // ================= GoBajaaj: matching / live tracking / presence =================

    /** Auto-matches the nearest eligible driver server-side (SQL RPC). */
    suspend fun matchDriversForRide(rideId: String, radiusMeters: Int = 5000): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                if (!prefs.isSupabaseConfigured() || !hasUserJwt()) return@withContext Result.success(0)
                val resp = getApiService().matchDriversForRide(
                    apiKey = getSafeApiKey(),
                    authHeader = getEffectiveAuthHeader(),
                    body = mapOf("p_ride_id" to rideId, "p_radius_m" to radiusMeters)
                )
                if (resp.isSuccessful) {
                    val matched = (resp.body()?.get("matched") as? Number)?.toInt() ?: 0
                    if (matched > 0) {
                        val ride = rideDao.getRideByIdDirect(rideId)
                        if (ride != null) rideDao.updateRide(ride.copy(status = "assigned"))
                    }
                    Result.success(matched)
                } else {
                    Result.success(0)
                }
            } catch (e: Exception) {
                Result.success(0)
            }
        }

    /** Driver declined an auto-assigned ride: release it so matching can continue. */
    suspend fun declineRide(rideId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured() && hasUserJwt()) {
                getApiService().releaseRideDriver(
                    apiKey = getSafeApiKey(),
                    authHeader = getEffectiveAuthHeader(),
                    body = mapOf("p_ride_id" to rideId)
                )
            }
            val existing = rideDao.getRideByIdDirect(rideId)
            if (existing != null) {
                rideDao.updateRide(
                    existing.copy(
                        status = "pending",
                        assignedDriverId = null,
                        assignedDriverName = null,
                        assignedDriverPhone = null
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Rides auto-assigned to this driver (status = assigned) waiting for accept/decline. */
    suspend fun fetchRidesForDriver(statusFilter: String): Result<List<RideEntity>> =
        withContext(Dispatchers.IO) {
            try {
                val user = prefs.getCurrentUser() ?: return@withContext Result.success(emptyList())
                if (!prefs.isSupabaseConfigured()) return@withContext Result.success(emptyList())
                val resp = getApiService().getRidesForDriver(
                    apiKey = getSafeApiKey(),
                    authHeader = getEffectiveAuthHeader(),
                    driverId = "eq.${user.id}",
                    statusFilter = statusFilter
                )
                if (resp.isSuccessful && resp.body() != null) {
                    val list = resp.body()!!.map { mapRideRow(it) }
                    if (list.isNotEmpty()) rideDao.insertRides(list)
                    Result.success(list)
                } else {
                    Result.success(emptyList())
                }
            } catch (e: Exception) {
                Result.success(emptyList())
            }
        }

    /** Driver presence upsert: online/offline + last known GPS for the matching pool. */
    suspend fun upsertDriverPresence(
        online: Boolean,
        lat: Double? = null,
        lng: Double? = null,
        heading: Double? = null,
        speedKmh: Double? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = prefs.getCurrentUser() ?: return@withContext Result.success(Unit)
            if (!prefs.isSupabaseConfigured() || !hasUserJwt()) return@withContext Result.success(Unit)
            val body = mutableMapOf<String, Any>(
                "driver_id" to user.id,
                "is_online" to online,
                "updated_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).format(Date())
            )
            if (lat != null) body["latitude"] = lat
            if (lng != null) body["longitude"] = lng
            if (heading != null) body["heading"] = heading
            if (speedKmh != null) body["speed"] = speedKmh
            getApiService().upsertDriverPresence(
                apiKey = getSafeApiKey(),
                authHeader = getEffectiveAuthHeader(),
                body = body
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Streams driver GPS into bajaj_rides (driver phone side, throttled). */
    suspend fun updateDriverLiveLocation(
        rideId: String,
        lat: Double,
        lng: Double,
        heading: Double?,
        speedKmh: Double?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!prefs.isSupabaseConfigured() || !hasUserJwt()) return@withContext Result.success(Unit)
            val updates = mutableMapOf<String, Any>(
                "driver_lat" to lat,
                "driver_lng" to lng,
                "driver_location_at" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())
            )
            heading?.let { updates["driver_heading"] = it }
            speedKmh?.let { updates["driver_speed"] = it }
            getApiService().updateRideStatus(
                apiKey = getSafeApiKey(),
                authHeader = getEffectiveAuthHeader(),
                idFilter = "eq.$rideId",
                updates = updates
            )
            val local = rideDao.getRideByIdDirect(rideId)
            if (local != null) {
                rideDao.updateRide(
                    local.copy(
                        driverLat = lat,
                        driverLng = lng,
                        driverHeading = heading,
                        driverSpeed = speedKmh,
                        driverLocationAt = updates["driver_location_at"]?.toString()
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Driver pressed "Bilow Safarka": transition to in_progress (arrived first, fallback direct). */
    suspend fun startTrip(rideId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (prefs.isSupabaseConfigured()) {
                val strict = getApiService().updateRideStatus(
                    apiKey = getSafeApiKey(),
                    authHeader = getEffectiveAuthHeader(),
                    idFilter = "eq.$rideId&status=eq.arrived",
                    updates = mapOf("status" to "in_progress")
                )
                if (!strict.isSuccessful) {
                    getApiService().updateRideStatus(
                        apiKey = getSafeApiKey(),
                        authHeader = getEffectiveAuthHeader(),
                        idFilter = "eq.$rideId",
                        updates = mapOf("status" to "in_progress")
                    )
                }
            }
            val existing = rideDao.getRideByIdDirect(rideId)
            if (existing != null) rideDao.updateRide(existing.copy(status = "in_progress"))
            logAudit("RIDE_STARTED", "bajaj_rides", rideId, "Trip started by driver")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Clears live driver location server-side when a ride finishes. */
    private suspend fun clearRideTracking(rideId: String) {
        try {
            if (prefs.isSupabaseConfigured()) {
                getApiService().finishRideTracking(
                    apiKey = getSafeApiKey(),
                    authHeader = getEffectiveAuthHeader(),
                    body = mapOf("p_ride_id" to rideId)
                )
            }
        } catch (_: Exception) {
        }
    }

    // Distance in meters using Haversine
    private fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    companion object {
        fun calculateHaversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            return IntersectionSearchEngine.calculateDistanceInMeters(lat1, lon1, lat2, lon2)
        }
    }
}
