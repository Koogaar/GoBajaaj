package com.example.somalitraffic.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliBottomNavigation
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
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
    val subscription by viewModel.subscription.collectAsState()
    val isDriver = currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER
    val isPassActive = viewModel.isSubscriptionActive(subscription)

    var searchQuery by remember { mutableStateOf("") }
    var isDataSaverOn by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("SO") } // "SO" or "EN"

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

    // Pulse animation for Live Grid indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "home",
                onNavigate = onNavigate,
                isDriver = isDriver
            )
        },
        containerColor = Color(0xFFF8FAFC) // Light, daylight-friendly background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ==============================================================
            // 1. TOP APP BAR (Kinetic Horizon Header: SHIRI Super App Logo)
            // ==============================================================
            Surface(
                color = Color.White,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo + Brand Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onNavigate("home") }
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.shiri_logo),
                            contentDescription = "SHIRI Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "SHIRI",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ShiriCrimson,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Mogadishu",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Location Pill + Language Toggle + Profile Avatar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Location Pill
                        Surface(
                            color = Color(0xFFEFF4FF),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { onNavigate("ride") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = ShiriCrimson,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = nearestIntersection?.name?.take(14) ?: "Muqdisho",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShiriDarkSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Language Toggle
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable {
                                selectedLanguage = if (selectedLanguage == "SO") "EN" else "SO"
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SO",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLanguage == "SO") FontWeight.Black else FontWeight.Normal,
                                    color = if (selectedLanguage == "SO") ShiriCrimson else Color(0xFF64748B)
                                )
                                Text(text = " | ", fontSize = 11.sp, color = Color(0xFFCBD5E1))
                                Text(
                                    text = "EN",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedLanguage == "EN") FontWeight.Black else FontWeight.Normal,
                                    color = if (selectedLanguage == "EN") ShiriCrimson else Color(0xFF64748B)
                                )
                            }
                        }

                        // Notification Bell (Matching Phone 1 in Poster)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF2F2))
                                .clickable { onNavigate("traffic_alerts") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = ShiriCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Profile Thumbnail
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0))
                                .clickable { 
                                    if (currentUser != null) onNavigate("profile_settings") 
                                    else onNavigate("login") 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (!currentUser?.avatarUrl.isNullOrBlank()) {
                                coil.compose.AsyncImage(
                                    model = currentUser!!.avatarUrl,
                                    contentDescription = "Profile",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                                )
                            } else {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Profile",
                                    tint = ShiriDarkSlate,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ==============================================================
                // 1. HERO WELCOME BANNER (PHONE 1 MOCKUP DESIGN)
                // ==============================================================
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFFEFF6FF),
                                        Color(0xFFFFFFFF),
                                        Color(0xFFFEF2F2)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = ShiriCrimson,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SUPER APP",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    text = "Smart City • Smart Choice",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Welcome to SHIRI",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Everything, One Journey.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                // ==============================================================
                // 2. THE 4 PROMINENT HUBS (2x2 ROLES GRID)
                // ==============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. DALBO BAJAAJ (Red - Rakaab)
                    ShiriHubCard(
                        title = "DALBO BAJAAJ",
                        subtitle = "Rakaab • Safar Degdeg",
                        statusTag = "Bilaash ($0)",
                        badgeText = "SHIRI RIDE",
                        icon = Icons.Default.TwoWheeler,
                        gradientColors = listOf(Color(0xFFE50914), Color(0xFFDC2626), Color(0xFFB91C1C)),
                        onClick = { onNavigate("ride") },
                        modifier = Modifier.weight(1f)
                    )

                    // 2. DARAWAL BAJAAJ (Orange - Driver Pass)
                    ShiriHubCard(
                        title = "DARAWAL BAJAAJ",
                        subtitle = "Driver Pass • Shaqo",
                        statusTag = "$0.25 Maalintii",
                        badgeText = "BAJAAJLE",
                        icon = Icons.Default.SportsMotorsports,
                        gradientColors = listOf(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFC2410C)),
                        onClick = { onNavigate("driver_pass") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 3. SHIRI DELIVERY (Blue - Mooto Courier)
                    ShiriHubCard(
                        title = "SHIRI DELIVERY",
                        subtitle = "Dirso Alaab • Baakado",
                        statusTag = "Mooto Express",
                        badgeText = "COURIER",
                        icon = Icons.Default.LocalShipping,
                        gradientColors = listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF075985)),
                        onClick = { onNavigate("delivery") },
                        modifier = Modifier.weight(1f)
                    )

                    // 4. SHIRI EATS & MARKET (Green - Ganacsade)
                    ShiriHubCard(
                        title = "SHIRI EATS & MARKET",
                        subtitle = "Dalbo Cunto & Wax Iibso",
                        statusTag = "Suuqa Bakaaro",
                        badgeText = "GANACSADE",
                        icon = Icons.Default.ShoppingBag,
                        gradientColors = listOf(Color(0xFF16A34A), Color(0xFF15803D), Color(0xFF166534)),
                        onClick = { onNavigate("eats_and_market") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ==============================================================
                // 3. SHIRI TRAFFIC (FULL-WIDTH PURPLE CARD FROM POSTER)
                // ==============================================================
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("live_traffic") }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED), Color(0xFF6D28D9))
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.25f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Traffic,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "SHIRI TRAFFIC",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "Live",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF7C3AED),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "La soco Jaamka & Khariidadda Tooska ah",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.92f)
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                // ==============================================================
                // 2. CONNECTIVITY & LOW-DATA / AMBIENT STATUS BAR
                // ==============================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ShiriEmerald.copy(alpha = pulseAlpha))
                        )
                        Text(
                            text = "MOGADISHU GRID LIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ShiriDarkSlate,
                            letterSpacing = 0.5.sp
                        )
                        Surface(
                            color = Color(0xFFE2E8F0),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "4G+",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Khad Yar / Data Saver Toggle Pill
                    Surface(
                        color = if (isDataSaverOn) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (isDataSaverOn) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable {
                            isDataSaverOn = !isDataSaverOn
                            Toast.makeText(
                                context,
                                if (isDataSaverOn) "⚡ Khad Yar (Data Saver) waa shaqeynayaa" else "Khadka caadiga ah",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isDataSaverOn) Color(0xFFB45309) else Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Khad Yar: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDataSaverOn) Color(0xFF92400E) else Color(0xFF64748B)
                            )
                            Text(
                                text = if (isDataSaverOn) "ON" else "OFF",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDataSaverOn) ShiriCrimson else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // ==============================================================
                // 3. FAST PAY MICRO-WALLETS (EVC+, eDahab, Sahal)
                // ==============================================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = ShiriCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Haraaga Jeebka • Fast Pay",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShiriDarkSlate
                                )
                            }
                            Text(
                                text = "Hormuud • Somtel • Golis",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // EVC+
                            FastPayWalletCard(
                                provider = "EVC+",
                                code = "*712#",
                                balance = "$45.80",
                                color = EvcPlusGreen,
                                bgContainer = Color(0xFFECFDF5),
                                modifier = Modifier.weight(1f)
                            )
                            // eDahab
                            FastPayWalletCard(
                                provider = "eDahab",
                                code = "*789#",
                                balance = "$12.50",
                                color = EdahabGold,
                                bgContainer = Color(0xFFFFFBEB),
                                modifier = Modifier.weight(1f)
                            )
                            // SAHAL
                            FastPayWalletCard(
                                provider = "SAHAL",
                                code = "*888#",
                                balance = "Ku Xir",
                                color = SahalBlue,
                                bgContainer = Color(0xFFF0F9FF),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ==============================================================
                // 4. DRIVER PASS CARD (Darawalka Kaliya - Driver Daily Pass $0.25)
                // ==============================================================
                if (isDriver) {
                    if (!isPassActive) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("driver_pass") }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(ShiriCrimson),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Baaska Maalinlaha ah ($0.25)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = "Waa kaa dhammaaday adeegga maanta, dib ugu shubo si aad u shaqeyso.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { onNavigate("driver_pass") },
                                    colors = ButtonDefaults.buttonColors(containerColor = ShiriCrimson),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("Ku Shubo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    } else {
                        val expiryFormatted = remember(subscription) {
                            formatPassExpiry(subscription)
                        }
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ShiriEmerald.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate("driver_pass") }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(ShiriEmerald)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Active • $0.25 (24 Saac): ",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857)
                                    )
                                    Text(
                                        text = expiryFormatted,
                                        fontSize = 11.sp,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Text("Faahfaahin →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ShiriEmerald)
                            }
                        }
                    }
                }

                // ==============================================================
                // 5. UNIVERSAL SEARCH & SOMALI VOICE INPUT
                // ==============================================================
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("ride") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = ShiriCrimson,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Xaggee u socotaa? Where to in Mogadishu?",
                                fontSize = 13.5.sp,
                                color = Color(0xFF64748B),
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF4FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice",
                                    tint = ShiriCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Quick Destination Landmark Tags
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Iskoolka KM4", Icons.Default.NearMe, ShiriCrimson),
                            Triple("Suuqa Bakaaraha", Icons.Default.Storefront, ShiriTeal),
                            Triple("Liido Beach", Icons.Default.Waves, Color(0xFF0284C7)),
                            Triple("Aadan Cadde Airport", Icons.Default.Flight, Color(0xFFD97706)),
                            Triple("Isbitaal Banaadir", Icons.Default.LocalHospital, ShiriEmerald)
                        ).forEach { (tag, icon, color) ->
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable { onNavigate("ride") }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = tag, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = ShiriDarkSlate)
                                }
                            }
                        }
                    }
                }

                // ==============================================================
                // 6. LIVE COMMUTE & ROAD TRAFFIC INTELLIGENCE BAR (Goes to SHIRI RIDE)
                // ==============================================================
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("ride") }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Traffic, contentDescription = null, tint = ShiriCrimson, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("LIVE RADAR", fontSize = 10.5.sp, fontWeight = FontWeight.Black, color = ShiriCrimson)
                                    Text(" • ", fontSize = 10.sp, color = Color(0xFF94A3B8))
                                    Text(
                                        text = "${nearestIntersection?.name ?: "Makka Al-Mukarama"} Furfuran",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiriEmerald
                                    )
                                }
                                Text(
                                    text = "Saxmadda isgoysyada toos uga daawo SHIRI RIDE",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = ShiriCrimson, modifier = Modifier.size(20.dp))
                    }
                }



                // ==============================================================
                // 8. LAUNCH PROMO BANNER
                // ==============================================================
                Surface(
                    color = Color(0xFFFFECEB),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = ShiriCrimson,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "50% DHISMO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SHIRI2025",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ShiriCrimson
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bajaaj-kaaga Koowaad waa Kala-Bar!",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiriDarkSlate
                            )
                            Text(
                                text = "Koodhka SHIRI2025 isticmaal safarkaaga 1-aad.",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                        }
                        Button(
                            onClick = { onNavigate("ride") },
                            colors = ButtonDefaults.buttonColors(containerColor = ShiriCrimson),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Dalbo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // ==============================================================
                // 9. RECENT ACTIVITY PREVIEW CARD
                // ==============================================================
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dalabyadii Dhowaa • Recent",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiriDarkSlate
                            )
                            Text(
                                text = "Taariikhda",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ShiriCrimson,
                                modifier = Modifier.clickable { onNavigate("ride") }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFECFDF5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ShiriEmerald, modifier = Modifier.size(18.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Bajaaj Safar Guul",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ShiriDarkSlate
                                    )
                                    Text(
                                        text = "Hodan ilaa Suuqa Bakaaraha",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Text(
                                text = "$1.50",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ShiriDarkSlate
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun ShiriHubCard(
    title: String,
    subtitle: String,
    statusTag: String,
    badgeText: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier
            .height(155.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: Icon box + Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = gradientColors.first(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Bottom row: Title, Subtitle, Status
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6EE7B7))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = statusTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FastPayWalletCard(
    provider: String,
    code: String,
    balance: String,
    color: Color,
    bgContainer: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = bgContainer,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = provider,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Text(text = code, fontSize = 9.5.sp, color = color, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = balance,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ShiriDarkSlate
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
                val sdf = java.text.SimpleDateFormat(fmt, Locale.US)
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                parsed = sdf.parse(endStr)
                if (parsed != null) break
            } catch (_: Exception) {}
        }
        if (parsed != null) {
            val remainingMs = parsed.time - System.currentTimeMillis()
            if (remainingMs <= 0) {
                "Wuu dhacay"
            } else {
                val hours = remainingMs / (1000 * 60 * 60)
                val mins = (remainingMs / (1000 * 60)) % 60
                "Waxaa ka dhiman ${hours}h ${mins}m"
            }
        } else {
            "24 Saac firfircoon"
        }
    } catch (_: Exception) {
        "24 Saac firfircoon"
    }
}
