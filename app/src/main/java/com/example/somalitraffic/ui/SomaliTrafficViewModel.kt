package com.example.somalitraffic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.somalitraffic.data.location.DriverLocationTracker
import com.example.somalitraffic.data.model.*
import com.example.somalitraffic.data.remote.OsmRoutingService
import com.example.somalitraffic.data.remote.SupabaseRealtimeClient
import com.example.somalitraffic.data.repository.SearchResultItem
import com.example.somalitraffic.data.repository.SomaliTrafficRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SomaliTrafficViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SomaliTrafficRepository(application)

    // User session
    val currentUser: StateFlow<UserProfile?> = repository.prefs.currentUserFlow

    val isDriver: StateFlow<Boolean> = currentUser.map { it?.role == UserRole.DRIVER }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSupabaseConfigured: Boolean
        get() = repository.prefs.isSupabaseConfigured()

    // Intersections
    val intersections: StateFlow<List<Intersection>> = repository.getIntersections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Incidents / Alerts
    val incidents: StateFlow<List<TrafficIncident>> = repository.getActiveIncidents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Devices & Phone Guard
    val registeredDevice: StateFlow<Device?> = repository.getRegisteredDevice()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userDevices: StateFlow<List<Device>> = repository.getUserDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDevices: StateFlow<List<Device>> = repository.getAllDevices()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedDeviceId = MutableStateFlow<String?>(null)
    val selectedDeviceId: StateFlow<String?> = _selectedDeviceId.asStateFlow()

    fun selectDeviceId(id: String?) {
        _selectedDeviceId.value = id
    }

    // Subscription
    private val _manualSubscription = MutableStateFlow<Subscription?>(null)

    val subscription: StateFlow<Subscription?> = combine(
        repository.getSubscription(),
        _manualSubscription
    ) { repoSub, manualSub ->
        when {
            repository.isSubscriptionActive(manualSub) -> manualSub
            repository.isSubscriptionActive(repoSub) -> repoSub
            manualSub != null -> manualSub
            else -> repoSub
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSubscriptions: StateFlow<List<Subscription>> = repository.getAllSubscriptions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Support Tickets
    val supportTickets: StateFlow<List<SupportTicket>> = repository.getSupportTickets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit Logs
    val auditLogs: StateFlow<List<AuditLog>> = repository.getAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Feedback state
    private val _uiMessage = MutableSharedFlow<String>()
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshingSubscription = MutableStateFlow(false)
    val isRefreshingSubscription: StateFlow<Boolean> = _isRefreshingSubscription.asStateFlow()

    // --- Phone SMS OTP States ---
    private val _otpCooldown = MutableStateFlow(0)
    val otpCooldown: StateFlow<Int> = _otpCooldown.asStateFlow()

    private val _isOtpSent = MutableStateFlow(false)
    val isOtpSent: StateFlow<Boolean> = _isOtpSent.asStateFlow()

    private val _pendingPhone = MutableStateFlow("")
    val pendingPhone: StateFlow<String> = _pendingPhone.asStateFlow()

    private val _otpNotice = MutableStateFlow<String?>(null)
    val otpNotice: StateFlow<String?> = _otpNotice.asStateFlow()

    private var cooldownJob: Job? = null

    private fun startCooldown(seconds: Int = 60) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            _otpCooldown.value = seconds
            while (_otpCooldown.value > 0) {
                delay(1000)
                _otpCooldown.value -= 1
            }
        }
    }

    fun resetOtpState() {
        _isOtpSent.value = false
        _pendingPhone.value = ""
        _otpNotice.value = null
        _otpCooldown.value = 0
        cooldownJob?.cancel()
    }

    fun sendOtpForLogin(
        phone: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.sendPhoneOtp(phone = phone, isSignUp = false)
            _isLoading.value = false
            res.onSuccess { result ->
                _isOtpSent.value = true
                _pendingPhone.value = result.phoneE164
                _otpNotice.value = result.message
                startCooldown(60)
                _uiMessage.emit(result.message)
                onSuccess()
            }.onFailure { e ->
                val errorMsg = e.localizedMessage ?: "Dirista SMS-ka way fashilantay"
                _uiMessage.emit(errorMsg)
                onError(errorMsg)
            }
        }
    }

    fun verifyOtpAndLogin(
        phone: String,
        code: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyPhoneOtpAndSignIn(phone, code)
            _isLoading.value = false
            res.onSuccess { user ->
                resetOtpState()
                onSuccess(user)
            }.onFailure { e ->
                val errorMsg = e.localizedMessage ?: "Koodhka OTP waa khalad ama wuu dhacay"
                _uiMessage.emit(errorMsg)
                onError(errorMsg)
            }
        }
    }

    fun verifyAccountForReset(
        phone: String,
        fullName: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyAccountForPasswordReset(phone, fullName)
            _isLoading.value = false
            res.onSuccess { user ->
                onSuccess(user)
            }.onFailure { e ->
                val msg = e.localizedMessage ?: "Hubinta akoonka way fashilantay"
                onError(msg)
            }
        }
    }

    fun resetPasswordAndLogin(
        phone: String,
        fullName: String,
        newPass: String,
        onSuccess: (UserProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.resetPasswordByPhone(phone, fullName, newPass)
            if (res.isFailure) {
                _isLoading.value = false
                val msg = res.exceptionOrNull()?.localizedMessage ?: "Beddelka furaha sirta wuu fashilantay"
                onError(msg)
                return@launch
            }
            // Auto login with new password
            val loginRes = repository.login(phone, newPass)
            _isLoading.value = false
            loginRes.onSuccess { user ->
                _uiMessage.emit("Furaha sirta si guul leh ayaa loo beddelay!")
                onSuccess(user)
            }.onFailure { e ->
                // Password was successfully reset, user can log in manually
                onError("Furaha sirta waa la beddelay! Fadlan ku gal furahaaga cusub.")
            }
        }
    }

    fun sendOtpForSignUp(phone: String, fullName: String, role: String? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.sendPhoneOtp(phone = phone, isSignUp = true, fullName = fullName, role = role)
            _isLoading.value = false
            res.onSuccess { result ->
                _isOtpSent.value = true
                _pendingPhone.value = result.phoneE164
                _otpNotice.value = result.message
                startCooldown(60)
                _uiMessage.emit(result.message)
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Dirista SMS-ka way fashilantay")
            }
        }
    }

    fun verifyOtpAndSignUp(
        fullName: String,
        phone: String,
        code: String,
        isDriver: Boolean,
        plateNumber: String? = null,
        vehicleModel: String? = null,
        onSuccess: (UserProfile) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyPhoneOtpAndRegister(
                fullName = fullName,
                phone = phone,
                otpCode = code,
                isDriver = isDriver,
                plateNumber = plateNumber,
                vehicleModel = vehicleModel
            )
            _isLoading.value = false
            res.onSuccess { user ->
                resetOtpState()
                onSuccess(user)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Diiwaangelinta way fashilantay")
            }
        }
    }

    fun resendOtp(phone: String) {
        if (_otpCooldown.value > 0) {
            viewModelScope.launch {
                _uiMessage.emit("Fadlan sug ${_otpCooldown.value} ilbiriqsi ka hor inta aadan koodh cusub codsan.")
            }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.resendPhoneOtp(phone)
            _isLoading.value = false
            res.onSuccess { result ->
                _otpNotice.value = result.message
                startCooldown(60)
                _uiMessage.emit(result.message)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Dib u dirista way fashilantay")
            }
        }
    }

    // --- Search & GPS States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    data class DriverGpsData(
        val latitude: Double,
        val longitude: Double,
        val accuracy: Float = 0f,
        val bearing: Float = 0f,
        val speedKmh: Double = 0.0,
        val timestamp: Long = System.currentTimeMillis()
    )

    private val _driverGps = MutableStateFlow<DriverGpsData?>(null)
    val driverGps: StateFlow<DriverGpsData?> = _driverGps.asStateFlow()

    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation.asStateFlow()

    private val _selectedIntersection = MutableStateFlow<Intersection?>(null)
    val selectedIntersection: StateFlow<Intersection?> = _selectedIntersection.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Real-time dynamic search results based on query, intersections, and GPS
    val searchResults: StateFlow<List<SearchResultItem>> = combine(
        _searchQuery,
        intersections,
        _userLocation
    ) { query, allIntersections, location ->
        _isSearching.value = true
        val results = repository.searchIntersections(
            query = query,
            allIntersections = allIntersections,
            userLat = location?.first,
            userLng = location?.second
        )
        _isSearching.value = false
        results
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedIntersectionsIfEmpty()
            _isLoading.value = true
            val result = repository.syncIntersectionsFromSupabase()
            _isLoading.value = false
            result.onFailure {
                _searchError.value = "Xogta lama soo dejin karin. Hubi xiriirka internetka."
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _searchError.value = null
    }

    fun setUserLocation(lat: Double, lng: Double) {
        _userLocation.value = Pair(lat, lng)
    }

    fun selectIntersection(intersection: Intersection?) {
        _selectedIntersection.value = intersection
    }

    fun selectIntersectionById(id: String) {
        viewModelScope.launch {
            val item = repository.getIntersectionById(id)
            _selectedIntersection.value = item
        }
    }

    fun refreshTrafficData() {
        viewModelScope.launch {
            _isLoading.value = true
            _searchError.value = null
            val result = repository.syncIntersectionsFromSupabase()
            _isLoading.value = false
            result.onFailure {
                // In low connectivity, local Room data continues seamlessly
                _searchError.value = "Xogta lama soo dejin karin. Hubi xiriirka internetka."
            }
        }
    }

    /**
     * Validates any persisted session at startup. Returns true only when the
     * stored user has a real, usable Supabase token (or was refreshed). A stale
     * or backup-restored session is cleared, so the user is treated as logged
     * out instead of being routed into the Driver Pass screen.
     */
    suspend fun validateStoredSession(): Boolean = repository.validateStoredSession()

    fun login(identifier: String, pass: String, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.login(identifier, pass)
            _isLoading.value = false
            res.onSuccess { user ->
                onSuccess(user)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Khalad baa dhacay")
            }
        }
    }

    fun loginWithGoogle(onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.loginWithGoogle()
            _isLoading.value = false
            res.onSuccess { user ->
                onSuccess(user)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Google Sign-In failed")
            }
        }
    }

    fun register(
        name: String,
        phone: String,
        pass: String,
        isDriver: Boolean = true,
        plateNumber: String? = null,
        vehicleModel: String? = null,
        referralCode: String? = null,
        onSuccess: (UserProfile) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.registerUser(name, phone, pass, isDriver, plateNumber, vehicleModel, referralCode)
            _isLoading.value = false
            res.onSuccess { user ->
                onSuccess(user)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Diiwaangelintu way fashilantay")
            }
        }
    }

    fun isPlateRegistered(plate: String): Boolean = repository.isPlateRegistered(plate)
    fun isPhoneRegistered(phone: String): Boolean = repository.isPhoneRegistered(phone)
    fun isSubscriptionActive(sub: Subscription? = subscription.value): Boolean = repository.isSubscriptionActive(sub)
    fun isSubscription24hExpired(sub: Subscription? = subscription.value): Boolean = repository.isSubscription24hExpired(sub)

    fun payDriverPass(
        senderPhone: String,
        destinationPhone: String,
        transactionId: String = "",
        paymentMethod: String = "EVC Plus",
        onSuccess: () -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.processDriverPassPayment(
                senderPhone,
                destinationPhone,
                transactionId,
                paymentMethod
            )
            _isLoading.value = false
            res.onSuccess {
                val message = "Lacag-bixintaada waa loo diray Admin-ka. Fadlan sug ansixinta."
                _uiMessage.emit(message)
                onSuccess()
            }.onFailure { e ->
                val message = e.localizedMessage
                    ?.takeIf { it.isNotBlank() }
                    ?: "Lacag-bixinta way fashilantay. Fadlan isku day mar kale."
                _uiMessage.emit(message)
                onError(message)
            }
        }
    }

    fun syncCurrentSubscription(phone: String? = null, onFinished: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshingSubscription.value = true
            val res = repository.syncCurrentSubscription(phone)
            _isRefreshingSubscription.value = false
            res.onSuccess { sub ->
                if (sub != null) {
                    _manualSubscription.value = sub
                    val isActive = repository.isSubscriptionActive(sub)
                    if (isActive) {
                        onFinished?.invoke(true, "Hambalyo! Adeeggaaga waa la ansixiyay (Active)!")
                    } else if (sub.status.lowercase() in listOf("rejected", "failed")) {
                        onFinished?.invoke(false, "Bixintan waa la diaday (Rejected).")
                    } else {
                        onFinished?.invoke(false, "Weli waxaa la sugayaa ansixinta Admin-ka (Pending).")
                    }
                } else {
                    onFinished?.invoke(false, "Ma jiro Driver Pass la helay.")
                }
            }.onFailure { err ->
                onFinished?.invoke(false, "Cilad baa dhacday: ${err.localizedMessage ?: "Isku xirka shabakadda hubi"}")
            }
        }
    }

    fun updateSubscriptionStatus(subscriptionId: String, newStatus: String, rejectionReason: String? = null) {
        viewModelScope.launch {
            repository.updateSubscriptionStatus(subscriptionId, newStatus, rejectionReason)
        }
    }

    fun submitTrafficReport(
        intersectionId: String,
        status: String,
        cause: String?,
        notes: String?,
        lat: Double,
        lng: Double,
        accuracy: Double,
        onError: (String) -> Unit = {},
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.submitTrafficReport(intersectionId, status, cause, notes, lat, lng, accuracy)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Warbixinta saxmadda si guul leh ayaa loo gudbiyay!")
                onSuccess()
            }.onFailure { e ->
                val err = e.localizedMessage ?: "Warbixinta lama gudbin karin"
                _uiMessage.emit(err)
                onError(err)
            }
        }
    }

    fun registerIntersection(
        name: String,
        lat: Double,
        lng: Double,
        radius: Int = 400,
        city: String = "Muqdisho",
        district: String = "Hodan",
        onSuccess: (Intersection) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.registerIntersection(name, lat, lng, radius, city, district)
            _isLoading.value = false
            res.onSuccess { item ->
                _uiMessage.emit("Isgoyska ${item.name} si guul leh ayaa loo diiwaangeliyay!")
                onSuccess(item)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Diiwaangelinta isgoysku way fashilantay")
            }
        }
    }

    fun registerDevice(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.registerCurrentDevice()
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Taleefankaaga si nabadgelyo leh ayaa loo diiwaangeliyay!")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Diiwaangelinta taleefanku way fashilantay")
            }
        }
    }

    fun registerCustomDevice(deviceName: String, deviceModel: String, platform: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.registerCustomDevice(deviceName, deviceModel, platform)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Aaladda cusub ($deviceName) si guul leh ayaa loogu daray Phone Guard!")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Ku darista aaladda way fashilantay")
            }
        }
    }

    fun setProtectionEnabled(deviceId: String, enabled: Boolean, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.setProtectionEnabled(deviceId, enabled)
            res.onSuccess {
                _uiMessage.emit(if (enabled) "Ilaalinta taleefanka waa la furay (Active)" else "Ilaalinta taleefanka waa la damiyay")
                onComplete()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Lama beddeli karo xaaladda ilaalinta")
            }
        }
    }

    fun markDeviceLost(deviceId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.markDeviceAsLost(deviceId)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Taleefanka waxaa loo calaamadeeyay inuu lumay! Raadinta tooska ah waa la bilaabay.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Lama beddeli karo xaaladda taleefanka")
            }
        }
    }

    fun markDeviceFound(deviceId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.markDeviceAsFound(deviceId)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Alxamdulillaah! Taleefanka waxaa loo calaamadeeyay in la helay.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Lama xaqiijin karo xaaladda")
            }
        }
    }

    fun updateUserLocationDirect(context: android.content.Context) {
        fetchFreshUserLocation(context)
    }

    fun fetchFreshUserLocation(
        context: android.content.Context,
        onSuccess: ((Pair<Double, Double>) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        try {
            val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasFine && !hasCoarse) {
                onError?.invoke("Fadlan ogolow fasaxa goobta (Location Permission)")
                return
            }

            val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            val cts = com.google.android.gms.tasks.CancellationTokenSource()

            fused.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { loc ->
                if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                    val pair = Pair(loc.latitude, loc.longitude)
                    _userLocation.value = pair
                    viewModelScope.launch {
                        try {
                            repository.updateDeviceLocation(loc.latitude, loc.longitude, loc.accuracy.toDouble())
                        } catch (_: Exception) {}
                    }
                    onSuccess?.invoke(pair)
                } else {
                    fused.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null && lastLoc.latitude != 0.0 && lastLoc.longitude != 0.0) {
                            val pair = Pair(lastLoc.latitude, lastLoc.longitude)
                            _userLocation.value = pair
                            onSuccess?.invoke(pair)
                        } else {
                            onError?.invoke("Lama heli karo goobtaada GPS-ka")
                        }
                    }.addOnFailureListener {
                        onError?.invoke("Khalad GPS")
                    }
                }
            }.addOnFailureListener {
                fused.lastLocation.addOnSuccessListener { lastLoc ->
                    if (lastLoc != null && lastLoc.latitude != 0.0 && lastLoc.longitude != 0.0) {
                        val pair = Pair(lastLoc.latitude, lastLoc.longitude)
                        _userLocation.value = pair
                        onSuccess?.invoke(pair)
                    } else {
                        onError?.invoke("Lama heli karo goobtaada GPS-ka")
                    }
                }.addOnFailureListener {
                    onError?.invoke("Khalad GPS")
                }
            }
        } catch (e: Exception) {
            onError?.invoke(e.localizedMessage ?: "Khalad GPS")
        }
    }

    fun findNow(deviceId: String, onSuccess: (Device) -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val lat = _userLocation.value?.first
            val lng = _userLocation.value?.second
            val res = repository.findNow(deviceId, lat, lng)
            _isLoading.value = false
            res.onSuccess { updatedDev ->
                _uiMessage.emit("Goobta tooska ah ee taleefanka si guul ah ayaa loo soo helay!")
                onSuccess(updatedDev)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Lama heli karo goobta tooska ah hadda")
            }
        }
    }

    fun deleteDevice(deviceId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.deleteDevice(deviceId)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Aaladda waa laga saaray liiska.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Tirista aaladda way fashilantay")
            }
        }
    }

    fun getDeviceLocations(deviceId: String): Flow<List<DeviceLocation>> {
        return repository.getDeviceLocations(deviceId)
    }

    fun getDeviceRecoveryEvents(deviceId: String): Flow<List<DeviceRecoveryEvent>> {
        return repository.getDeviceRecoveryEvents(deviceId)
    }

    fun createTicket(category: String, subject: String, message: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createSupportTicket(category, subject, message)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Cabashadaada/su'aashaada si guul leh ayaa loo diiwaangeliyay!")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Lama diri karin")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            repository.logout()
            _isLoading.value = false
            resetOtpState()
            onSuccess()
        }
    }

    // ==================== GoBajaaj: live ride state ====================
    private val _liveRide = MutableStateFlow<RideEntity?>(null)
    val liveRide: StateFlow<RideEntity?> = _liveRide.asStateFlow()

    private val _routeInfo = MutableStateFlow<RouteInfo?>(null)
    val routeInfo: StateFlow<RouteInfo?> = _routeInfo.asStateFlow()

    private val _rideConnected = MutableStateFlow(false)
    val rideConnected: StateFlow<Boolean> = _rideConnected.asStateFlow()

    private var rideObserveJob: Job? = null
    private var realtimeSub: SupabaseRealtimeClient.Subscription? = null
    private var lastRouteFetchedAt = 0L

    private val realtimeClient: SupabaseRealtimeClient by lazy {
        SupabaseRealtimeClient(repository.prefs.getSupabaseUrl(), repository.prefs.getSupabaseKey())
    }

    private fun distanceMeters(aLat: Double?, aLng: Double?, bLat: Double?, bLng: Double?): Double? {
        if (aLat == null || aLng == null || bLat == null || bLng == null) return null
        return OsmRoutingService.haversineMeters(aLat, aLng, bLat, bLng)
    }

    fun createRideRequest(
        pickup: String,
        destination: String,
        vehicleType: String,
        offeredPrice: Double,
        pickupLat: Double? = null,
        pickupLng: Double? = null,
        destLat: Double? = null,
        destLng: Double? = null,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createRideRequest(
                pickup, destination, vehicleType, offeredPrice,
                pickupLat, pickupLng, destLat, destLng
            )
            _isLoading.value = false
            res.onSuccess { rideId ->
                _uiMessage.emit("Dalabkaaga gaadiidka waa la diray! Darawallada ayaa loo gudbiyay.")
                observeRide(rideId)
                // kick off auto-matching right away, then again with wider radius
                viewModelScope.launch {
                    delay(1_500)
                    runRideMatching(rideId, 5_000)
                    delay(10_000)
                    runRideMatching(rideId, 8_000)
                    delay(15_000)
                    runRideMatching(rideId, 15_000)
                }
                onSuccess(rideId)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Dalabka lama diri karin")
            }
        }
    }

    /** Streaming (Realtime) + polling fallback for one ride. */
    fun observeRide(rideId: String) {
        rideObserveJob?.cancel()
        realtimeSub?.close()
        repository.prefs.setActiveRideId(rideId)
        _rideConnected.value = false

        try {
            realtimeSub = realtimeClient.subscribeToRide(
                rideId = rideId,
                token = repository.realtimeAccessToken(),
                listener = object : SupabaseRealtimeClient.RideListener {
                    override fun onRideUpdate(record: Map<String, Any?>) {
                        val mapped = repository.mapRideRow(record, rideId)
                        if (_liveRide.value?.id == rideId) {
                            _liveRide.value = mergeRideRecord(_liveRide.value!!, record, rideId)
                        } else {
                            _liveRide.value = mapped
                        }
                    }

                    override fun onConnectionChanged(connected: Boolean) {
                        _rideConnected.value = connected
                    }
                }
            )
        } catch (e: Exception) {
            _rideConnected.value = false
        }

        // Polling fallback: keeps ETA/status fresh even if realtime is blocked
        rideObserveJob = viewModelScope.launch {
            while (isActive) {
                delay(6_000)
                val ride = repository.getRideById(rideId).getOrNull() ?: continue
                _liveRide.value = ride
                refreshRouteIfNeeded(ride)
                if (ride.isTerminal) {
                    repository.prefs.setActiveRideId(null)
                    repository.prefs.setRideWatchStatus(ride.status)
                    break
                }
            }
        }
    }

    fun stopObservingRide() {
        rideObserveJob?.cancel()
        rideObserveJob = null
        realtimeSub?.close()
        realtimeSub = null
        _liveRide.value = null
        _routeInfo.value = null
        _rideConnected.value = false
        lastRouteFetchedAt = 0L
        repository.prefs.setActiveRideId(null)
    }

    private fun mergeRideRecord(base: RideEntity, record: Map<String, Any?>, rideId: String): RideEntity {
        // Full row from realtime; rebuild entity then keep local-only fields
        val full = repository.mapRideRow(record, rideId)
        return base.copy(
            status = full.status.takeIf { it.isNotBlank() } ?: base.status,
            driverLat = full.driverLat ?: if (record.containsKey("driver_lat")) null else base.driverLat,
            driverLng = full.driverLng ?: if (record.containsKey("driver_lng")) null else base.driverLng,
            driverHeading = full.driverHeading ?: base.driverHeading,
            driverSpeed = full.driverSpeed ?: base.driverSpeed,
            driverLocationAt = full.driverLocationAt ?: base.driverLocationAt,
            assignedDriverName = full.assignedDriverName ?: base.assignedDriverName,
            assignedDriverPlate = full.assignedDriverPlate ?: base.assignedDriverPlate,
            assignedDriverPhone = full.assignedDriverPhone ?: base.assignedDriverPhone,
            assignedDriverId = full.assignedDriverId ?: base.assignedDriverId,
            agreedFare = full.agreedFare ?: base.agreedFare,
            updatedAt = full.updatedAt.ifBlank { base.updatedAt }
        )
    }

    /** OSRM route for the current phase (driver->pickup, or pickup->dest). */
    private suspend fun refreshRouteIfNeeded(ride: RideEntity) {
        val status = ride.status.lowercase()
        if (status in listOf("completed", "cancelled")) return

        val (fromLat, fromLng, toLat, toLng) = when (status) {
            "in_progress" -> listOf(ride.pickupLat, ride.pickupLng, ride.destLat, ride.destLng)
            else -> listOf(ride.driverLat, ride.driverLng, ride.pickupLat, ride.pickupLng)
        }
        if (fromLat == null || fromLng == null || toLat == null || toLng == null) return

        val current = _routeInfo.value
        val sameEnds = current != null &&
            Math.abs(current.toLat - toLat) < 0.0005 && Math.abs(current.toLng - toLng) < 0.0005 &&
            current.fromLat == ride.driverLat && current.fromLng == ride.driverLng
        if (sameEnds && System.currentTimeMillis() - lastRouteFetchedAt < 30_000) return

        requestRoute(fromLat, fromLng, toLat, toLng)
    }

    fun requestRoute(fromLat: Double, fromLng: Double, toLat: Double, toLng: Double) {
        viewModelScope.launch {
            val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                OsmRoutingService.fetchRoute(fromLat, fromLng, toLat, toLng)
                    ?: OsmRoutingService.estimateStraightLine(fromLat, fromLng, toLat, toLng)
            }
            lastRouteFetchedAt = System.currentTimeMillis()
            _routeInfo.value = RouteInfo(
                distanceMeters = result.distanceMeters,
                durationSeconds = result.durationSeconds,
                pointsJson = result.pointsJson(),
                fromLat = fromLat,
                fromLng = fromLng,
                toLat = toLat,
                toLng = toLng,
                steps = result.steps
            )
        }
    }

    fun triggerReroute(driverLat: Double, driverLng: Double, toLat: Double, toLng: Double) {
        val now = System.currentTimeMillis()
        if (now - lastRouteFetchedAt >= 4_000) {
            requestRoute(driverLat, driverLng, toLat, toLng)
        }
    }

    fun clearRoute() {
        _routeInfo.value = null
        lastRouteFetchedAt = 0L
    }

    // ==================== Driver side: matching + GPS ====================

    fun runRideMatching(rideId: String, radiusMeters: Int = 5000, onResult: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val matched = repository.matchDriversForRide(rideId, radiusMeters).getOrDefault(0)
            if (matched > 0) {
                val ride = repository.getRideById(rideId).getOrNull()
                if (ride != null) _liveRide.value = ride
            }
            onResult(matched)
        }
    }

    fun declineRide(rideId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.declineRide(rideId)
            if (_liveRide.value?.id == rideId) _liveRide.value = null
            onDone()
        }
    }

    fun fetchAssignedRides(onResult: (List<RideEntity>) -> Unit = {}) {
        viewModelScope.launch {
            val list = repository.fetchRidesForDriver("in.(assigned)").getOrDefault(emptyList())
            onResult(list)
        }
    }

    fun startTrip(rideId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.startTrip(rideId)
            _isLoading.value = false
            res.onSuccess {
                startDriverRideTracking(rideId)
                _uiMessage.emit("Safarka wuu bilaabmay! Waddo gal.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Safarka lama bilaabi karin")
            }
        }
    }

    // --- Driver GPS streaming (presence + ride live location) ---
    private var tracker: DriverLocationTracker? = null
    private var presenceJob: Job? = null
    private var trackingRideId: String? = null
    private var lastUploadAt = 0L
    private var lastUploadLat = Double.NaN
    private var lastUploadLng = Double.NaN

    fun startDriverRideTracking(rideId: String) {
        trackingRideId = rideId
        if (tracker == null) tracker = DriverLocationTracker(getApplication())
        tracker?.start { loc ->
            val now = System.currentTimeMillis()
            val heading = if (loc.hasBearing()) loc.bearing else 0f
            val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6) else 0.0

            // Continuous local UI update for MapLibre & smooth tracking
            _userLocation.value = Pair(loc.latitude, loc.longitude)
            _driverGps.value = DriverGpsData(
                latitude = loc.latitude,
                longitude = loc.longitude,
                accuracy = loc.accuracy,
                bearing = heading,
                speedKmh = speedKmh,
                timestamp = loc.time
            )

            // Check if automatic rerouting is needed
            val curRoute = _routeInfo.value
            if (curRoute != null) {
                val pts = OsmRoutingService.parsePointsJson(curRoute.pointsJson)
                if (pts.isNotEmpty()) {
                    val offDist = OsmRoutingService.distanceToPolylineMeters(loc.latitude, loc.longitude, pts)
                    if (offDist > 65.0 && now - lastRouteFetchedAt >= 8_000) {
                        requestRoute(loc.latitude, loc.longitude, curRoute.toLat, curRoute.toLng)
                    }
                }
            }

            val moved = if (lastUploadLat.isNaN()) Double.MAX_VALUE else
                OsmRoutingService.haversineMeters(lastUploadLat, lastUploadLng, loc.latitude, loc.longitude)
            val due = (now - lastUploadAt >= 3_000 && moved >= 8.0) || (now - lastUploadAt >= 12_000)
            if (due && trackingRideId != null) {
                lastUploadAt = now
                lastUploadLat = loc.latitude
                lastUploadLng = loc.longitude
                viewModelScope.launch {
                    repository.updateDriverLiveLocation(
                        rideId, loc.latitude, loc.longitude, heading.toDouble(), speedKmh
                    )
                }
            }
        }
        setDriverOnline(true)
    }

    fun stopDriverRideTracking(goOffline: Boolean = false) {
        tracker?.stop()
        trackingRideId = null
        lastUploadAt = 0L
        if (goOffline) setDriverOnline(false)
    }

    /** Driver home screen online switch -> matching pool membership. */
    fun setDriverOnline(online: Boolean) {
        presenceJob?.cancel()
        presenceJob = null
        if (!online) {
            viewModelScope.launch { repository.upsertDriverPresence(false) }
            return
        }
        presenceJob = viewModelScope.launch {
            var lastSentAt = 0L
            var lastLat = Double.NaN
            var lastLng = Double.NaN
            while (isActive) {
                val loc = _userLocation.value
                val now = System.currentTimeMillis()
                val moved = if (loc == null || lastLat.isNaN()) Double.MAX_VALUE else
                    OsmRoutingService.haversineMeters(lastLat, lastLng, loc.first, loc.second)
                if (now - lastSentAt >= 30_000 || moved >= 30.0) {
                    lastSentAt = now
                    if (loc != null) {
                        lastLat = loc.first
                        lastLng = loc.second
                        repository.upsertDriverPresence(true, loc.first, loc.second)
                    } else {
                        repository.upsertDriverPresence(true)
                    }
                }
                delay(30_000)
            }
        }
    }

    fun getRideById(rideId: String, onResult: (RideEntity?) -> Unit) {
        viewModelScope.launch {
            val res = repository.getRideById(rideId)
            onResult(res.getOrNull())
        }
    }

    fun fetchPendingRides(onResult: (List<RideEntity>) -> Unit = {}) {
        viewModelScope.launch {
            val res = repository.fetchPendingRideEntities()
            onResult(res.getOrDefault(emptyList()))
        }
    }

    fun acceptRide(rideId: String, agreedFare: Double? = null, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.acceptRideByDriver(rideId, agreedFare)
            _isLoading.value = false
            res.onSuccess {
                startDriverRideTracking(rideId)
                _uiMessage.emit("Waad aqbashay safarka! Macmiilka la xiriir.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Safarka lama aqbali karin")
            }
        }
    }

    fun arriveAtPickup(rideId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.arriveAtPickup(rideId)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Waxaad soo gaartay goobta macmiilka! Codka iyo dhawaaqa imaanshaha waa la diray.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Imaanshaha lama diiwaangelin karin")
            }
        }
    }

    fun completeRide(rideId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.completeRide(rideId)
            _isLoading.value = false
            res.onSuccess {
                if (trackingRideId == rideId) stopDriverRideTracking()
                if (_liveRide.value?.id == rideId) stopObservingRide()
                _uiMessage.emit("Safarkii wuu dhammaaday! Lacagta toos uga qaado rakaabka.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Safarka lama xiri karin")
            }
        }
    }

    fun cancelRide(rideId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.cancelRide(rideId)
            _isLoading.value = false
            res.onSuccess {
                if (trackingRideId == rideId) stopDriverRideTracking()
                if (_liveRide.value?.id == rideId) stopObservingRide()
                _uiMessage.emit("Dalabkii safarka waa la joojiyay.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Dalabka lama joojin karin")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        rideObserveJob?.cancel()
        realtimeSub?.close()
        tracker?.stop()
        presenceJob?.cancel()
    }

    val activeRide: Flow<RideEntity?> = repository.getActiveRideFlow()
    val pendingRides: Flow<List<RideEntity>> = repository.getPendingRidesFlow()

    fun updateSubscriptionStatus(subscriptionId: String, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.updateSubscriptionStatus(subscriptionId, newStatus)
            _isLoading.value = false
            res.onSuccess {
                _uiMessage.emit("Subscription updated to $newStatus")
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Failed to update subscription")
            }
        }
    }
}
