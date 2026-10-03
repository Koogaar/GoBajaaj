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
import com.example.somalitraffic.ui.components.SomaliBottomNavigation
import com.example.somalitraffic.ui.components.SomaliInteractiveMap
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

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
                onNavigate = onNavigate,
                isDriver = isDriver
            )
        },
        containerColor = GoBajaajCardBg
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
                                    text = "Gaadiid • Waddooyin • Ammaan",
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

            Spacer(modifier = Modifier.height(18.dp))

            // ==============================================================
            // 3 CORE SERVICE BUTTONS (Matching Screen 3: Ride, Traffic, Lost Phone)
            // ==============================================================
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                val subscription by viewModel.subscription.collectAsState()
                val isPassActive = viewModel.isSubscriptionActive(subscription)

                if (isDriver && subscription != null && !isPassActive) {
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
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Adeegga Maalinlaha: WAA HAWLGELIYAY ($0.25) • Wuxuu ku egyahay 05:00 AM",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                Text(
                    text = "Adeegyada GoBajaaj",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = BrandBlack
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (isDriver) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. Ride (Blue Card) - Dalabaadka Bajaajta
                        ServiceQuickCard(
                            title = "Bajaaj",
                            somaliDesc = "Dalabaadka & Safarrada",
                            iconDrawableRes = com.example.R.drawable.ic_bajaaj_icon,
                            bgColor = GoBajaajBlue,
                            onClick = { 
                                if (isPassActive) onNavigate("ride") else onNavigate("driver_pass")
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
                } else {
                    // For Customer / Rakaab: Ride Card (Dalbo Bajaaj Hadda)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = GoBajaajBlue),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("ride") }
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dalbo Bajaaj Hadda",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Dooro gaadiid, qiimo gooso, si nabad ah ku gaar",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoBajaajYellow
                            ) {
                                Text(
                                    text = "Dalbo",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoBajaajNavy,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
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
