package com.example.somalitraffic.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.somalitraffic.data.model.*
import com.example.somalitraffic.data.repository.SearchResultItem
import com.example.somalitraffic.data.repository.SomaliTrafficRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class SomaliTrafficViewModel(application: Application) : AndroidViewModel(application) {
    val repository = SomaliTrafficRepository(application)

    // User session
    val currentUser: StateFlow<UserProfile?> = repository.prefs.currentUserFlow

    val isDriver: StateFlow<Boolean> = currentUser.map { it?.role == UserRole.DRIVER }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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
            manualSub?.status?.lowercase() in listOf("active", "completed", "approved") -> manualSub
            repoSub?.status?.lowercase() in listOf("active", "completed", "approved") -> repoSub
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

    fun sendOtpForLogin(phone: String, onSuccess: () -> Unit = {}) {
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
                _uiMessage.emit(e.localizedMessage ?: "Dirista SMS-ka way fashilantay")
            }
        }
    }

    fun verifyOtpAndLogin(phone: String, code: String, onSuccess: (UserProfile) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.verifyPhoneOtpAndSignIn(phone, code)
            _isLoading.value = false
            res.onSuccess { user ->
                resetOtpState()
                onSuccess(user)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Koodhka OTP waa khalad ama wuu dhacay")
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
        onSuccess: (UserProfile) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.registerUser(name, phone, pass, isDriver, plateNumber, vehicleModel)
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
                    val isActive = sub.status.lowercase() in listOf("active", "completed", "approved")
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
        try {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                val fused = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                fused.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) {
                        _userLocation.value = Pair(loc.latitude, loc.longitude)
                        viewModelScope.launch {
                            repository.updateDeviceLocation(loc.latitude, loc.longitude, loc.accuracy.toDouble())
                        }
                    }
                }
            }
        } catch (_: Exception) {}
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

    fun createRideRequest(
        pickup: String,
        destination: String,
        vehicleType: String,
        offeredPrice: Double,
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.createRideRequest(pickup, destination, vehicleType, offeredPrice)
            _isLoading.value = false
            res.onSuccess { rideId ->
                _uiMessage.emit("Dalabkaaga gaadiidka waa la diray! Darawallada ayaa loo gudbiyay.")
                onSuccess(rideId)
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Dalabka lama diri karin")
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
                _uiMessage.emit("Dalabkii safarka waa la joojiyay.")
                onSuccess()
            }.onFailure { e ->
                _uiMessage.emit(e.localizedMessage ?: "Dalabka lama joojin karin")
            }
        }
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
