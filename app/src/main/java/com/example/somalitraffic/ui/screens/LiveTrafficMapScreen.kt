package com.example.somalitraffic.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliBottomNavigation
import com.example.somalitraffic.ui.components.SomaliInteractiveMap
import com.example.somalitraffic.util.ArrivalSoundManager
import com.example.ui.theme.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

private enum class TrafficMapMode {
    RIDES,
    TRAFFIC,
    ROAD_CLOSURES
}

private data class RoadClosureItem(
    val id: String,
    val roadName: String,
    val area: String,
    val reason: String,
    val timeWindow: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrafficMapScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit,
    onIntersectionClick: (String) -> Unit
) {
    val context = LocalContext.current
    val intersections by viewModel.intersections.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val selectedIntersection by viewModel.selectedIntersection.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDriver = currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER
    val subscription by viewModel.subscription.collectAsState()
    val isPassActive = viewModel.isSubscriptionActive(subscription)

    var mapMode by remember { mutableStateOf(TrafficMapMode.TRAFFIC) }
    var showReportDialog by remember { mutableStateOf(false) }

    // Incident Report Dialog state
    var reportType by remember { mutableStateOf("Traffic Jam") }
    var reportLocation by remember { mutableStateOf("KM4 (Mogadishu)") }
    var reportDescription by remember { mutableStateOf("") }
    var reportSeverity by remember { mutableStateOf("Medium") }

    val roadClosures = remember {
        listOf(
            RoadClosureItem("1", "Wadada Maka Al-Mukarama", "Hodan - Waberi", "Dayactir Jidka & Amni", "06:00 - 18:00", "ACTIVE"),
            RoadClosureItem("2", "Wadada Wadnaha (Isgoyska Banaadir)", "Hodan", "Mashquul & Biyo-dhigasho", "Socota", "ACTIVE"),
            RoadClosureItem("3", "Wadada 21st October", "Waaberi", "Dhisme Buundo", "2 maalmood", "PLANNED")
        )
    }

    // Driver search text state for finding intersections
    var localSearchText by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }

    // Search results using IntersectionSearchEngine
    val searchResults = remember(localSearchText, intersections, userLocation) {
        if (localSearchText.trim().isEmpty()) {
            emptyList()
        } else {
            com.example.somalitraffic.data.repository.IntersectionSearchEngine.search(
                query = localSearchText,
                intersections = intersections,
                userLat = userLocation?.first,
                userLng = userLocation?.second
            ).take(5)
        }
    }

    // Driver dynamic driving telemetry
    var driverSpeedKmh by remember { mutableDoubleStateOf(0.0) }
    var driverBearing by remember { mutableFloatStateOf(0f) }
    var isAudioAlertEnabled by remember { mutableStateOf(true) }
    var lastAlertedIntersectionId by remember { mutableStateOf<String?>(null) }

    fun calcDistMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double =
        com.example.somalitraffic.data.repository.IntersectionSearchEngine.calculateDistanceInMeters(lat1, lon1, lat2, lon2)

    fun calcBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = Math.sin(deltaLambda) * Math.cos(phi2)
        val x = Math.cos(phi1) * Math.sin(phi2) - Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda)
        val theta = Math.atan2(y, x)
        return ((Math.toDegrees(theta) + 360.0) % 360.0).toFloat()
    }

    fun diffBearing(b1: Float, b2: Float): Float =
        Math.abs((b1 - b2 + 180f) % 360f - 180f)

    fun openGoogleMapsForIntersection(lat: Double, lng: Double, label: String) {
        try {
            val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (_: Exception) {
            try {
                val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving")
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK })
            } catch (_: Exception) {
                Toast.makeText(context, "Fadlan fur app-ka Google Maps", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Nearest intersection (Current intersection where the driver is located / starting from)
    val currentOrNearestIntersection = remember(intersections, userLocation) {
        val uLat = userLocation?.first ?: 2.0371
        val uLng = userLocation?.second ?: 45.3283
        intersections.minByOrNull { item ->
            calcDistMeters(uLat, uLng, item.latitude, item.longitude)
        }
    }

    // Distance to current/nearest intersection
    val currentDistanceMeters = remember(currentOrNearestIntersection, userLocation) {
        if (currentOrNearestIntersection == null) null
        else {
            val uLat = userLocation?.first ?: 2.0371
            val uLng = userLocation?.second ?: 45.3283
            calcDistMeters(uLat, uLng, currentOrNearestIntersection.latitude, currentOrNearestIntersection.longitude)
        }
    }

    // Next upcoming intersection ahead (Smart detection: Bearing-driven direction of travel + corridor map)
    val nextUpcomingIntersection = remember(intersections, currentOrNearestIntersection, userLocation, driverBearing, driverSpeedKmh) {
        if (currentOrNearestIntersection == null) null
        else {
            val uLat = userLocation?.first ?: 2.0371
            val uLng = userLocation?.second ?: 45.3283
            val cName = currentOrNearestIntersection.name.lowercase(java.util.Locale.US)

            // 1. Major road connection networks for Mogadishu
            val connectedCandidates = when {
                cName.contains("km4") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("zoobe") || n.contains("shaqaalaha") || n.contains("taleex") || n.contains("dabka") || n.contains("tarbuunka")
                }
                cName.contains("zoobe") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("banaadir") || n.contains("km4") || n.contains("digfeer") || n.contains("taleex")
                }
                cName.contains("banaadir") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("tarbuunka") || n.contains("zoobe") || n.contains("black sea") || n.contains("bakaraha")
                }
                cName.contains("tarbuunka") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("bakaraha") || n.contains("bakaaraha") || n.contains("banaadir") || n.contains("km4")
                }
                cName.contains("shaqaalaha") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("dabka") || n.contains("km4") || n.contains("sayidka")
                }
                cName.contains("dabka") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("sayidka") || n.contains("shaqaalaha") || n.contains("bakaraha")
                }
                cName.contains("sayidka") -> intersections.filter {
                    val n = it.name.lowercase(java.util.Locale.US)
                    n.contains("dabka") || n.contains("taalada sayidka") || n.contains("maka al-mukarama")
                }
                else -> emptyList()
            }

            // 2. If moving with speed and valid bearing, pick connected candidate matching direction of travel
            val candidateByBearing = if (driverSpeedKmh > 3.0 && driverBearing != 0f && connectedCandidates.isNotEmpty()) {
                connectedCandidates.minByOrNull { item ->
                    val bearingToItem = calcBearing(uLat, uLng, item.latitude, item.longitude)
                    diffBearing(driverBearing, bearingToItem)
                }?.takeIf {
                    val bearingToItem = calcBearing(uLat, uLng, it.latitude, it.longitude)
                    diffBearing(driverBearing, bearingToItem) < 70f
                }
            } else null

            // 3. Fallback to default Mogadishu primary corridor progression
            val candidateByCorridor = when {
                cName.contains("km4") -> intersections.find { it.name.lowercase(java.util.Locale.US).contains("zoobe") }
                    ?: intersections.find { it.name.lowercase(java.util.Locale.US).contains("shaqaalaha") }
                cName.contains("zoobe") -> intersections.find { it.name.lowercase(java.util.Locale.US).contains("banaadir") }
                    ?: intersections.find { it.name.lowercase(java.util.Locale.US).contains("km4") }
                cName.contains("banaadir") -> intersections.find { it.name.lowercase(java.util.Locale.US).contains("tarbuunka") }
                    ?: intersections.find { it.name.lowercase(java.util.Locale.US).contains("zoobe") }
                cName.contains("tarbuunka") -> intersections.find { it.name.lowercase(java.util.Locale.US).contains("bakaraha") || it.name.lowercase(java.util.Locale.US).contains("bakaaraha") }
                    ?: intersections.find { it.name.lowercase(java.util.Locale.US).contains("banaadir") }
                cName.contains("shaqaalaha") -> intersections.find { it.name.lowercase(java.util.Locale.US).contains("dabka") }
                    ?: intersections.find { it.name.lowercase(java.util.Locale.US).contains("km4") }
                cName.contains("dabka") -> intersections.find { it.name.lowercase(java.util.Locale.US).contains("sayidka") }
                    ?: intersections.find { it.name.lowercase(java.util.Locale.US).contains("shaqaalaha") }
                else -> null
            }

            // 4. Fallback to closest neighboring intersection
            candidateByBearing ?: candidateByCorridor ?: intersections
                .filter { it.id != currentOrNearestIntersection.id }
                .minByOrNull { item ->
                    calcDistMeters(uLat, uLng, item.latitude, item.longitude)
                }
        }
    }

    // Distance to next upcoming intersection
    val nextDistanceMeters = remember(nextUpcomingIntersection, userLocation) {
        if (nextUpcomingIntersection == null) null
        else {
            val uLat = userLocation?.first ?: 2.0371
            val uLng = userLocation?.second ?: 45.3283
            calcDistMeters(uLat, uLng, nextUpcomingIntersection.latitude, nextUpcomingIntersection.longitude)
        }
    }

    val nextStatus = com.example.somalitraffic.data.model.TrafficStatus.fromString(nextUpcomingIntersection?.currentStatus)
    val isSevereAhead = nextStatus == com.example.somalitraffic.data.model.TrafficStatus.JAAM ||
            nextStatus == com.example.somalitraffic.data.model.TrafficStatus.XERAN

    // Both current and next intersections blink on the map so the driver sees both immediately
    val effectiveBlinkingIds = remember(selectedIntersection, currentOrNearestIntersection, nextUpcomingIntersection) {
        val set = mutableSetOf<String>()
        selectedIntersection?.let { set.add(it.id) }
        currentOrNearestIntersection?.let { set.add(it.id) }
        nextUpcomingIntersection?.let { set.add(it.id) }
        set
    }
    val effectiveBlinkingIntersectionId = selectedIntersection?.id ?: nextUpcomingIntersection?.id ?: currentOrNearestIntersection?.id

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            viewModel.setUserLocation(loc.latitude, loc.longitude)
                        }
                    }
            } catch (_: SecurityException) {}
        }
    }

    // ✅ CONTINUOUS HIGH-ACCURACY GPS TRACKING FOR DRIVER
    DisposableEffect(context) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val fused = LocationServices.getFusedLocationProviderClient(context)
        var callback: LocationCallback? = null

        if (hasFine || hasCoarse) {
            val req = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2_000L
            ).setMinUpdateIntervalMillis(1_000L)
                .setMinUpdateDistanceMeters(2f)
                .build()

            val cb = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val loc = result.lastLocation ?: return
                    viewModel.setUserLocation(loc.latitude, loc.longitude)
                    if (loc.hasSpeed()) {
                        driverSpeedKmh = loc.speed * 3.6
                    }
                    if (loc.hasBearing()) {
                        driverBearing = loc.bearing
                    }
                }
            }
            callback = cb
            try {
                fused.requestLocationUpdates(req, cb, android.os.Looper.getMainLooper())
            } catch (_: SecurityException) {}
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }

        onDispose {
            callback?.let {
                try {
                    fused.removeLocationUpdates(it)
                } catch (_: Exception) {}
            }
        }
    }

    // ✅ EARLY WARNING AUDIO CHIME: Beeps once when driver is within 950m of a JAMMED or CLOSED road ahead
    LaunchedEffect(nextUpcomingIntersection?.id, nextStatus, nextDistanceMeters, isAudioAlertEnabled) {
        if (isAudioAlertEnabled && isSevereAhead && nextUpcomingIntersection != null) {
            val dist = nextDistanceMeters ?: Double.MAX_VALUE
            if (dist <= 950.0 && lastAlertedIntersectionId != nextUpcomingIntersection.id) {
                lastAlertedIntersectionId = nextUpcomingIntersection.id
                ArrivalSoundManager.playSingleHornBeep(context)
            }
        }
    }

    // ✅ FAST 15-SECOND AUTO-REFRESH: Automatically pulls latest reporter updates from Supabase
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(15_000L)
            viewModel.refreshTrafficData()
        }
    }

    Scaffold(
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "live_traffic",
                onNavigate = onNavigate,
                isDriver = isDriver
            )
        },
        floatingActionButton = {
            // Only display floating buttons when ROAD_CLOSURES bottom sheet is NOT taking up the bottom space
            if (mapMode != TrafficMapMode.ROAD_CLOSURES) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Report Traffic Floating Button
                    ExtendedFloatingActionButton(
                        onClick = { showReportDialog = true },
                        containerColor = GoBajaajAmber,
                        contentColor = BrandBlack,
                        shape = RoundedCornerShape(16.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        icon = {
                            Icon(
                                Icons.Default.AddAlert,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        text = {
                            Text(
                                "Warbixi Saxmad",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp
                            )
                        }
                    )

                    // My Location FAB
                    FloatingActionButton(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted) {
                                try {
                                    val fused = LocationServices.getFusedLocationProviderClient(context)
                                    fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { loc ->
                                            if (loc != null) {
                                                viewModel.setUserLocation(loc.latitude, loc.longitude)
                                            }
                                        }
                                } catch (_: SecurityException) {}
                            } else {
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        },
                        containerColor = Color.White,
                        contentColor = GoBajaajNavy,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Location",
                            tint = GoBajaajNavy,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
                .padding(padding)
        ) {
            // Main Scrollable Traffic Screen Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Waddooyinka Xiran Action Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (mapMode == TrafficMapMode.ROAD_CLOSURES) GoBajaajNavy else Color.White,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (mapMode == TrafficMapMode.ROAD_CLOSURES) GoBajaajAmber else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mapMode = if (mapMode == TrafficMapMode.ROAD_CLOSURES) TrafficMapMode.TRAFFIC else TrafficMapMode.ROAD_CLOSURES
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Construction,
                            contentDescription = null,
                            tint = if (mapMode == TrafficMapMode.ROAD_CLOSURES) GoBajaajAmber else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (mapMode == TrafficMapMode.ROAD_CLOSURES) "Waddooyinka Xiran (Furantahay)" else "Waddooyinka Xiran",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (mapMode == TrafficMapMode.ROAD_CLOSURES) Color.White else BrandBlack
                        )
                        if (roadClosures.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDC2626)
                            ) {
                                Text(
                                    text = "${roadClosures.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                // 1. AUTOMATIC DRIVER CORRIDOR BANNER: Current Intersection & Next Upcoming Intersection (Blinking Live)
                if (currentOrNearestIntersection != null) {
                    val currStatus = com.example.somalitraffic.data.model.TrafficStatus.fromString(currentOrNearestIntersection.currentStatus)
                    val currDistLabel = if (currentDistanceMeters != null) {
                        if (currentDistanceMeters < 1000) "${currentDistanceMeters.toInt()}m" else String.format(java.util.Locale.US, "%.1fkm", currentDistanceMeters / 1000.0)
                    } else "Halkan"

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Header: Mode indicator & GPS status badge with modern styling
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                androidx.compose.ui.graphics.Brush.linearGradient(
                                                    listOf(Color(0xFF0F172A), Color(0xFF1E293B))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Navigation,
                                            contentDescription = null,
                                            tint = GoBajaajAmber,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "TILMAAMAHA TOOSKA AH (LIVE)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = GoBajaajNavy,
                                            letterSpacing = 0.4.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Hubi Saxmadda Jidadka",
                                            fontSize = 9.5.sp,
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (driverSpeedKmh > 2.0) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = GoBajaajNavy
                                        ) {
                                            Text(
                                                text = "⚡ ${driverSpeedKmh.toInt()} km/h",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GoBajaajAmber,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(5.dp))
                                    }
                                    // Sound alert mute/unmute toggle button
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isAudioAlertEnabled) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isAudioAlertEnabled) Color(0xFFFDE68A) else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .clickable {
                                                isAudioAlertEnabled = !isAudioAlertEnabled
                                                Toast.makeText(
                                                    context,
                                                    if (isAudioAlertEnabled) "Digniinta codka waa shidan tahay 🔔" else "Digniinta codka waa damisan tahay 🔕",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isAudioAlertEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                                contentDescription = "Digniinta Codka",
                                                tint = if (isAudioAlertEnabled) Color(0xFFD97706) else Color(0xFF64748B),
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF16A34A))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "GPS TOOS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF15803D)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // CARD 1: Meesha aad hadda joogto ama ka socoto (ORIGIN / FROM)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF0F9FF), // Soft sky blue tint
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBAE6FD)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        viewModel.selectIntersection(currentOrNearestIntersection)
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Row 1: Bright Blue "KA SOCDAA" (FROM / ORIGIN) Badge & Distance
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF0284C7)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.TripOrigin,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "KA SOCDAA",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White,
                                                        letterSpacing = 0.4.sp
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = "Goobta aad hadda joogto",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF0369A1),
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Distance tag
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBAE6FD))
                                        ) {
                                            Text(
                                                text = currDistLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0369A1),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Big Road Name and Eye-Catching Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            BlinkingTrafficIndicator(status = currStatus, size = 30.dp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = currentOrNearestIntersection.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Waddada bilowga ah",
                                                    fontSize = 9.5.sp,
                                                    color = Color(0xFF64748B),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Refined Modern Status Badge
                                        ModernTrafficBadge(status = currStatus, label = currentOrNearestIntersection.currentStatus)
                                    }
                                }
                            }

                            // Visual Route Directional Banner: Clear visual cue of traveling from A -> B
                            if (nextUpcomingIntersection != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color(0xFFF1F5F9),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "KU SOCOTA • DHINACA WADDADA",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF1E293B),
                                                letterSpacing = 0.3.sp
                                            )
                                        }
                                    }
                                }

                                val nextStatus = com.example.somalitraffic.data.model.TrafficStatus.fromString(nextUpcomingIntersection.currentStatus)
                                val nextDistLabel = if (nextDistanceMeters != null) {
                                    if (nextDistanceMeters < 1000) "${nextDistanceMeters.toInt()}m" else String.format(java.util.Locale.US, "%.1fkm", nextDistanceMeters / 1000.0)
                                } else "Ku xigta"

val isSevere = nextStatus == com.example.somalitraffic.data.model.TrafficStatus.JAAM ||
        nextStatus == com.example.somalitraffic.data.model.TrafficStatus.XERAN

                                val nextCardBg = if (isSevere) Color(0xFFFFF1F2) else Color(0xFFF0FDF4)
                                val nextBorderColor = if (isSevere) Color(0xFFFDA4AF) else Color(0xFF86EFAC)

                                // CARD 2: Meesha aad u socoto (DESTINATION / TO / AHEAD)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = nextCardBg,
                                    border = androidx.compose.foundation.BorderStroke(1.5.dp, nextBorderColor),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable {
                                            viewModel.selectIntersection(nextUpcomingIntersection)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // Row 1: Orange/Amber or Green "U SOCODAA" (DESTINATION / TO) Badge & Distance
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSevere) Color(0xFFE11D48) else Color(0xFF16A34A)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Place,
                                                            contentDescription = null,
                                                            tint = Color.White,
                                                            modifier = Modifier.size(11.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "U SOCODAA",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White,
                                                            letterSpacing = 0.4.sp
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(
                                                    text = "Waddada aad u socoto / ku xigta",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSevere) Color(0xFF9F1239) else Color(0xFF166534),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Distance tag with prominent color
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSevere) Color(0xFFFECDD3) else Color(0xFFBBF7D0))
                                            ) {
                                                Text(
                                                    text = nextDistLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isSevere) Color(0xFFBE123C) else Color(0xFF15803D),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // Big Road Name + Blinking Indicator + Distinct Status Badge
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                BlinkingTrafficIndicator(status = nextStatus, size = 30.dp)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = nextUpcomingIntersection.name,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "Waddada bartilmaameedka ah",
                                                        fontSize = 9.5.sp,
                                                        color = if (isSevere) Color(0xFFBE123C) else Color(0xFF15803D),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Refined Modern Status Badge for Next Road
                                            ModernTrafficBadge(status = nextStatus, label = nextUpcomingIntersection.currentStatus)
                                        }

                                        // Flashing Early Warning Banner if Next Road is JAAM / XERAN
                                        if (isSevere) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFFFFE4E6),
                                                border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFF43F5E)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = "🚨", fontSize = 15.sp)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "DIGNIIN: ${nextUpcomingIntersection.name.uppercase()} WAA ${nextUpcomingIntersection.currentStatus.uppercase()}!",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF9F1239)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(3.dp))
                                                    if (!nextUpcomingIntersection.alternativeRoute.isNullOrBlank()) {
                                                        Text(
                                                            text = "💡 Waddo kale: ${nextUpcomingIntersection.alternativeRoute}",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF881337)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "Fadlan ka leexo inta aadan gaarin ($nextDistLabel kaa horeeya)",
                                                            fontSize = 9.5.sp,
                                                            color = Color(0xFF9F1239),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Reporter Verification Badge & Live Status Timestamp
                                        if (nextUpcomingIntersection.verifiedByReporter || !nextUpcomingIntersection.latestVerifiedReport.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFECFDF5),
                                                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFA7F3D0))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "✓ XAQIIJIYAY WARIYE (LIVE)",
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color(0xFF047857),
                                                            letterSpacing = 0.3.sp
                                                        )
                                                    }
                                                }
                                                if (!nextUpcomingIntersection.latestVerifiedReport.isNullOrBlank()) {
                                                    Text(
                                                        text = nextUpcomingIntersection.latestVerifiedReport ?: "",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF64748B),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // 1-Tap Google Maps Navigation to this Intersection
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                openGoogleMapsForIntersection(
                                                    nextUpcomingIntersection.latitude,
                                                    nextUpcomingIntersection.longitude,
                                                    nextUpcomingIntersection.name
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSevere) Color(0xFFE11D48) else GoBajaajNavy,
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Directions,
                                                contentDescription = null,
                                                tint = GoBajaajAmber,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "🗺️ U Fur Google Maps (${nextUpcomingIntersection.name})",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. SEARCH BAR FOR INTERSECTIONS (With live autocomplete & blinking preview)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Raadi",
                                    tint = GoBajaajNavy,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.foundation.text.BasicTextField(
                                value = localSearchText,
                                onValueChange = { localSearchText = it },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = BrandBlack
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (localSearchText.isEmpty()) {
                                            Text(
                                                text = "Raadi isgoys (KM4, Zoobe, Banaadir...)",
                                                fontSize = 13.sp,
                                                color = TextMutedDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (localSearchText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        localSearchText = ""
                                        viewModel.selectIntersection(null)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Tirtir", tint = TextMutedDark, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // Search Results Dropdown List
                        if (searchResults.isNotEmpty()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                                searchResults.forEach { result ->
                                    val item = result.intersection
                                    val sStatus = com.example.somalitraffic.data.model.TrafficStatus.fromString(item.currentStatus)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.selectIntersection(item)
                                                localSearchText = item.name
                                            }
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            BlinkingTrafficIndicator(status = sStatus)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = BrandBlack
                                                )
                                                if (result.formattedDistance != null) {
                                                    Text(
                                                        text = "${item.district ?: item.city} • ${result.formattedDistance}",
                                                        fontSize = 11.sp,
                                                        color = TextMutedDark
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = sStatus.color.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = item.currentStatus,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = sStatus.color,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. ALL MOGADISHU INTERSECTIONS LIST (Live Traffic status lights: Furan, Dhex, Mashquul, Xiran)
                if (mapMode == TrafficMapMode.TRAFFIC) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ISGOYSYADA MUQDISHO (LIVE TRAFFIC)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "${intersections.size} Isgoys",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoBajaajNavy
                            )
                        }

                        intersections.forEach { item ->
                            val sStatus = com.example.somalitraffic.data.model.TrafficStatus.fromString(item.currentStatus)
                            val isSelected = selectedIntersection?.id == item.id
                            
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 3.dp else 1.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectIntersection(item)
                                        onIntersectionClick(item.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        BlinkingTrafficIndicator(status = sStatus, size = 26.dp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = item.district ?: (item.roadNames.ifEmpty { "Muqdisho" }),
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    ModernTrafficBadge(status = sStatus, label = item.currentStatus)
                                }
                            }
                        }
                    }
                }
            }

            // Road Closures Overlay Sheet if mode is ROAD_CLOSURES
            if (mapMode == TrafficMapMode.ROAD_CLOSURES) {
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Waddooyinka Xiran (Road Closures)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BrandBlack
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GoBajaajAmber,
                                    modifier = Modifier.clickable { showReportDialog = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.AddAlert, contentDescription = null, tint = BrandBlack, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Warbixi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlack)
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(onClick = { mapMode = TrafficMapMode.TRAFFIC }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xir")
                                }
                            }
                        }

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(roadClosures) { closure ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = BrandLightGray,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(TrafficClosedBlack),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(closure.roadName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlack)
                                            Text("Sababta: ${closure.reason}", fontSize = 11.sp, color = TextMutedDark)
                                            Text("Waqtiga: ${closure.timeWindow}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TrafficHeavyRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }



            // ==============================================================
            // REPORT TRAFFIC DIALOG (Matching Screen 3 of Row 3)
            // ==============================================================
            if (showReportDialog) {
                AlertDialog(
                    onDismissRequest = { showReportDialog = false },
                    title = {
                        Text(
                            text = "Warbixi Saxmad / Dhacdo",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandBlack
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Dooro nooca dhacdada:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMutedDark
                            )

                            // Incident Chips
                            val types = listOf("Traffic Jam", "Road Closed", "Accident", "Blocked Road", "Flood", "Other")
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(types) { t ->
                                    FilterChip(
                                        selected = reportType == t,
                                        onClick = { reportType = t },
                                        label = { Text(t, fontSize = 11.sp) }
                                    )
                                }
                            }

                            val reportFieldColors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrandBlack,
                                unfocusedTextColor = BrandBlack,
                                focusedContainerColor = Color(0xFFF8FAFC),
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                focusedBorderColor = GoBajaajBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = GoBajaajBlue,
                                unfocusedLabelColor = Color(0xFF64748B),
                                cursorColor = GoBajaajBlue
                            )
                            val reportTextStyle = androidx.compose.ui.text.TextStyle(
                                color = BrandBlack,
                                fontSize = 14.sp
                            )

                            OutlinedTextField(
                                value = reportLocation,
                                onValueChange = { reportLocation = it },
                                label = { Text("Goobta (Location)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = reportTextStyle,
                                colors = reportFieldColors
                            )

                            OutlinedTextField(
                                value = reportDescription,
                                onValueChange = { reportDescription = it },
                                label = { Text("Faahfaahin kooban") },
                                placeholder = { Text("Tusaale: Baabuur ciladoobay ayaa xiray jidka...") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = reportTextStyle,
                                colors = reportFieldColors
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Heerka saxmadda:", fontSize = 12.sp, color = TextMutedDark)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("Low", "Medium", "High").forEach { sev ->
                                        FilterChip(
                                            selected = reportSeverity == sev,
                                            onClick = { reportSeverity = sev },
                                            label = { Text(sev, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Mahadsanid! Warbixintaada saxmadda waa la faafiyay.", Toast.LENGTH_LONG).show()
                                showReportDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoBajaajAmber)
                        ) {
                            Text("Submit Report", color = BrandBlack, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showReportDialog = false }) {
                            Text("Ka noqo", color = TextMutedDark)
                        }
                    }
                )
            }

            // Lock Barrier Modal if Driver Pass is Expired
            if (isDriver && !isPassActive) {
                AlertDialog(
                    onDismissRequest = { onNavigate("home") },
                    icon = {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Adeegga Maalinlaha ah wuu Dhammaaday ($0.25)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    text = {
                        Text(
                            text = "Driver Pass-kaaga maanta wuu dhacay. Fadlan dib ugu shubo ($0.25) si aad u hesho xogta saxmadda waddooyinka.",
                            fontSize = 13.5.sp,
                            color = Color.DarkGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { onNavigate("driver_pass") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Bixi $0.25 Hadda", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { onNavigate("home") }) {
                            Text("Ka Noqo (Home)", color = Color.Gray)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SegmentTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconDrawableRes: Int? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GoBajaajNavy else Color.Transparent,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (iconDrawableRes != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = iconDrawableRes),
                    contentDescription = null,
                    tint = if (isSelected) GoBajaajAmber else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) GoBajaajAmber else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                color = if (isSelected) Color.White else Color(0xFF475569)
            )
        }
    }
}



@Composable
private fun ModernTrafficBadge(
    status: com.example.somalitraffic.data.model.TrafficStatus,
    label: String,
    modifier: Modifier = Modifier
) {
    val (bgGradient, borderColor, textColor, icon) = when (status) {
        com.example.somalitraffic.data.model.TrafficStatus.FURAN -> Quadruple(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFDCFCE7), Color(0xFFBBF7D0))),
            Color(0xFF86EFAC),
            Color(0xFF15803D),
            Icons.Default.CheckCircle
        )
        com.example.somalitraffic.data.model.TrafficStatus.NORMAL -> Quadruple(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFFEF9C3), Color(0xFFFEF08A))),
            Color(0xFFFDE047),
            Color(0xFF854D0E),
            Icons.Default.Warning
        )
        com.example.somalitraffic.data.model.TrafficStatus.JAAM,
        com.example.somalitraffic.data.model.TrafficStatus.XERAN -> Quadruple(
            androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626))),
            Color(0xFFB91C1C),
            Color.White,
            Icons.Default.Dangerous
        )
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .background(bgGradient)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = textColor,
                    letterSpacing = 0.2.sp
                )
            }
        }
    }
}

// Data helper for ModernTrafficBadge
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun BlinkingTrafficIndicator(
    status: com.example.somalitraffic.data.model.TrafficStatus,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BlinkTransition")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkAlpha"
    )
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BlinkScale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Blinking outer halo glow
        Box(
            modifier = Modifier
                .size(size * scaleAnim)
                .clip(CircleShape)
                .background(status.color.copy(alpha = alphaAnim * 0.40f))
        )
        // Mid ring
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(status.color.copy(alpha = 0.25f))
        )
        // Solid high-visibility core
        Box(
            modifier = Modifier
                .size(size * 0.52f)
                .clip(CircleShape)
                .background(status.color)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

