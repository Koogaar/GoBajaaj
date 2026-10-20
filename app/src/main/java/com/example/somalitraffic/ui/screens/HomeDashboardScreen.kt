package com.example.somalitraffic.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.DriverSecondaryTabBar
import com.example.somalitraffic.ui.components.SomaliBottomNavigation
import com.example.somalitraffic.ui.components.SomaliInteractiveMap
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val intersections by viewModel.intersections.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val isDriver = currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER

    // Secondary tab state — Driver only (Shaqada=0, Khariiradda=1, Driver Pass=2, Dakhliga=3)
    var selectedDriverTab by remember { mutableStateOf(0) }

    var currentAddressText by remember { mutableStateOf("Mogadishu, KM4") }

    // Nearest intersection calculation based on userLocation
    val nearestIntersection = remember(intersections, userLocation) {
        val uLat = userLocation?.first ?: 2.0371
        val uLng = userLocation?.second ?: 45.3283
        intersections.minByOrNull { item ->
            com.example.somalitraffic.data.repository.IntersectionSearchEngine.calculateDistanceInMeters(
                uLat, uLng, item.latitude, item.longitude
            )
        }
    }

    val nearestDistanceMeters = remember(nearestIntersection, userLocation) {
        if (nearestIntersection == null) null
        else {
            val uLat = userLocation?.first ?: 2.0371
            val uLng = userLocation?.second ?: 45.3283
            com.example.somalitraffic.data.repository.IntersectionSearchEngine.calculateDistanceInMeters(
                uLat, uLng, nearestIntersection.latitude, nearestIntersection.longitude
            )
        }
    }

    // Request GPS permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            try {
                val fused = LocationServices.getFusedLocationProviderClient(context)
                fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            viewModel.setUserLocation(loc.latitude, loc.longitude)
                            currentAddressText = "Mogadishu (${String.format("%.4f", loc.latitude)}, ${String.format("%.4f", loc.longitude)})"
                        }
                    }
            } catch (_: SecurityException) {}
        }
    }

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    Scaffold(
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "home",
                onNavigate = { route ->
                    // Khariiradda tab -> live_traffic, Dalbo tab -> ride, Akoonka -> profile
                    onNavigate(route)
                },
                isDriver = isDriver
            )
        },
        containerColor = Color(0xFF0F131C) // surface dark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ==============================================================
            // TOP HEADER (Matching Screen 3: "GoBajaaj", Avatar, Greeting)
            // ==============================================================
            Surface(
                color = GoBajaajNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_gobajaaj_logo),
                                contentDescription = "GoBajaaj Logo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "GoBajaaj",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Ka Badbaad Saxmada Waddooyinka",
                                    fontSize = 11.sp,
                                    color = GoBajaajYellow,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Greeting
                    val firstName = currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Ahmed"
                    Text(
                        text = "Salaan, $firstName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Safar nabad ah",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // DriverSecondaryTabBar — Darwalka kaliya
            if (isDriver) {
                DriverSecondaryTabBar(
                    selectedTab = selectedDriverTab,
                    onTabSelected = { tab ->
                        selectedDriverTab = tab
                        when (tab) {
                            1 -> onNavigate("live_traffic") // Khariiradda
                            2 -> onNavigate("driver_pass")  // Driver Pass
                            // 0 = Shaqada (current), 3 = Dakhliga (future)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ==============================================================
            // 3 CORE SERVICE BUTTONS (Matching Screen 3: Ride, Traffic, Lost Phone)
            // ==============================================================
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                val subscription by viewModel.subscription.collectAsState()
                val isPassActive = viewModel.isSubscriptionActive(subscription)

                if (isDriver && !isPassActive) {
                    Surface(
                        color = Color(0xFFFEF2F2),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("driver_pass") }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Adeegga Maalinlaha ah ($0.25)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color(0xFF991B1B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Waa kaa dhammaaday adeegga maalinlaha ah ee GoBajaaj, fadlan dib ugu shubo ($0.25) si aad shaqada u wadato.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E293B),
                                    lineHeight = 16.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onNavigate("driver_pass") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text(
                                    text = "Dib Ugu Shubo",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                } else if (isDriver && isPassActive) {
                    val expiryFormatted = remember(subscription) {
                        formatPassExpiry(subscription)
                    }
                    Surface(
                        color = Color(0xFF10281E),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("driver_pass") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Now Active • $0.25 (24 Saac)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF4EDEA3)
                                    )
                                    Text(
                                        text = expiryFormatted,
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFDFE2EE).copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Faahfaahin",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4EDEA3),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // ==============================================================
                // CURRENT & DESTINATION ROAD TRACKER (Waddada aad taagan tahay & Mida aad u socoto)
                // ==============================================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF181C24)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("live_traffic") }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Waddooyinka & Jidkaaga",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDFE2EE).copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Khariiradda ➔",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4EDEA3)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Indicator dots & connecting line
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4EDEA3))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(28.dp)
                                        .background(Color.White.copy(alpha = 0.15f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE50914))
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                // 1. Waddada aad taagan tahay
                                Text(
                                    text = "Waddada aad taagan tahay (Hadda):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDFE2EE).copy(alpha = 0.6f)
                                )
                                Text(
                                    text = nearestIntersection?.name ?: "KM4 Roundabout (Mogadishu)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // 2. Mida aad u socoto / Isgoyska xiga
                                val nextIntersection = intersections.firstOrNull { it.id != nearestIntersection?.id }
                                Text(
                                    text = "Mida aad u socoto / Isgoyska xiga:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFDFE2EE).copy(alpha = 0.6f)
                                )
                                Text(
                                    text = nextIntersection?.let { "${it.name} (${if (it.currentStatus.isBlank() || it.currentStatus.equals("hadda", ignoreCase = true)) "Furan" else it.currentStatus})" } ?: "Dooro waddada aad u socoto",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDFE2EE),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // The two circled service cards placed directly below the road tracker!
                Text(
                    text = "Adeegyada GoBajaaj",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Digniinaha (Blue Card) - Digniinaha & Xirnaanshaha
                    ServiceQuickCard(
                        title = "Digniinaha",
                        somaliDesc = "Xirnaanshaha & Shilalka",
                        icon = Icons.Default.NotificationsActive,
                        bgColor = GoBajaajBlue,
                        onClick = { 
                            if (isPassActive) onNavigate("traffic_alerts") else onNavigate("driver_pass")
                        },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. Waddooyinka (Amber/Orange Card) - Xogta Waddooyinka
                    ServiceQuickCard(
                        title = "Waddooyinka",
                        somaliDesc = "Saxmadda & Isgoysyada",
                        icon = Icons.Default.Traffic,
                        bgColor = GoBajaajAmber,
                        onClick = { 
                            if (isPassActive) onNavigate("live_traffic") else onNavigate("driver_pass")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ==============================================================
                // QUICK HIGHLIGHTS / ROAD CLOSURES & TRAFFIC INTEL
                // ==============================================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Isgoysyada Muqdisho",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandBlack,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Arag Dhammaan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoBajaajBlue,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.clickable { onNavigate("live_traffic") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (intersections.isEmpty()) {
                            Text(
                                text = "Waddooyin lama diwaan gelin. Sug ilaa laga diwaan gelinayo isgoysyada.",
                                fontSize = 13.sp,
                                color = TextMutedDark
                            )
                        } else {
                            val displayIntersections = intersections
                                .distinctBy { it.name.trim().lowercase().replace("e", "a") }
                                .take(3)

                            displayIntersections.forEach { item ->
                                val itemStatus = com.example.somalitraffic.data.model.TrafficStatus.fromString(item.currentStatus)
                                val statusText = if (item.currentStatus.equals("hadda", ignoreCase = true) || item.currentStatus.isBlank()) {
                                    "Furan"
                                } else {
                                    itemStatus.somaliName
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigate("traffic_details/${item.id}") }
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(itemStatus.color)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = BrandBlack,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = statusText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = itemStatus.color,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ServiceQuickCard(
    title: String,
    somaliDesc: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconDrawableRes: Int? = null,
    bgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable { onClick() }
            .testTag("service_card_$title")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (iconDrawableRes != null) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = iconDrawableRes),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = somaliDesc,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}

private fun formatPassExpiry(sub: com.example.somalitraffic.data.model.Subscription?): String {
    if (sub == null) return "24 Saac firfircoon"
    val endStr = sub.endDate?.trim()
    if (endStr.isNullOrBlank()) return "Wuxuu shaqaynayaa 24 Saac"
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
        if (parsed != null) {
            val outDf = SimpleDateFormat("hh:mm a (dd/MM)", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Africa/Mogadishu")
            }
            "Wuxuu ku egyahay: ${outDf.format(parsed)}"
        } else {
            "Wuxuu shaqaynayaa 24 Saac"
        }
    } catch (_: Exception) {
        "Wuxuu shaqaynayaa 24 Saac"
    }
}
