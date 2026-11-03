package com.example.somalitraffic.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.horizontalScroll
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.data.model.RideEntity
import com.example.somalitraffic.data.model.RouteInfo
import com.example.somalitraffic.data.model.SavedAddress
import com.example.somalitraffic.data.model.VehicleType
import com.example.somalitraffic.data.remote.OsmRoutingService
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliBottomNavigation
import com.example.somalitraffic.ui.components.SomaliInteractiveMap
import com.example.somalitraffic.util.ArrivalSoundManager
import com.example.somalitraffic.util.NotificationHelper
import com.example.ui.theme.*
import kotlinx.coroutines.delay

private enum class RiderFlowStep {
    REQUEST_FORM,
    LOOKING_FOR_DRIVERS,
    ON_TRIP_TRACKING,
    TRIP_COMPLETED
}

private enum class DriverFlowStep {
    DRIVER_HOME,
    INCOMING_REQUEST,
    ON_TRIP_NAV
}

private data class BidDriverItem(
    val id: String,
    val name: String,
    val rating: Double,
    val reviewCount: Int,
    val vehicleModel: String,
    val plateNumber: String,
    val distanceKm: Double,
    val etaMinutes: Int,
    val offeredPrice: Double,
    val isCounterOffer: Boolean = false,
    val phone: String = "615123456"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BajajRideScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val intersections by viewModel.intersections.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val liveRide by viewModel.liveRide.collectAsState()
    val routeInfo by viewModel.routeInfo.collectAsState()
    val driverGps by viewModel.driverGps.collectAsState()
    val rideConnected by viewModel.rideConnected.collectAsState()
    val savedAddresses by viewModel.savedAddresses.collectAsState()
    val nearbyDriversCount by viewModel.nearbyDriversCount.collectAsState()

    // Mode Toggle: Rider vs Driver
    var isDriverMode by remember {
        mutableStateOf(currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER)
    }
    LaunchedEffect(currentUser?.role) {
        if (currentUser?.role != null) {
            isDriverMode = (currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER)
        }
    }

    // ----------------------------------------------------
    // RIDER STATE (Screens 4, 5, 6, 7 from Blueprint)
    // ----------------------------------------------------
    var riderStep by remember { mutableStateOf(RiderFlowStep.REQUEST_FORM) }
    var pickupLocation by remember { mutableStateOf("") }
    var destinationLocation by remember { mutableStateOf("") }
    var somaliLandmarkNote by remember { mutableStateOf("") }
    var showAddressDialog by remember { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf(VehicleType.BAJAAJ) }
    var riderOfferText by remember { mutableStateOf("1.50") }

    var selectedBidDriver by remember {
        mutableStateOf<BidDriverItem?>(null)
    }

    // Live driver bids available to rider (empty by default, populated dynamically)
    val availableBids = remember {
        mutableStateListOf<BidDriverItem>()
    }

    var tripRating by remember { mutableIntStateOf(5) }
    var isPrivacyProtected by remember { mutableStateOf(true) }
    var activeRideId by remember { mutableStateOf<String?>(null) }
    var incomingRide by remember { mutableStateOf<RideEntity?>(null) }
    var isDriverArrivedState by remember { mutableStateOf(false) }
    var arrivalSoundPlayedForRideId by remember { mutableStateOf<String?>(null) }

    // Rider high-accuracy GPS coordinates & loading state
    var riderPickupGps by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isFetchingRiderLocation by remember { mutableStateOf(false) }

    LaunchedEffect(riderPickupGps, userLocation) {
        val coords = riderPickupGps ?: userLocation ?: (2.0371 to 45.3283)
        viewModel.fetchNearbyDriversCount(coords.first, coords.second)
    }

    // ✅ GPS PERMISSION LAUNCHER: Auto-requests permission and fetches GPS after granted
    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isFetchingRiderLocation = true
            viewModel.fetchFreshUserLocation(
                context = context,
                onSuccess = { coords ->
                    riderPickupGps = coords
                    if (pickupLocation.isBlank()) pickupLocation = "Goobtayda Hadda (GPS)"
                    isFetchingRiderLocation = false
                    Toast.makeText(context, "\uD83D\uDCCD Goobtaada GPS-ka si sax ah ayaa loo helay!", Toast.LENGTH_SHORT).show()
                },
                onError = { err ->
                    isFetchingRiderLocation = false
                    Toast.makeText(context, "\uD83D\uDCCD GPS: $err", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Toast.makeText(context, "GPS-ka fasaxa la'aanteed ma shaqayn karo. Fadlan ogolow Location.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        // Initialise system notification channels once
        NotificationHelper.createChannels(context)

        // Check permission first, request if not granted, then fetch GPS
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // Permission already granted - fetch GPS directly
            isFetchingRiderLocation = true
            viewModel.fetchFreshUserLocation(
                context = context,
                onSuccess = { coords ->
                    riderPickupGps = coords
                    if (pickupLocation.isBlank()) {
                        pickupLocation = "Goobtayda Hadda (GPS)"
                    }
                    isFetchingRiderLocation = false
                },
                onError = {
                    isFetchingRiderLocation = false
                }
            )
        } else {
            // No permission yet - request it (GPS fetch happens in launcher callback above)
            gpsPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Keep riderPickupGps updated if userLocation updates
    LaunchedEffect(userLocation) {
        if (riderPickupGps == null && userLocation != null) {
            riderPickupGps = userLocation
            if (pickupLocation.isBlank()) {
                pickupLocation = "Goobtayda Hadda (GPS)"
            }
        }
    }

    // Real-time Rider Polling for Driver Acceptance & Completion & Arrival Sound
    LaunchedEffect(activeRideId, riderStep) {
        val rId = activeRideId ?: return@LaunchedEffect
        while (activeRideId != null && (riderStep == RiderFlowStep.LOOKING_FOR_DRIVERS || riderStep == RiderFlowStep.ON_TRIP_TRACKING)) {
            viewModel.getRideById(rId) { ride ->
                if (ride != null) {
                    if (riderStep == RiderFlowStep.LOOKING_FOR_DRIVERS && (ride.status == "accepted" || ride.status == "in_progress" || ride.status == "arrived")) {
                        selectedBidDriver = BidDriverItem(
                            id = ride.assignedDriverId ?: "drv-active",
                            name = ride.assignedDriverName ?: "Darawal Bajaaj",
                            rating = ride.assignedDriverRating,
                            reviewCount = 18,
                            vehicleModel = "Bajaj TVS",
                            plateNumber = ride.assignedDriverPlate ?: "TR-" + (ride.assignedDriverId?.take(4) ?: "1001"),
                            distanceKm = 0.5,
                            etaMinutes = ride.etaMinutes,
                            offeredPrice = ride.agreedFare ?: ride.riderOffer,
                            phone = ride.assignedDriverPhone ?: "615000000"
                        )
                        // Show notification that driver accepted (visible even if app is in background)
                        NotificationHelper.showRideAcceptedNotification(
                            context = context,
                            driverName = ride.assignedDriverName ?: "Bajaajlaha",
                            plate = ride.assignedDriverPlate ?: "TR-" + (ride.assignedDriverId?.take(4) ?: "1001"),
                            etaMinutes = ride.etaMinutes
                        )
                        riderStep = RiderFlowStep.ON_TRIP_TRACKING
                    } else if (riderStep == RiderFlowStep.LOOKING_FOR_DRIVERS && ride.status == "assigned" && selectedBidDriver == null) {
                        // Auto-matched driver found; waiting for the driver to accept
                        selectedBidDriver = BidDriverItem(
                            id = ride.assignedDriverId ?: "drv-assigned",
                            name = ride.assignedDriverName ?: "Darawal Bajaaj",
                            rating = ride.assignedDriverRating,
                            reviewCount = 18,
                            vehicleModel = "Bajaj TVS",
                            plateNumber = ride.assignedDriverPlate ?: "TR-" + (ride.assignedDriverId?.take(4) ?: "1001"),
                            distanceKm = 0.5,
                            etaMinutes = ride.etaMinutes,
                            offeredPrice = ride.agreedFare ?: ride.riderOffer,
                            phone = ride.assignedDriverPhone ?: "615000000"
                        )
                    }
                    
                    if (riderStep == RiderFlowStep.ON_TRIP_TRACKING) {
                        if (ride.status == "arrived") {
                            isDriverArrivedState = true
                            if (arrivalSoundPlayedForRideId != rId) {
                                arrivalSoundPlayedForRideId = rId
                                // Play horn sound (no TTS, no music)
                                ArrivalSoundManager.playBajajHornSequence(context)
                                // Show system notification (works even when app is in background)
                                NotificationHelper.showDriverArrivedNotification(
                                    context = context,
                                    driverName = ride.assignedDriverName ?: "Bajaajlaha",
                                    pickup = pickupLocation.ifBlank { ride.pickupAddress }
                                )
                                Toast.makeText(context, "📯 Bajaajlihii wuu soo gaaray gurigaaga!", Toast.LENGTH_LONG).show()
                            }
                        } else if (ride.status == "completed") {
                            riderStep = RiderFlowStep.TRIP_COMPLETED
                        }
                    }
                }
            }
            delay(2500)
        }
    }

    // ----------------------------------------------------
    // DRIVER STATE
    // ----------------------------------------------------
    var driverStep by remember { mutableStateOf(DriverFlowStep.DRIVER_HOME) }
    var isDriverOnline by remember { mutableStateOf(true) }
    var driverTodayTrips by remember { mutableIntStateOf(0) }
    var driverTodayEarnings by remember { mutableDoubleStateOf(0.0) }
    var driverRating by remember { mutableDoubleStateOf(5.0) }
    var counterOfferText by remember { mutableStateOf("2.00") }

    // ✅ AUTO-REFRESH: Xaaladaha Wadooyinka cusboonsii 45-sn kasta marka Darawalku safar ku jiro
    LaunchedEffect(driverStep) {
        while (driverStep == DriverFlowStep.ON_TRIP_NAV) {
            kotlinx.coroutines.delay(45_000L)
            viewModel.refreshTrafficData()
        }
    }

    // Real-time Driver Polling for incoming pending rides
    LaunchedEffect(isDriverOnline, driverStep) {
        while (isDriverOnline && driverStep == DriverFlowStep.DRIVER_HOME) {
            // 1) Rides auto-matched specifically to this driver
            viewModel.fetchAssignedRides { assigned ->
                if (assigned.isNotEmpty()) {
                    incomingRide = assigned.first()
                    driverStep = DriverFlowStep.INCOMING_REQUEST
                    NotificationHelper.showNewRideRequestNotification(
                        context = context,
                        pickup = assigned.first().pickupAddress,
                        destination = assigned.first().destinationAddress,
                        offer = "$${String.format(java.util.Locale.US, "%.2f", assigned.first().riderOffer)}"
                    )
                    ArrivalSoundManager.playSingleHornBeep(context)
                } else if (driverStep == DriverFlowStep.DRIVER_HOME) {
                    // 2) Open pool rides
                    viewModel.fetchPendingRides { list ->
                        if (list.isNotEmpty()) {
                            val ride = list.first()
                            incomingRide = ride
                            driverStep = DriverFlowStep.INCOMING_REQUEST
                            NotificationHelper.showNewRideRequestNotification(
                                context = context,
                                pickup = ride.pickupAddress,
                                destination = ride.destinationAddress,
                                offer = "$${String.format(java.util.Locale.US, "%.2f", ride.riderOffer)}"
                            )
                            ArrivalSoundManager.playSingleHornBeep(context)
                        }
                    }
                }
            }
            delay(3500)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_gobajaaj_logo),
                                contentDescription = "GoBajaaj Logo",
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GoBajaaj",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoBajaajYellow
                                )
                                Text(
                                    text = if (isDriverMode) "App-ka Darawalka • Shaqee oo Dakhli Hel" else "Gaadiid • Ammaan & Qiimo Jaban",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Seamless Mode Switcher (Rider <-> Driver)
                            FilterChip(
                                selected = isDriverMode,
                                onClick = {
                                    isDriverMode = !isDriverMode
                                },
                                label = {
                                    Text(
                                        text = if (isDriverMode) "Darawal" else "Rakaab",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    if (isDriverMode) {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GoBajaajYellow,
                                    selectedLabelColor = BrandBlack,
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    labelColor = Color.White
                                ),
                                border = null
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GoBajaajNavy)
            )
        },
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "ride",
                onNavigate = onNavigate,
                isDriver = isDriverMode
            )
        },
        containerColor = GoBajaajCardBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!isDriverMode) {
                // ==========================================
                // RIDER APP EXPERIENCE (ROW 1 OF BLUEPRINT)
                // ==========================================
                when (riderStep) {
                    RiderFlowStep.REQUEST_FORM -> {
                        // Screen 4: Ride Request
                        RiderRequestFormScreen(
                            pickup = pickupLocation,
                            onPickupChange = { pickupLocation = it },
                            destination = destinationLocation,
                            onDestinationChange = { destinationLocation = it },
                            somaliNote = somaliLandmarkNote,
                            onSomaliNoteChange = { somaliLandmarkNote = it },
                            selectedVehicle = selectedVehicle,
                            onVehicleSelect = { vehicle ->
                                selectedVehicle = vehicle
                                riderOfferText = when (vehicle) {
                                    VehicleType.BAJAAJ -> "1.50"
                                    VehicleType.CAR -> "2.50"
                                    VehicleType.SUV -> "4.00"
                                }
                            },
                            offer = riderOfferText,
                            onOfferChange = { riderOfferText = it },
                            nearbyDriversCount = nearbyDriversCount,
                            savedAddresses = savedAddresses,
                            onSelectSavedAddress = { addr ->
                                pickupLocation = addr.fullFormattedAddress
                                riderPickupGps = addr.latitude to addr.longitude
                                Toast.makeText(context, "🏠 Guri Number: ${addr.digitalCode} waa la doortay!", Toast.LENGTH_SHORT).show()
                            },
                            onOpenAddressDialog = { showAddressDialog = true },
                            isFetchingGps = isFetchingRiderLocation,
                            pickupCoordinates = riderPickupGps ?: userLocation,
                            onFetchGps = {
                                isFetchingRiderLocation = true
                                viewModel.fetchFreshUserLocation(
                                    context = context,
                                    onSuccess = { coords ->
                                        riderPickupGps = coords
                                        if (pickupLocation.isBlank()) pickupLocation = "Goobtayda Hadda (GPS)"
                                        isFetchingRiderLocation = false
                                        Toast.makeText(context, "📍 Goobtaada GPS-ka si sax ah ayaa loo helay!", Toast.LENGTH_SHORT).show()
                                    },
                                    onError = { err ->
                                        isFetchingRiderLocation = false
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onRequestRide = {
                                if (pickupLocation.trim().isEmpty()) {
                                    Toast.makeText(context, "Fadlan qor goobta lagaa qaadayo (Pickup)", Toast.LENGTH_SHORT).show()
                                } else if (destinationLocation.trim().isEmpty()) {
                                    Toast.makeText(context, "Fadlan qor meesha aad u socoto (Destination)", Toast.LENGTH_SHORT).show()
                                } else {
                                    val resolvedPickup = riderPickupGps
                                        ?: userLocation
                                        ?: resolveIntersectionCoords(pickupLocation.trim(), intersections)
                                        ?: (2.0371 to 45.3283)

                                    val destCoords = resolveIntersectionCoords(destinationLocation.trim(), intersections)
                                        ?: (2.0425 to 45.3411)

                                    val finalPickup = if (somaliLandmarkNote.isNotBlank() && !pickupLocation.contains(somaliLandmarkNote)) {
                                        "$pickupLocation • $somaliLandmarkNote"
                                    } else {
                                        pickupLocation
                                    }

                                    isDriverArrivedState = false
                                    arrivalSoundPlayedForRideId = null
                                    selectedBidDriver = null
                                    viewModel.createRideRequest(
                                        pickup = finalPickup.trim(),
                                        destination = destinationLocation.trim(),
                                        vehicleType = selectedVehicle.name,
                                        offeredPrice = riderOfferText.toDoubleOrNull() ?: 1.50,
                                        pickupLat = resolvedPickup.first,
                                        pickupLng = resolvedPickup.second,
                                        destLat = destCoords.first,
                                        destLng = destCoords.second,
                                        onSuccess = { rideId ->
                                            activeRideId = rideId
                                            riderStep = RiderFlowStep.LOOKING_FOR_DRIVERS
                                        }
                                    )
                                }
                            }
                        )
                    }

                    RiderFlowStep.LOOKING_FOR_DRIVERS -> {
                        // Screen 5: Nearby Drivers & Bids
                        RiderBidsScreen(
                            pickup = pickupLocation,
                            destination = destinationLocation,
                            riderOffer = riderOfferText,
                            bids = availableBids,
                            assignedDriver = if (selectedBidDriver != null && liveRide?.status == "assigned") selectedBidDriver else null,
                            onAcceptBid = { bid ->
                                selectedBidDriver = bid
                                riderStep = RiderFlowStep.ON_TRIP_TRACKING
                            },
                            onCancelRequest = {
                                if (activeRideId != null) {
                                    viewModel.cancelRide(activeRideId!!) {
                                        activeRideId = null
                                        selectedBidDriver = null
                                        riderStep = RiderFlowStep.REQUEST_FORM
                                    }
                                } else {
                                    selectedBidDriver = null
                                    riderStep = RiderFlowStep.REQUEST_FORM
                                }
                            }
                        )
                    }

                    RiderFlowStep.ON_TRIP_TRACKING -> {
                        // Screen 6: Live Tracking
                        val driver = selectedBidDriver ?: BidDriverItem(
                            id = liveRide?.assignedDriverId ?: "drv-active",
                            name = liveRide?.assignedDriverName ?: "Darawalka Bajaajta",
                            rating = liveRide?.assignedDriverRating ?: 4.9,
                            reviewCount = 10,
                            vehicleModel = "Bajaj",
                            plateNumber = liveRide?.assignedDriverPlate ?: "M-999",
                            distanceKm = 0.5,
                            etaMinutes = liveRide?.etaMinutes ?: 3,
                            offeredPrice = liveRide?.agreedFare ?: (riderOfferText.toDoubleOrNull() ?: 1.50),
                            phone = liveRide?.assignedDriverPhone ?: "615000000"
                        )
                        RiderLiveTrackingScreen(
                            driver = driver,
                            pickup = pickupLocation.ifBlank { "Gurigaaga (KM4)" },
                            destination = destinationLocation.ifBlank { "Hodan" },
                            isDriverArrived = isDriverArrivedState,
                            intersections = intersections,
                            userLocation = userLocation,
                            liveRide = liveRide,
                            routeInfo = routeInfo,
                            rideConnected = rideConnected,
                            onPlayArrivalSound = {
                                ArrivalSoundManager.playDriverArrivalAlert(
                                    context = context,
                                    pickupLocation = pickupLocation.ifBlank { "Gurigaaga" }
                                )
                                Toast.makeText(context, "📯 Dhawaaqa imaanshaha Bajaajta waa la yeeriyay!", Toast.LENGTH_SHORT).show()
                            },
                            onSimulateArrival = {
                                isDriverArrivedState = true
                                ArrivalSoundManager.playDriverArrivalAlert(
                                    context = context,
                                    pickupLocation = pickupLocation.ifBlank { "Gurigaaga" }
                                )
                                Toast.makeText(context, "📯 Darawalku wuxuu soo gaaray gurigaaga!", Toast.LENGTH_LONG).show()
                            },
                            onOpenNavigation = {
                                val navTargetLat = liveRide?.driverLat ?: liveRide?.pickupLat ?: riderPickupGps?.first ?: userLocation?.first
                                val navTargetLng = liveRide?.driverLng ?: liveRide?.pickupLng ?: riderPickupGps?.second ?: userLocation?.second
                                openGoogleMapsNavigation(
                                    context = context,
                                    lat = navTargetLat,
                                    lng = navTargetLng,
                                    label = "Bajaaj ${driver.name} (${driver.plateNumber})",
                                    isDrivingMode = false
                                )
                            },
                            onCallDriver = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${driver.phone}"))
                                context.startActivity(intent)
                            },
                            onShareTrip = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Waxaan ku safrayaa GoBajaaj! Darawalkayga waa ${driver.name} (${driver.plateNumber})."
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "La wadaag Safarka"))
                            },
                            onCompleteTripSimulation = {
                                if (activeRideId != null) {
                                    viewModel.completeRide(activeRideId!!) {
                                        riderStep = RiderFlowStep.TRIP_COMPLETED
                                    }
                                } else {
                                    riderStep = RiderFlowStep.TRIP_COMPLETED
                                }
                            },
                            onCancel = {
                                if (activeRideId != null) {
                                    viewModel.cancelRide(activeRideId!!) {
                                        activeRideId = null
                                        selectedBidDriver = null
                                        riderStep = RiderFlowStep.REQUEST_FORM
                                    }
                                } else {
                                    selectedBidDriver = null
                                    riderStep = RiderFlowStep.REQUEST_FORM
                                }
                            }
                        )
                    }

                    RiderFlowStep.TRIP_COMPLETED -> {
                        // Screen 7: Trip Complete & Rating
                        val driver = selectedBidDriver ?: BidDriverItem(
                            id = "drv-active",
                            name = "Darawalka Bajaajta",
                            rating = 4.9,
                            reviewCount = 10,
                            vehicleModel = "Bajaj",
                            plateNumber = "M-999",
                            distanceKm = 0.5,
                            etaMinutes = 3,
                            offeredPrice = riderOfferText.toDoubleOrNull() ?: 1.50,
                            phone = "615000000"
                        )
                        RiderTripCompleteScreen(
                            driver = driver,
                            fare = driver.offeredPrice,
                            rating = tripRating,
                            onRatingChange = { tripRating = it },
                            onDone = {
                                Toast.makeText(context, "Waad ku mahadsan tahay isticmaalka GoBajaaj!", Toast.LENGTH_SHORT).show()
                                activeRideId = null
                                selectedBidDriver = null
                                riderStep = RiderFlowStep.REQUEST_FORM
                            }
                        )
                    }
                }
            } else {
                // ==========================================
                // DRIVER APP EXPERIENCE (ROW 2 OF BLUEPRINT)
                // ==========================================
                when (driverStep) {
                    DriverFlowStep.DRIVER_HOME -> {
                        // Screen 4: Driver Home
                        DriverHomeScreen(
                            isOnline = isDriverOnline,
                            onToggleOnline = {
                                isDriverOnline = !isDriverOnline
                                viewModel.setDriverOnline(isDriverOnline)
                            },
                            todayTrips = driverTodayTrips,
                            todayEarnings = driverTodayEarnings,
                            rating = driverRating,
                            intersections = intersections,
                            userLocation = userLocation,
                            driverGps = driverGps,
                            onSimulateIncomingRequest = {
                                viewModel.fetchPendingRides { list ->
                                    if (list.isNotEmpty()) {
                                        incomingRide = list.first()
                                        driverStep = DriverFlowStep.INCOMING_REQUEST
                                    } else {
                                        Toast.makeText(context, "Hadda ma jiraan dalabyo cusub oo furan", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }

                    DriverFlowStep.INCOMING_REQUEST -> {
                        // Screen 5: Ride Request (Driver Alert)
                        val pLoc = incomingRide?.pickupAddress ?: "KM4 (Muqdisho)"
                        val dLoc = incomingRide?.destinationAddress ?: "Hodan (Muqdisho)"
                        val offerVal = incomingRide?.riderOffer ?: 1.50
                        val rName = incomingRide?.riderName ?: "Macmiil GoBajaaj"
                        val rPhone = incomingRide?.riderPhone ?: ""
                        DriverRideRequestAlertScreen(
                            riderName = rName,
                            riderPhone = rPhone,
                            pickup = pLoc,
                            destination = dLoc,
                            riderOffer = "$${String.format(java.util.Locale.US, "%.2f", offerVal)}",
                            pickupLat = incomingRide?.pickupLat,
                            pickupLng = incomingRide?.pickupLng,
                            destLat = incomingRide?.destLat,
                            destLng = incomingRide?.destLng,
                            intersections = intersections,
                            userLocation = userLocation,
                            driverGps = driverGps,
                            onAccept = {
                                val rId = incomingRide?.id
                                if (rId != null) {
                                    viewModel.acceptRide(rId, incomingRide?.agreedFare ?: incomingRide?.riderOffer) {
                                        viewModel.observeRide(rId)
                                        // ✅ GPS streaming: billow u dir location-ka Supabase-ka
                                        viewModel.startDriverRideTracking(rId)
                                        val dLat = driverGps?.latitude ?: userLocation?.first ?: 2.0371
                                        val dLng = driverGps?.longitude ?: userLocation?.second ?: 45.3283
                                        val pLat = incomingRide?.pickupLat ?: 2.0371
                                        val pLng = incomingRide?.pickupLng ?: 45.3283
                                        viewModel.requestRoute(dLat, dLng, pLat, pLng)
                                        driverTodayEarnings += incomingRide?.agreedFare ?: incomingRide?.riderOffer ?: offerVal
                                        driverTodayTrips += 1
                                        driverStep = DriverFlowStep.ON_TRIP_NAV
                                    }
                                } else {
                                    driverTodayEarnings += offerVal
                                    driverTodayTrips += 1
                                    driverStep = DriverFlowStep.ON_TRIP_NAV
                                }
                            },
                            onDecline = {
                                incomingRide?.id?.let { viewModel.declineRide(it) }
                                incomingRide = null
                                // ✅ GPS streaming: jooji marka la dido
                                viewModel.stopDriverRideTracking(goOffline = false)
                                driverStep = DriverFlowStep.DRIVER_HOME
                            }
                        )
                    }

                    DriverFlowStep.ON_TRIP_NAV -> {
                        // Screen 7: Trip in Progress & Turn-by-Turn Navigation (Driver)
                        val currentRide = liveRide
                        val riderPhoneStr = incomingRide?.riderPhone ?: currentRide?.riderPhone ?: "+252 615 000 000"
                        val riderNameStr = incomingRide?.riderName ?: currentRide?.riderName ?: "Macmiil GoBajaaj"
                        val destinationStr = incomingRide?.destinationAddress ?: currentRide?.destinationAddress ?: "Hodan"
                        val pickupStr = incomingRide?.pickupAddress ?: currentRide?.pickupAddress ?: "Guriga Macmiilka"

                        val currentDriverLat = driverGps?.latitude ?: currentRide?.driverLat ?: userLocation?.first
                        val currentDriverLng = driverGps?.longitude ?: currentRide?.driverLng ?: userLocation?.second
                        val currentBearing = driverGps?.bearing ?: currentRide?.driverHeading?.toFloat() ?: 0f
                        val currentSpeed = driverGps?.speedKmh ?: currentRide?.driverSpeed ?: 0.0

                        val pLat = incomingRide?.pickupLat ?: currentRide?.pickupLat
                        val pLng = incomingRide?.pickupLng ?: currentRide?.pickupLng
                        val dLat = incomingRide?.destLat ?: currentRide?.destLat
                        val dLng = incomingRide?.destLng ?: currentRide?.destLng

                        val navLat = if (currentRide?.status == "in_progress") dLat else pLat
                        val navLng = if (currentRide?.status == "in_progress") dLng else pLng

                        DriverTripInProgressScreen(
                            riderName = riderNameStr,
                            riderPhone = riderPhoneStr,
                            pickup = pickupStr,
                            destination = destinationStr,
                            pickupLat = pLat,
                            pickupLng = pLng,
                            destLat = dLat,
                            destLng = dLng,
                            intersections = intersections,
                            userLocation = userLocation,
                            liveStatus = currentRide?.status ?: incomingRide?.status,
                            driverLatitude = currentDriverLat,
                            driverLongitude = currentDriverLng,
                            driverBearing = currentBearing,
                            driverSpeed = currentSpeed,
                            routeInfo = routeInfo,
                            onReroute = {
                                val targetLat = if (currentRide?.status == "in_progress") dLat else pLat
                                val targetLng = if (currentRide?.status == "in_progress") dLng else pLng
                                if (currentDriverLat != null && currentDriverLng != null && targetLat != null && targetLng != null) {
                                    viewModel.triggerReroute(currentDriverLat, currentDriverLng, targetLat, targetLng)
                                }
                            },
                            onCallRider = {
                                val cleanPhone = incomingRide?.riderPhone ?: "615000000"
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                context.startActivity(intent)
                            },
                            onOpenNavigation = {
                                openGoogleMapsNavigation(
                                    context = context,
                                    lat = navLat,
                                    lng = navLng,
                                    label = if (currentRide?.status == "in_progress") "Meesha Safarka ($destinationStr)" else "Goobta Pickup-ga ($pickupStr)",
                                    isDrivingMode = true
                                )
                            },
                            onStartTrip = {
                                val rId = incomingRide?.id
                                if (rId != null) {
                                    viewModel.startTrip(rId)
                                    if (currentDriverLat != null && currentDriverLng != null && dLat != null && dLng != null) {
                                        viewModel.requestRoute(currentDriverLat, currentDriverLng, dLat, dLng)
                                    }
                                }
                            },
                            onArrivedAtPickup = {
                                val rId = incomingRide?.id
                                if (rId != null) {
                                    viewModel.arriveAtPickup(rId)
                                }
                                ArrivalSoundManager.playBajajHornSequence(context)
                                Toast.makeText(context, "Waxaad soo gaartay guriga macmiilka! Codka iyo dhawaaqa imaanshaha waa la diray.", Toast.LENGTH_LONG).show()
                            },
                            onPlayHornSound = {
                                ArrivalSoundManager.playBajajHornSequence(context)
                                Toast.makeText(context, "📯 Dhawaaqa Bajaajta waa la yeeriyay!", Toast.LENGTH_SHORT).show()
                            },
                            onFinishTrip = {
                                val rId = incomingRide?.id
                                if (rId != null) {
                                    viewModel.completeRide(rId) {
                                        // ✅ GPS streaming: jooji marka safarkii dhammaado
                                        viewModel.stopDriverRideTracking(goOffline = false)
                                        Toast.makeText(context, "Safarkii wuu dhammaaday! Lacagta toos uga qaado rakaabka.", Toast.LENGTH_LONG).show()
                                        incomingRide = null
                                        viewModel.stopObservingRide()
                                        driverStep = DriverFlowStep.DRIVER_HOME
                                    }
                                } else {
                                    // ✅ GPS streaming: jooji xaaladda kasta
                                    viewModel.stopDriverRideTracking(goOffline = false)
                                    incomingRide = null
                                    driverStep = DriverFlowStep.DRIVER_HOME
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showAddressDialog) {
            CreateAddressDialog(
                currentLocation = riderPickupGps ?: userLocation,
                onDismiss = { showAddressDialog = false },
                onSave = { newAddress ->
                    viewModel.saveAddress(newAddress) {
                        pickupLocation = newAddress.fullFormattedAddress
                        riderPickupGps = newAddress.latitude to newAddress.longitude
                        showAddressDialog = false
                        Toast.makeText(context, "🏠 Guri Number ${newAddress.digitalCode} waa la kaydiyay!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}

// =========================================================================
// RIDER COMPONENTS (SCREENS 4, 5, 6, 7)
// =========================================================================

@Composable
private fun RiderRequestFormScreen(
    pickup: String,
    onPickupChange: (String) -> Unit,
    destination: String,
    onDestinationChange: (String) -> Unit,
    somaliNote: String = "",
    onSomaliNoteChange: (String) -> Unit = {},
    selectedVehicle: VehicleType,
    onVehicleSelect: (VehicleType) -> Unit,
    offer: String,
    onOfferChange: (String) -> Unit,
    nearbyDriversCount: Int = 4,
    savedAddresses: List<SavedAddress> = emptyList(),
    onSelectSavedAddress: (SavedAddress) -> Unit = {},
    onOpenAddressDialog: () -> Unit = {},
    isFetchingGps: Boolean = false,
    pickupCoordinates: Pair<Double, Double>? = null,
    onFetchGps: () -> Unit = {},
    onRequestRide: () -> Unit
) {
    val quickPresets = listOf("1.00", "1.50", "2.00", "2.50", "3.00")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Location Pickers Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Pickup Label with GPS Action Chip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Goobta lagaa qaadayo (Pickup)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoBajaajNavy
                        )
                        Surface(
                            onClick = onFetchGps,
                            shape = RoundedCornerShape(8.dp),
                            color = if (pickupCoordinates != null) GoBajaajGreenLight else Color(0xFFEFF6FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isFetchingGps) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = GoBajaajBlue
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = if (pickupCoordinates != null) GoBajaajGreen else GoBajaajBlue,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pickupCoordinates != null) "📍 GPS Sax ah" else "📍 Hel Goobtayda",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pickupCoordinates != null) GoBajaajGreen else GoBajaajBlue
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (pickupCoordinates != null) GoBajaajGreen.copy(alpha = 0.15f) else GoBajaajBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (pickupCoordinates != null) GoBajaajGreen else GoBajaajBlue)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = pickup,
                            onValueChange = onPickupChange,
                            placeholder = { Text("Geli goobtaada ama isticmaal GPS...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = onFetchGps) {
                                    Icon(
                                        imageVector = Icons.Default.GpsFixed,
                                        contentDescription = "GPS",
                                        tint = if (pickupCoordinates != null) GoBajaajGreen else GoBajaajBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = BrandBlack,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrandBlack,
                                unfocusedTextColor = BrandBlack,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = if (pickupCoordinates != null) GoBajaajGreen else GoBajaajBlue,
                                unfocusedBorderColor = if (pickupCoordinates != null) GoBajaajGreen.copy(alpha = 0.5f) else Color(0xFFCBD5E1),
                                cursorColor = GoBajaajBlue
                            )
                        )
                    }

                    if (pickupCoordinates != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✓ GPS Coordinates: ${String.format(java.util.Locale.US, "%.5f, %.5f", pickupCoordinates.first, pickupCoordinates.second)} (Darawalku wuxuu kuugu imaanayaa halkan)",
                            fontSize = 10.sp,
                            color = GoBajaajGreen,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Destination Label
                    Text(
                        text = "Meesha aad u socoto (Destination)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoBajaajNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(TrafficHeavyRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(TrafficHeavyRed)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = destination,
                            onValueChange = onDestinationChange,
                            placeholder = { Text("Geli meesha aad u socoto...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = BrandBlack,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrandBlack,
                                unfocusedTextColor = BrandBlack,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = GoBajaajBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = GoBajaajBlue
                            )
                        )
                    }
                    // Somali Landmark Note (optional directions)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Tilmaanta Goobta (Somali Directions) — Ikhtiyaari",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoBajaajNavy
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = somaliNote,
                        onValueChange = onSomaliNoteChange,
                        placeholder = {
                            Text(
                                "Tusaale: Taleex, Masaajidka horteeda, albaabka huruudi ah...",
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        leadingIcon = {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                tint = GoBajaajBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // ── Nearby Drivers 1 KM Radar Banner ──
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, GoBajaajBlue.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NearMe,
                        contentDescription = null,
                        tint = GoBajaajBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Bajaajyada 1 KM Kugu Dhow",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoBajaajNavy
                        )
                        Text(
                            "Si toos ah loo ogaan doonaa",
                            fontSize = 10.sp, color = Color(0xFF64748B)
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = GoBajaajBlue) {
                        Text(
                            "$nearbyDriversCount Bajaaj",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // ── Saved Addresses (Cinwaannadayda) ──
            if (savedAddresses.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Cinwaannadayda",
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoBajaajNavy
                        )
                        TextButton(onClick = onOpenAddressDialog) {
                            Text("+ Cusub", fontSize = 11.sp)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        savedAddresses.forEach { addr ->
                            Surface(
                                onClick = { onSelectSavedAddress(addr) },
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, GoBajaajBlue.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    Text(
                                        addr.label,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoBajaajNavy
                                    )
                                    Text(
                                        addr.digitalCode,
                                        fontSize = 10.sp, color = GoBajaajBlue, fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenAddressDialog,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("+ Keydi Cinwaankaaga (Guri Number)", fontSize = 13.sp)
                }
            }

            // Price Offer Card: "Qiimaha aad Bixinayso"
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, GoBajaajBlue.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Qiimaha aad bixinayso (Your Offer)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = GoBajaajNavy
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = GoBajaajYellow
                        ) {
                            Text(
                                text = "Gorgortan",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoBajaajNavy,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Price input field and quick stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = offer,
                            onValueChange = onOfferChange,
                            placeholder = { Text("1.50", color = Color(0xFF94A3B8), fontSize = 18.sp) },
                            leadingIcon = {
                                Text("$", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GoBajaajBlue)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = GoBajaajNavy,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = GoBajaajNavy,
                                unfocusedTextColor = GoBajaajNavy,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = GoBajaajBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                cursorColor = GoBajaajBlue
                            )
                        )

                        // Stepper buttons
                        Button(
                            onClick = {
                                val current = offer.toDoubleOrNull() ?: 1.50
                                onOfferChange(String.format(java.util.Locale.US, "%.2f", (current - 0.25).coerceAtLeast(0.50)))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text("-0.25", color = GoBajaajNavy, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                val current = offer.toDoubleOrNull() ?: 1.50
                                onOfferChange(String.format(java.util.Locale.US, "%.2f", current + 0.25))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoBajaajBlue.copy(alpha = 0.15f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text("+0.25", color = GoBajaajBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickPresets.forEach { preset ->
                            val isSelected = offer == preset
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoBajaajBlue else Color(0xFFF1F5F9),
                                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOfferChange(preset) }
                            ) {
                                Text(
                                    text = "$$preset",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else GoBajaajNavy,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Extra space so content is never covered by bottom bar
            Spacer(modifier = Modifier.height(90.dp))
        }

        // Persistent Sticky Bottom Action Bar
        Surface(
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            color = Color.White,
            shadowElevation = 16.dp,
            tonalElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Interactive Price Adjustment on bottom bar
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val current = offer.toDoubleOrNull() ?: 1.50
                            onOfferChange(String.format(java.util.Locale.US, "%.2f", (current - 0.25).coerceAtLeast(0.50)))
                        },
                        modifier = Modifier.size(32.dp).background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Text("-", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GoBajaajNavy)
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Qiimaha", fontSize = 10.sp, color = TextMutedDark)
                        Text(
                            text = if (offer.isNotBlank()) "\$$offer" else "$1.50",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = GoBajaajNavy
                        )
                    }

                    IconButton(
                        onClick = {
                            val current = offer.toDoubleOrNull() ?: 1.50
                            onOfferChange(String.format(java.util.Locale.US, "%.2f", current + 0.25))
                        },
                        modifier = Modifier.size(32.dp).background(GoBajaajBlue.copy(alpha = 0.15f), CircleShape)
                    ) {
                        Text("+", fontWeight = FontWeight.Black, fontSize = 16.sp, color = GoBajaajBlue)
                    }
                }

                Button(
                    onClick = onRequestRide,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoBajaajBlue),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("btn_request_ride_sticky")
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dalbo Hadda",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun VehicleOptionCard(
    title: String,
    price: String,
    capacity: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconDrawableRes: Int? = null,
    badge: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, GoBajaajBlue)
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
        },
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 3.dp else 1.dp
        ),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (badge != null) {
                Surface(
                    color = if (isSelected) GoBajaajYellow else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) GoBajaajNavy else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) GoBajaajBlue else Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    if (iconDrawableRes != null) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = iconDrawableRes),
                            contentDescription = title,
                            tint = if (isSelected) Color.White else GoBajaajNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = if (isSelected) Color.White else GoBajaajNavy,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) GoBajaajBlue else BrandBlack
                )

                Text(
                    text = capacity,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMutedDark
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) GoBajaajBlue else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = price,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color.White else BrandBlack,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RiderBidsScreen(
    pickup: String,
    destination: String,
    riderOffer: String,
    bids: List<BidDriverItem>,
    assignedDriver: BidDriverItem? = null,
    onAcceptBid: (BidDriverItem) -> Unit,
    onCancelRequest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onCancelRequest) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Dib u noqo")
            }
            Column {
                Text(
                    text = "Looking for drivers...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlack
                )
                Text(
                    text = "$pickup ➔ $destination • Dalabkaaga: $$riderOffer",
                    fontSize = 12.sp,
                    color = TextMutedDark
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Searching banner animation -> assigned driver card when one is found
        if (assignedDriver != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GoBajaajYellow.copy(alpha = 0.18f)),
                border = BorderStroke(1.5.dp, GoBajaajYellow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = GoBajaajNavy,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Darawal ayaa la helay!",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoBajaajNavy
                        )
                        Text(
                            text = "${assignedDriver.name} (${assignedDriver.plateNumber}) ayaa xaqiijinaya dalabkaaga...",
                            fontSize = 12.sp,
                            color = GoBajaajNavy.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = GoBajaajBlueLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = GoBajaajBlue,
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Darawallada kuugu dhow ayaa arkay dalabkaaga...",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GoBajaajBlueDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Darawallada u dhow ee soo aqbalay:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = BrandBlack
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(bids) { bid ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(GoBajaajYellow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                                    contentDescription = null,
                                    tint = GoBajaajNavy,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = bid.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandBlack
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GoBajaajAmber, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${bid.rating} (${bid.reviewCount}) • ${bid.vehicleModel}",
                                        fontSize = 11.sp,
                                        color = TextMutedDark
                                    )
                                }
                                Text(
                                    text = "${bid.distanceKm} km • ${bid.etaMinutes} min",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TrafficOpenGreen
                                )
                            }
                        }

                        Button(
                            onClick = { onAcceptBid(bid) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoBajaajGreen
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Aqbal \$${bid.offeredPrice}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onCancelRequest,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Jooji Dalabka", color = TrafficHeavyRed, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RiderLiveTrackingScreen(
    driver: BidDriverItem,
    pickup: String,
    destination: String,
    isDriverArrived: Boolean = false,
    intersections: List<Intersection> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    liveRide: RideEntity? = null,
    routeInfo: RouteInfo? = null,
    rideConnected: Boolean = false,
    onPlayArrivalSound: () -> Unit = {},
    onSimulateArrival: () -> Unit = {},
    onOpenNavigation: () -> Unit = {},
    onCallDriver: () -> Unit,
    onShareTrip: () -> Unit,
    onCompleteTripSimulation: () -> Unit,
    onCancel: () -> Unit
) {
    // Live driver position from backend when available; otherwise fall back to a simulated approach
    val baseLat = userLocation?.first ?: 2.0371
    val baseLng = userLocation?.second ?: 45.3283
    val liveLat = liveRide?.driverLat
    val liveLng = liveRide?.driverLng
    val driverLat = liveLat ?: (if (isDriverArrived) baseLat else baseLat + 0.0035)
    val driverLng = liveLng ?: (if (isDriverArrived) baseLng else baseLng - 0.0040)
    val etaMinutes = routeInfo?.etaMinutes ?: liveRide?.etaMinutes ?: driver.etaMinutes
    val distanceLabel = routeInfo?.distanceMeters?.let { "• ${it / 1000.0} km" } ?: ""
    val staleSeconds = run {
        val at = liveRide?.driverLocationAt ?: return@run null
        val parsed = try {
            val t = at.replace("Z", " UTC")
            java.time.OffsetDateTime.parse(t).toInstant().toEpochMilli()
        } catch (e: Exception) {
            return@run null
        }
        ((System.currentTimeMillis() - parsed) / 1000L).toInt()
    }
    val staleActive = staleSeconds != null && staleSeconds > 90 && !isDriverArrived
    var acknowledgedArrival by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Dynamic Top Banner based on Arrival State
        Surface(
            color = if (isDriverArrived) GoBajaajGreen else GoBajaajNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (isDriverArrived) Color.White else GoBajaajYellow)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDriverArrived) "📯 BAJAAJLIHII WUU SOO GAARAY!" else "Darawalka ayaa kusoo socda",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isDriverArrived) "Wuxuu sugayaa bannaanka: $pickup" else "ETA: $etaMinutes daq $distanceLabel ➔ $pickup",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Quick Horn Play / Sound Button
                Surface(
                    onClick = onPlayArrivalSound,
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDriverArrived) GoBajaajYellow else Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Codka",
                            tint = if (isDriverArrived) BrandBlack else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Codka",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDriverArrived) BrandBlack else Color.White
                        )
                    }
                }
            }
        }

        // Live Map view — shows both rider (blue) and approaching driver (yellow Bajaaj pin)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SomaliInteractiveMap(
                intersections = intersections,
                modifier = Modifier.fillMaxSize(),
                userLatitude = liveRide?.pickupLat ?: userLocation?.first ?: 2.0371,
                userLongitude = liveRide?.pickupLng ?: userLocation?.second ?: 45.3283,
                customUserPinLabel = "Adigu (${pickup})",
                routePointsJson = routeInfo?.pointsJson,
                driverLatitude = driverLat,
                driverLongitude = driverLng,
                driverBearing = liveRide?.driverHeading?.toFloat() ?: 0f,
                driverSpeed = liveRide?.driverSpeed ?: 0.0,
                driverPinLabel = "🛺 ${driver.name}"
            )

            // Realtime connection + stale-location status chip
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (rideConnected && !staleActive) Color.White.copy(alpha = 0.95f) else TrafficHeavyRed.copy(alpha = 0.95f),
                    shadowElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (rideConnected && !staleActive) GoBajaajGreen else TrafficHeavyRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when {
                                isDriverArrived -> "Darawalku wuu yimid"
                                staleActive -> "Xiriirkii go'ay — cusboonayn"
                                rideConnected -> "Realtime oo wacan"
                                else -> "Realtime oo la sugayo"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlack
                        )
                    }
                }
            }

            // External navigation button (Google Maps)
            Surface(
                onClick = onOpenNavigation,
                shape = RoundedCornerShape(12.dp),
                color = GoBajaajYellow,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = BrandBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🗺️ Ku Daawe Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Black, color = BrandBlack)
                }
            }

            // Arrival Notification Dialog / Banner overlay when driver arrives
            if (isDriverArrived && !acknowledgedArrival) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(2.dp, GoBajaajGreen),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(GoBajaajGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = GoBajaajGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "📯 Bajaajlihii Wuu Yimid!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = BrandBlack
                                )
                                Text(
                                    text = "${driver.name} (${driver.plateNumber}) wuxuu joogaa bannaanka.",
                                    fontSize = 12.sp,
                                    color = TextMutedDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPlayArrivalSound,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoBajaajBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Codka Yeeri", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoBajaajBlue)
                            }

                            Button(
                                onClick = { acknowledgedArrival = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GoBajaajGreen),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("Waan Soo Baxayaa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // Driver Bottom Card (Matching Screen 6)
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(GoBajaajYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = GoBajaajNavy, modifier = Modifier.size(30.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = driver.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandBlack
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoBajaajAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${driver.rating} • ${driver.vehicleModel}",
                                    fontSize = 12.sp,
                                    color = TextMutedDark
                                )
                            }
                            Text(
                                text = "Plate: ${driver.plateNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoBajaajNavy
                            )
                            if (liveRide?.hasLiveDriverLocation == true) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = buildString {
                                        append(if (rideConnected) "Realtime oo wacan" else "Xiriir la'aan")
                                        if (liveRide.driverSpeed != null) append(" • ")
                                        if (liveRide.driverSpeed != null) append("${liveRide.driverSpeed.toInt()} km/h")
                                        if (staleActive) append(" • Xig. saacadi hore")
                                    },
                                    fontSize = 10.sp,
                                    color = if (staleActive) TrafficHeavyRed else GoBajaajGreen
                                )
                            }
                        }
                    }

                    // Sound, Call and Share buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onPlayArrivalSound,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoBajaajYellow.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "Codka Bajaajta", tint = GoBajaajNavy)
                        }
                        IconButton(
                            onClick = onCallDriver,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoBajaajGreenLight)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Wac Darawalka", tint = GoBajaajGreen)
                        }
                        IconButton(
                            onClick = onShareTrip,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GoBajaajBlueLight)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "La wadaag Safarka", tint = GoBajaajBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Heshiiska Qiimaha (Agreed Fare):", fontSize = 11.sp, color = TextMutedDark)
                        Text(
                            text = "\$${driver.offeredPrice}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoBajaajGreen
                        )
                    }

                    Button(
                        onClick = onCompleteTripSimulation,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoBajaajNavy)
                    ) {
                        Text("Safarkii waa dhammaaday", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RiderTripCompleteScreen(
    driver: BidDriverItem,
    fare: Double,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Green Checkmark Icon (Matching Screen 7)
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(GoBajaajGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Trip Completed",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BrandBlack
        )
        Text(
            text = "Thank you for using GoBajaaj",
            fontSize = 14.sp,
            color = TextMutedDark
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Total Fare Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Total Fare",
                    fontSize = 13.sp,
                    color = TextMutedDark
                )
                Text(
                    text = "\$$fare",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlack
                )
                Text(
                    text = "Paid to Driver (Cash / Mobile Money)",
                    fontSize = 12.sp,
                    color = TrafficOpenGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "GoBajaaj commission = \$0.00 (100% to Driver)",
                    fontSize = 11.sp,
                    color = TextMutedDark
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rating Stars
        Text(
            text = "Rate your driver",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = BrandBlack
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Star $i",
                    tint = GoBajaajAmber,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onRatingChange(i) }
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = onDone,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoBajaajBlue),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Done", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

// =========================================================================
// DRIVER COMPONENTS (SCREENS 4, 5, 6, 7)
// =========================================================================

@Composable
private fun DriverHomeScreen(
    isOnline: Boolean,
    onToggleOnline: () -> Unit,
    todayTrips: Int,
    todayEarnings: Double,
    rating: Double,
    intersections: List<Intersection> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    driverGps: SomaliTrafficViewModel.DriverGpsData? = null,
    onSimulateIncomingRequest: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Driver Header
        Surface(
            color = GoBajaajNavy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "GoBajaaj Driver",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Adeegga: ACTIVE (\$0.25/maalinle) • 0% Commission",
                        color = GoBajaajYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Online/Offline Pill Badge
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isOnline) GoBajaajGreenLight else Color.White.copy(alpha = 0.2f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) GoBajaajGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) GoBajaajGreen else Color.White
                        )
                    }
                }
            }
        }

        // Stats Row (Matching Screen 4: Today's Trips, Earnings, Rating)
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Today's Trips", fontSize = 11.sp, color = TextMutedDark)
                    Text(
                        text = "$todayTrips",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlack
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = BrandBorder
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Earnings", fontSize = 11.sp, color = TextMutedDark)
                    Text(
                        text = "\$$todayEarnings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoBajaajBlue
                    )
                }

                Divider(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp),
                    color = BrandBorder
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Rating", fontSize = 11.sp, color = TextMutedDark)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = GoBajaajAmber, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "$rating",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlack
                        )
                    }
                }
            }
        }

        // Map View
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SomaliInteractiveMap(
                intersections = intersections,
                modifier = Modifier.fillMaxSize(),
                userLatitude = driverGps?.latitude ?: userLocation?.first ?: 2.0371,
                userLongitude = driverGps?.longitude ?: userLocation?.second ?: 45.3283,
                driverLatitude = driverGps?.latitude ?: userLocation?.first,
                driverLongitude = driverGps?.longitude ?: userLocation?.second,
                driverBearing = driverGps?.bearing ?: 0f,
                driverSpeed = driverGps?.speedKmh ?: 0.0,
                followDriver = isOnline,
                driverPinLabel = "🛺 Darawal (Online)",
                customUserPinLabel = "Goobtaada Hadda"
            )

            // Live status and check requests overlay
            if (isOnline) {
                // Top control bar spanning the screen cleanly with comfortable spacing
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Online Status Indicator Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(GoBajaajGreen)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "Diyaar u ah dalabaadka",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right: "Dalab Cusub" Simulation / Action Button with distinct styling and no overlap
                    Surface(
                        onClick = onSimulateIncomingRequest,
                        shape = RoundedCornerShape(20.dp),
                        color = GoBajaajYellow,
                        shadowElevation = 5.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEAB308))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                                contentDescription = null,
                                tint = BrandBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dalab Cusub",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandBlack
                            )
                        }
                    }
                }
            }
        }

        // Big Bottom Toggle Button [Go Offline] / [Go Online]
        Surface(
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onToggleOnline,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isOnline) TrafficHeavyRed else GoBajaajGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp)
            ) {
                Text(
                    text = if (isOnline) "Go Offline" else "Go Online (Bilow Shaqada)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DriverRideRequestAlertScreen(
    riderName: String = "Macmiil GoBajaaj",
    riderPhone: String = "",
    pickup: String,
    destination: String,
    riderOffer: String,
    pickupLat: Double? = null,
    pickupLng: Double? = null,
    destLat: Double? = null,
    destLng: Double? = null,
    intersections: List<Intersection> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    driverGps: SomaliTrafficViewModel.DriverGpsData? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Map Preview
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SomaliInteractiveMap(
                intersections = intersections,
                modifier = Modifier.fillMaxSize(),
                userLatitude = driverGps?.latitude ?: userLocation?.first ?: 2.0371,
                userLongitude = driverGps?.longitude ?: userLocation?.second ?: 45.3283,
                driverLatitude = driverGps?.latitude ?: userLocation?.first,
                driverLongitude = driverGps?.longitude ?: userLocation?.second,
                driverBearing = driverGps?.bearing ?: 0f,
                driverPinLabel = "🛺 Darawal",
                pickupLatitude = pickupLat,
                pickupLongitude = pickupLng,
                pickupLabel = "📍 Ka qaad ($pickup)",
                destinationLatitude = destLat,
                destinationLongitude = destLng,
                destinationLabel = "🏁 U socda ($destination)"
            )
        }

        // Incoming Request Card (Matching Screen 5)
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = GoBajaajAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dalab Cusub oo Bajaaj ah",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandBlack
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Passenger Details (Magaca & Taleefanka Macmiilka)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoBajaajBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = GoBajaajBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = riderName.ifBlank { "Macmiil GoBajaaj" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = BrandBlack
                            )
                            if (riderPhone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "📞 $riderPhone",
                                    fontSize = 12.5.sp,
                                    color = TextMutedDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (riderPhone.isNotBlank()) {
                            val context = LocalContext.current
                            IconButton(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${riderPhone.trim()}")
                                    }
                                    context.startActivity(callIntent)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoBajaajGreenLight)
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Wac Macmiilka",
                                    tint = GoBajaajGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Route details (Halka uu macmiilku joogo & meesha loo socdo)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandLightGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Pickup
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(GoBajaajGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Halka uu joogo (Pickup):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMutedDark
                                )
                                Text(
                                    text = pickup,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = BrandBlack
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(TrafficHeavyRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Halka loo socdo (Destination):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextMutedDark
                                )
                                Text(
                                    text = destination,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = BrandBlack
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Qiimaha rakaabka: $riderOffer",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoBajaajGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2 Action Buttons: [Aqbal] and [Diid] — Gorgortan removed
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoBajaajGreen),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("✅ Aqbal Dalabka ($riderOffer)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDecline,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrafficHeavyRed),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Diid Dalabka", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// DriverCounterOfferScreen removed — gorgortan (bidding) has been disabled per product decision.

@Composable
private fun DriverTripInProgressScreen(
    riderName: String = "Macmiil GoBajaaj",
    riderPhone: String,
    pickup: String = "KM4",
    destination: String,
    pickupLat: Double? = null,
    pickupLng: Double? = null,
    destLat: Double? = null,
    destLng: Double? = null,
    intersections: List<Intersection> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    liveStatus: String? = null,
    driverLatitude: Double? = null,
    driverLongitude: Double? = null,
    driverBearing: Float? = null,
    driverSpeed: Double? = null,
    routeInfo: RouteInfo? = null,
    onReroute: (() -> Unit)? = null,
    onOpenNavigation: () -> Unit = {},
    onCallRider: () -> Unit,
    onStartTrip: () -> Unit = {},
    onArrivedAtPickup: () -> Unit = {},
    onPlayHornSound: () -> Unit = {},
    onFinishTrip: () -> Unit
) {
    var tripStatus by remember { mutableStateOf("GOING_TO_PICKUP") } // GOING_TO_PICKUP, ARRIVED, ON_TRIP
    // Keep local trip phase in sync with the backend status
    LaunchedEffect(liveStatus) {
        tripStatus = when (liveStatus) {
            "arrived" -> "ARRIVED"
            "in_progress" -> "ON_TRIP"
            else -> tripStatus
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Banner (Matching Screen 7)
        Surface(
            color = when (tripStatus) {
                "ARRIVED" -> GoBajaajGreen
                "ON_TRIP" -> GoBajaajNavy
                else -> GoBajaajBlueDark
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (tripStatus == "ARRIVED") Color.White else GoBajaajYellow)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (tripStatus) {
                            "GOING_TO_PICKUP" -> "Aadista Guriga Macmiilka"
                            "ARRIVED" -> "📯 Ku Sugan Guriga Macmiilka"
                            else -> "On Trip • U Socda Goobta"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = when (tripStatus) {
                            "GOING_TO_PICKUP" -> "Ka qaad: $pickup"
                            "ARRIVED" -> "Macmiilka ayaa usoo baxaya gaariga ($pickup)"
                            else -> "U socda: $destination"
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp
                    )
                }

                if (tripStatus == "ARRIVED" || tripStatus == "GOING_TO_PICKUP") {
                    Surface(
                        onClick = onPlayHornSound,
                        shape = RoundedCornerShape(16.dp),
                        color = GoBajaajYellow,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.VolumeUp,
                                contentDescription = "Yeeri Horn",
                                tint = BrandBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Horn",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlack
                            )
                        }
                    }
                }
            }
        }

        // Navigation Map
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SomaliInteractiveMap(
                intersections = intersections,
                modifier = Modifier.fillMaxSize(),
                userLatitude = driverLatitude ?: userLocation?.first ?: 2.0371,
                userLongitude = driverLongitude ?: userLocation?.second ?: 45.3283,
                routePointsJson = routeInfo?.pointsJson,
                driverLatitude = driverLatitude ?: userLocation?.first,
                driverLongitude = driverLongitude ?: userLocation?.second,
                driverBearing = driverBearing ?: 0f,
                driverSpeed = driverSpeed ?: 0.0,
                followDriver = true,
                driverPinLabel = "🛺 Bajaaj (Live Nav)",
                pickupLatitude = pickupLat,
                pickupLongitude = pickupLng,
                pickupLabel = "📍 Ka qaad ($pickup)",
                destinationLatitude = destLat,
                destinationLongitude = destLng,
                destinationLabel = "🏁 U socda ($destination)",
                navigationSteps = routeInfo?.steps ?: emptyList(),
                routeDistanceMeters = routeInfo?.distanceMeters,
                routeDurationSeconds = routeInfo?.durationSeconds,
                onReroute = onReroute
            )

            // Google Maps Navigation Button (Always accessible for driver)
            Surface(
                onClick = onOpenNavigation,
                shape = RoundedCornerShape(12.dp),
                color = GoBajaajYellow,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = BrandBlack, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (tripStatus) {
                            "GOING_TO_PICKUP" -> "🗺️ Fur Google Maps (U Tag Macmiilka)"
                            "ARRIVED" -> "🗺️ Fur Google Maps (Albaabka Macmiilka)"
                            else -> "🗺️ Fur Google Maps (U Wad Meesha Loo Socdo)"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandBlack
                    )
                }
            }
        }

        // Bottom Rider Info & Controls Card (Matching Screen 7)
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Live ETA & Distance & Speed Status Card (Realtime update)
                if (routeInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Distance", fontSize = 10.5.sp, color = TextMutedDark, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = OsmRoutingService.formatDistance(routeInfo.distanceMeters),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlack
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ETA", fontSize = 10.5.sp, color = TextMutedDark, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = OsmRoutingService.formatEta(routeInfo.durationSeconds),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoBajaajGreen
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Xawaaraha", fontSize = 10.5.sp, color = TextMutedDark, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${(driverSpeed ?: 0.0).toInt()} km/h",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoBajaajNavy
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = riderName.ifBlank { "Macmiil GoBajaaj" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandBlack
                        )
                        Text(
                            text = "📞 $riderPhone",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMutedDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (tripStatus == "GOING_TO_PICKUP") "📍 Ka qaad: $pickup" else "🏁 U gee: $destination",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (tripStatus == "GOING_TO_PICKUP") GoBajaajGreen else GoBajaajBlue,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onCallRider,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(GoBajaajGreenLight)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Wac Rakaabka", tint = GoBajaajGreen)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progression buttons: Arrived -> Start Trip -> Complete Trip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (tripStatus == "GOING_TO_PICKUP") {
                        Button(
                            onClick = {
                                tripStatus = "ARRIVED"
                                onArrivedAtPickup()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoBajaajGreen),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("📯 Waan Iimid Guriga (Codka Yeeri)", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else if (tripStatus == "ARRIVED") {
                        OutlinedButton(
                            onClick = onPlayHornSound,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.45f).height(48.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = GoBajaajNavy)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Yeeri Horn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoBajaajNavy)
                        }

                        Button(
                            onClick = {
                                tripStatus = "ON_TRIP"
                                onStartTrip()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoBajaajGreen),
                            modifier = Modifier.weight(0.55f).height(48.dp)
                        ) {
                            Text("▶ Bilow Safarka", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = onFinishTrip,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoBajaajNavy),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("🏁 Dhamee Safarka (Complete Trip)", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun resolveIntersectionCoords(text: String, intersections: List<Intersection>): Pair<Double, Double>? {
    if (text.isBlank()) return null
    val t = text.lowercase(java.util.Locale.US).trim()
    return intersections.firstOrNull {
        it.name.lowercase(java.util.Locale.US).contains(t) ||
            t.contains(it.name.lowercase(java.util.Locale.US))
    }?.let { it.latitude to it.longitude }
}

private fun openGoogleMapsNavigation(
    context: android.content.Context,
    lat: Double?,
    lng: Double?,
    label: String = "Goobta Safarka",
    isDrivingMode: Boolean = true
) {
    val cleanLocation = label
        .replace("Meesha Safarka (", "")
        .replace("Goobta Pickup-ga (", "")
        .replace("Albaabka Macmiilka (", "")
        .replace(")", "")
        .trim()
        .ifBlank { "Mogadishu" }

    val hasValidCoords = lat != null && lng != null && lat != 0.0 && lng != 0.0

    val navUri = if (hasValidCoords) {
        if (isDrivingMode) {
            // Google Maps Turn-by-Turn Driving Navigation
            Uri.parse("google.navigation:q=$lat,$lng&mode=d")
        } else {
            // Google Maps Direct Directions
            Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving")
        }
    } else {
        // Fallback: search place query in Mogadishu via Google Maps
        val query = if (cleanLocation.contains("Mogadishu", ignoreCase = true)) cleanLocation else "$cleanLocation, Mogadishu"
        Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(query)}&travelmode=driving")
    }

    val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        // Fallback 1: Web / universal intent (opens Google Maps or browser)
        try {
            val fallbackUri = if (hasValidCoords) {
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving")
            } else {
                val query = if (cleanLocation.contains("Mogadishu", ignoreCase = true)) cleanLocation else "$cleanLocation, Mogadishu"
                Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(query)}&travelmode=driving")
            }
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (_: Exception) {
            // Fallback 2: Geo scheme
            try {
                val geoUri = if (hasValidCoords) {
                    Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(cleanLocation)})")
                } else {
                    Uri.parse("geo:0,0?q=${Uri.encode("$cleanLocation, Mogadishu")}")
                }
                context.startActivity(Intent(Intent.ACTION_VIEW, geoUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            } catch (_: Exception) {
                Toast.makeText(context, "Fadlan soo degso ama fur app-ka Google Maps", Toast.LENGTH_LONG).show()
            }
        }
    }
}

// =========================================================================
// CREATE ADDRESS DIALOG — GoBajaaj Digital House Number
// =========================================================================

@Composable
private fun CreateAddressDialog(
    currentLocation: Pair<Double, Double>?,
    onDismiss: () -> Unit,
    onSave: (SavedAddress) -> Unit
) {
    val districts = listOf(
        "Hodan", "Hawlwadaag", "Waberi", "Wadajir", "Dharkenley",
        "Dayniile", "Kaxda", "Yaaqshiid", "Kaaraan", "Heliwaa",
        "Boondheere", "Shibis", "Cabdicasis", "Warta Nabadda",
        "Shangaani", "Xamar Weyne", "Xamar Jajab", "Garasbaaley",
        "Ceelasha Biyaha", "Tareedisho", "Gubadleey", "Jazeera", "Daarusalaam"
    )

    var label by remember { mutableStateOf("Gurigeyga") }
    var district by remember { mutableStateOf("Hodan") }
    var somaliDir by remember { mutableStateOf("") }
    var randomSuffix by remember { mutableStateOf((100..999).random()) }

    // Automatic Guri Number generation based on selected or typed district
    val cleanDistrict = district.trim().uppercase()
        .filter { it.isLetterOrDigit() }
        .take(8)
        .ifBlank { "MOG" }
    val digitalCode = "MG-$cleanDistrict-$randomSuffix"

    val lat = currentLocation?.first ?: 2.0371
    val lng = currentLocation?.second ?: 45.3283

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "🏠 Keydi Guri Number & Cinwaan",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = GoBajaajNavy
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Digital Code Display (Automatic Generated Guri Number)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Koodhka Guri Number-ka:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF15803D)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF15803D).copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        "Automatic",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = digitalCode,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF15803D)
                            )
                        }

                        // Regenerate suffix button
                        IconButton(
                            onClick = { randomSuffix = (100..999).random() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7))
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Beddel Number-ka",
                                tint = Color(0xFF15803D),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Label Chips (Nooca Goobta)
                Text("Nooca Goobta:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Gurigeyga" to "🏠", "Shaqada" to "🏢", "Baqaalka" to "🛒", "Kale" to "📍").forEach { (lbl, emoji) ->
                        Surface(
                            onClick = { label = lbl },
                            shape = RoundedCornerShape(8.dp),
                            color = if (label == lbl) GoBajaajBlue else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                "$emoji $lbl",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (label == lbl) Color.White else GoBajaajNavy,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // District Section: User can TYPE their district OR CLICK from the list
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Degmadaada (Qor ama hoos ka dooro):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { 
                            district = it 
                        },
                        placeholder = { Text("Qor degmadaada (Tusaale: Hodan, Waberi, Kaxda...)", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text("Degmooyinka Muqdisho & Nawaaxigeeda:", fontSize = 11.sp, color = Color(0xFF64748B))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        districts.forEach { d ->
                            val isSelected = district.equals(d, ignoreCase = true)
                            Surface(
                                onClick = { 
                                    district = d 
                                    randomSuffix = (100..999).random()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) GoBajaajAmber else Color(0xFFF1F5F9),
                                border = if (isSelected) BorderStroke(1.dp, GoBajaajYellow) else null
                            ) {
                                Text(
                                    d,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) GoBajaajNavy else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Somali Directions
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Tilmaanta Soomaalida (Landmark):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = somaliDir,
                        onValueChange = { somaliDir = it },
                        placeholder = {
                            Text(
                                "Tusaale: Masaajidka horteeda, albaabka casaanka ah, ka soo horjeedka baqaalka...",
                                fontSize = 11.5.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 3
                    )
                }

                // GPS Status
                if (currentLocation != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "📍 GPS Sax ah: ${String.format(java.util.Locale.US, "%.5f, %.5f", lat, lng)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚠️ GPS Lama Helin. Fadlan shid GPS-ka taleefanka si goobtaadu u noqoto mid sax ah.",
                                fontSize = 10.5.sp,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        SavedAddress(
                            digitalCode = digitalCode,
                            label = label,
                            district = district.trim().ifBlank { "Hodan" },
                            somaliDirections = somaliDir.trim(),
                            latitude = lat,
                            longitude = lng
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoBajaajBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Keydi Guri Number-ka", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ka Noqo") }
        }
    )
}

private fun openExternalNavigation(
    context: android.content.Context,
    lat: Double?,
    lng: Double?,
    label: String
) {
    openGoogleMapsNavigation(context, lat, lng, label, isDrivingMode = true)
}
