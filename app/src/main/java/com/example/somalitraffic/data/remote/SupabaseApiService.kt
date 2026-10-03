package com.example.somalitraffic.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Response
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Supabase Auth Data Classes
data class SupabasePhoneOtpRequest(
    val phone: String,
    val channel: String = "sms",
    val data: Map<String, String>? = null
)

data class SupabaseVerifyOtpRequest(
    val type: String = "sms",
    val phone: String,
    val token: String
)

data class SupabaseResendOtpRequest(
    val type: String = "sms",
    val phone: String
)

data class SupabaseAuthRequest(
    val email: String,
    val password: String,
    val data: Map<String, String>? = null
)

data class SupabaseAuthResponse(
    val id: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val user_metadata: Map<String, Any>? = null,
    val access_token: String? = null,
    val token_type: String? = null,
    val expires_in: Long? = null,
    val refresh_token: String? = null,
    val user: SupabaseUser? = null
) {
    val resolvedUserId: String?
        get() = user?.id ?: id
}

data class SupabaseUser(
    val id: String,
    val email: String?,
    val phone: String?,
    val user_metadata: Map<String, Any>?
)

// Supabase PostgREST Endpoints
interface SupabaseApiService {

    // Phone + SMS OTP Endpoints
    @POST("auth/v1/otp")
    suspend fun sendPhoneOtp(
        @Header("apikey") apiKey: String,
        @Body request: SupabasePhoneOtpRequest
    ): Response<Map<String, Any>>

    @POST("auth/v1/verify")
    suspend fun verifyPhoneOtp(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseVerifyOtpRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/resend")
    suspend fun resendPhoneOtp(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseResendOtpRequest
    ): Response<Map<String, Any>>

    @POST("auth/v1/logout")
    suspend fun logout(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
    ): Response<Unit>

    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body request: SupabaseAuthRequest
    ): Response<SupabaseAuthResponse>

    @POST("rest/v1/profiles")
    suspend fun upsertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates,return=representation",
        @Body profile: @JvmSuppressWildcards Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/profiles")
    suspend fun getProfileById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/profiles")
    suspend fun getProfileByPhone(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("phone_number") phoneFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/profiles")
    suspend fun getProfilesWithOr(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("or") orFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/admin_users")
    suspend fun getAdminUsersWithOr(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("or") orFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/intersections")
    suspend fun getIntersections(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/intersections")
    suspend fun createIntersection(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body intersection: @JvmSuppressWildcards Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @PATCH("rest/v1/intersections")
    suspend fun updateIntersectionStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body updates: @JvmSuppressWildcards Map<String, Any>
    ): Response<Unit>

    @POST("rest/v1/traffic_reports")
    suspend fun postTrafficReport(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body report: @JvmSuppressWildcards Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/traffic_incidents")
    suspend fun getTrafficIncidents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/devices")
    suspend fun registerDevice(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body device: @JvmSuppressWildcards Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/devices")
    suspend fun getDevicesForUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @PATCH("rest/v1/devices")
    suspend fun updateDevice(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body updates: @JvmSuppressWildcards Map<String, Any>
    ): Response<Unit>

    @DELETE("rest/v1/devices")
    suspend fun deleteDevice(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @POST("rest/v1/device_locations")
    suspend fun recordDeviceLocation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body location: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/device_locations")
    suspend fun getDeviceLocationHistory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceIdFilter: String,
        @Query("order") order: String = "recorded_at.desc",
        @Query("limit") limit: Int = 20,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/device_recovery_events")
    suspend fun recordDeviceRecoveryEvent(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body event: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/device_recovery_events")
    suspend fun getDeviceRecoveryEvents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("device_id") deviceIdFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 30,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/audit_logs")
    suspend fun insertAuditLog(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body log: Map<String, Any>
    ): Response<Unit>

    @POST("rest/v1/subscriptions")
    suspend fun createSubscription(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body subscription: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/payment_transactions")
    suspend fun createPaymentTransaction(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body payment: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/payment_transactions")
    suspend fun getPaymentTransactionsByPhone(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("phone_number") phoneFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 5,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/payment_transactions")
    suspend fun getPaymentTransactionsByUserId(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 5,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/subscriptions")
    suspend fun getSubscriptionForUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 10,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/subscriptions")
    suspend fun getSubscriptionsByPhone(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("sender_phone") phoneFilter: String,
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 10,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @PATCH("rest/v1/subscriptions")
    suspend fun updateSubscription(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any>
    ): Response<Unit>

    // Bajaj Rides (Customer Ride & Driver Bids sync)
    @POST("rest/v1/bajaj_rides")
    suspend fun createRide(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body ride: Map<String, Any>
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/bajaj_rides")
    suspend fun getPendingRides(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("status") status: String = "eq.pending",
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/bajaj_rides")
    suspend fun getRideById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>

    @PATCH("rest/v1/bajaj_rides")
    suspend fun updateRideStatus(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, Any>
    ): Response<Unit>

    // Lost phone / device reporting sync
    @GET("rest/v1/devices")
    suspend fun getLostDevices(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("is_lost") isLost: String = "eq.true",
        @Query("order") order: String = "updated_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<Map<String, Any>>>
}

object NetworkClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = if (com.example.BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun createService(baseUrl: String): SupabaseApiService {
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApiService::class.java)
    }
}
