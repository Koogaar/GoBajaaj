package com.example.somalitraffic.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.data.model.RideEntity
import com.example.somalitraffic.data.model.VehicleType
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

    // Mode Toggle: Rider vs Driver
    var isDriverMode by remember {
        mutableStateOf(currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER)
    }

    // ----------------------------------------------------
    // RIDER STATE (Screens 4, 5, 6, 7 from Blueprint)
    // ----------------------------------------------------
    var riderStep by remember { mutableStateOf(RiderFlowStep.REQUEST_FORM) }
    var pickupLocation by remember { mutableStateOf("") }
    var destinationLocation by remember { mutableStateOf("") }
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

    LaunchedEffect(Unit) {
        // Initialise system notification channels once
        NotificationHelper.createChannels(context)
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
                            plateNumber = ride.assignedDriverPlate ?: "TR-1001",
                            distanceKm = 0.5,
                            etaMinutes = ride.etaMinutes,
                            offeredPrice = ride.agreedFare ?: ride.riderOffer,
                            phone = ride.assignedDriverPhone ?: "615000000"
                        )
                        // Show notification that driver accepted (visible even if app is in background)
                        NotificationHelper.showRideAcceptedNotification(
                            context = context,
                            driverName = ride.assignedDriverName ?: "Bajaajlaha",
                            plate = ride.assignedDriverPlate ?: "TR-1001",
                            etaMinutes = ride.etaMinutes
                        )
                        riderStep = RiderFlowStep.ON_TRIP_TRACKING
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

    // Real-time Driver Polling for incoming pending rides
    LaunchedEffect(isDriverOnline, driverStep) {
        while (isDriverOnline && driverStep == DriverFlowStep.DRIVER_HOME) {
            viewModel.fetchPendingRides { list ->
                if (list.isNotEmpty()) {
                    val ride = list.first()
                    incomingRide = ride
                    driverStep = DriverFlowStep.INCOMING_REQUEST
                    // Notify driver of new ride (shows even if app is minimised)
                    NotificationHelper.showNewRideRequestNotification(
                        context = context,
                        pickup = ride.pickupAddress,
                        destination = ride.destinationAddress,
                        offer = "$${String.format(java.util.Locale.US, "%.2f", ride.riderOffer)}"
                    )
                    // Horn beep for driver
                    ArrivalSoundManager.playSingleHornBeep(context)
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
                            onRequestRide = {
                                if (pickupLocation.trim().isEmpty()) {
                                    Toast.makeText(context, "Fadlan qor goobta lagaa qaadayo (Pickup)", Toast.LENGTH_SHORT).show()
                                } else if (destinationLocation.trim().isEmpty()) {
                                    Toast.makeText(context, "Fadlan qor meesha aad u socoto (Destination)", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.createRideRequest(
                                        pickup = pickupLocation.trim(),
                                        destination = destinationLocation.trim(),
                                        vehicleType = selectedVehicle.name,
                                        offeredPrice = riderOfferText.toDoubleOrNull() ?: 1.50,
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
                            onAcceptBid = { bid ->
                                selectedBidDriver = bid
                                riderStep = RiderFlowStep.ON_TRIP_TRACKING
                            },
                            onCancelRequest = {
                                if (activeRideId != null) {
                                    viewModel.cancelRide(activeRideId!!) {
                                        activeRideId = null
                                        riderStep = RiderFlowStep.REQUEST_FORM
                                    }
                                } else {
                                    riderStep = RiderFlowStep.REQUEST_FORM
                                }
                            }
                        )
                    }

                    RiderFlowStep.ON_TRIP_TRACKING -> {
                        // Screen 6: Live Tracking
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
                        RiderLiveTrackingScreen(
                            driver = driver,
                            pickup = pickupLocation.ifBlank { "Gurigaaga (KM4)" },
                            destination = destinationLocation.ifBlank { "Hodan" },
                            isDriverArrived = isDriverArrivedState,
                            intersections = intersections,
                            userLocation = userLocation,
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
                                        riderStep = RiderFlowStep.REQUEST_FORM
                                    }
                                } else {
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
                            onToggleOnline = { isDriverOnline = !isDriverOnline },
                            todayTrips = driverTodayTrips,
                            todayEarnings = driverTodayEarnings,
                            rating = driverRating,
                            intersections = intersections,
                            userLocation = userLocation,
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
                        DriverRideRequestAlertScreen(
                            pickup = pLoc,
                            destination = dLoc,
                            riderOffer = "$${String.format(java.util.Locale.US, "%.2f", offerVal)}",
                            intersections = intersections,
                            userLocation = userLocation,
                            onAccept = {
                                val rId = incomingRide?.id
                                if (rId != null) {
                                    viewModel.acceptRide(rId, offerVal) {
                                        driverTodayEarnings += offerVal
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
                                incomingRide = null
                                driverStep = DriverFlowStep.DRIVER_HOME
                            }
                        )
                    }

                    DriverFlowStep.ON_TRIP_NAV -> {
                        // Screen 7: Trip in Progress
                        val riderPhoneStr = incomingRide?.riderPhone ?: "+252 615 000 000"
                        val destinationStr = incomingRide?.destinationAddress ?: "Hodan"
                        val pickupStr = incomingRide?.pickupAddress ?: "Guriga Macmiilka"
                        DriverTripInProgressScreen(
                            riderPhone = riderPhoneStr,
                            pickup = pickupStr,
                            destination = destinationStr,
                            intersections = intersections,
                            userLocation = userLocation,
                            onCallRider = {
                                val cleanPhone = incomingRide?.riderPhone ?: "615000000"
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
                                context.startActivity(intent)
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
                                        Toast.makeText(context, "Safarkii wuu dhammaaday! Lacagta toos uga qaado rakaabka.", Toast.LENGTH_LONG).show()
                                        incomingRide = null
                                        driverStep = DriverFlowStep.DRIVER_HOME
                                    }
                                } else {
                                    Toast.makeText(context, "Safarkii wuu dhammaaday! Lacagta toos uga qaado rakaabka.", Toast.LENGTH_LONG).show()
                                    incomingRide = null
                                    driverStep = DriverFlowStep.DRIVER_HOME
                                }
                            }
                        )
                    }
                }
            }
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
    selectedVehicle: VehicleType,
    onVehicleSelect: (VehicleType) -> Unit,
    offer: String,
    onOfferChange: (String) -> Unit,
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
                    // Pickup Label
                    Text(
                        text = "Goobta lagaa qaadayo (Pickup)",
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
                                .background(GoBajaajBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(GoBajaajBlue)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = pickup,
                            onValueChange = onPickupChange,
                            placeholder = { Text("Geli goobta lagaa qaadayo...", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
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
                                text = "Gorgortan Xor ah",
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

        // Searching banner animation
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
    onPlayArrivalSound: () -> Unit = {},
    onSimulateArrival: () -> Unit = {},
    onCallDriver: () -> Unit,
    onShareTrip: () -> Unit,
    onCompleteTripSimulation: () -> Unit,
    onCancel: () -> Unit
) {
    // Simulate driver position: approaching from pickup offset, arrives at pickup when isDriverArrived
    val baseLat = userLocation?.first ?: 2.0371
    val baseLng = userLocation?.second ?: 45.3283
    val driverLat = if (isDriverArrived) baseLat else baseLat + 0.0035
    val driverLng = if (isDriverArrived) baseLng else baseLng - 0.0040
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
                        text = if (isDriverArrived) "Wuxuu sugayaa bannaanka: $pickup" else "ETA: ${driver.etaMinutes} daq ➔ $pickup",
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
                userLatitude = userLocation?.first ?: 2.0371,
                userLongitude = userLocation?.second ?: 45.3283,
                customUserPinLabel = "Adigu (${pickup})",
                driverLatitude = driverLat,
                driverLongitude = driverLng,
                driverPinLabel = "📯 ${driver.name}"
            )

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
                userLatitude = userLocation?.first ?: 2.0371,
                userLongitude = userLocation?.second ?: 45.3283,
                customUserPinLabel = "Goobtaada Hadda (Online)"
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
    pickup: String,
    destination: String,
    riderOffer: String,
    intersections: List<Intersection> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Map Preview
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            SomaliInteractiveMap(
                intersections = intersections,
                modifier = Modifier.fillMaxSize(),
                userLatitude = userLocation?.first ?: 2.0371,
                userLongitude = userLocation?.second ?: 45.3283
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
                        text = "New Ride Request",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandBlack
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Route details
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BrandLightGray,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Pickup
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoBajaajGreen))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = pickup,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = BrandBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(TrafficHeavyRed))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = destination,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = BrandBlack,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
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
    riderPhone: String,
    pickup: String = "KM4",
    destination: String,
    intersections: List<Intersection> = emptyList(),
    userLocation: Pair<Double, Double>? = null,
    onCallRider: () -> Unit,
    onArrivedAtPickup: () -> Unit = {},
    onPlayHornSound: () -> Unit = {},
    onFinishTrip: () -> Unit
) {
    var tripStatus by remember { mutableStateOf("GOING_TO_PICKUP") } // GOING_TO_PICKUP, ARRIVED, ON_TRIP

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
                userLatitude = userLocation?.first ?: 2.0371,
                userLongitude = userLocation?.second ?: 45.3283,
                customUserPinLabel = "Bajaaj (Live Nav)"
            )
        }

        // Bottom Rider Info & Controls Card (Matching Screen 7)
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
                    Column {
                        Text(
                            text = "Rider: $riderPhone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandBlack
                        )
                        Text(
                            text = if (tripStatus == "GOING_TO_PICKUP") "Pickup: $pickup" else "Destination: $destination",
                            fontSize = 13.sp,
                            color = TextMutedDark
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
                            onClick = { tripStatus = "ON_TRIP" },
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
