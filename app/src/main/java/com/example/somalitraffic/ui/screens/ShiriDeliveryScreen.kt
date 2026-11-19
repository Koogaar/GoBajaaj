package com.example.somalitraffic.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.util.ArrivalSoundManager
import com.example.ui.theme.BrandGreenPrimary
import com.example.ui.theme.BrandGreenDark
import com.example.ui.theme.BrandGreenLight
import com.example.ui.theme.BrandBgLight
import com.example.ui.theme.BrandCardWhite
import com.example.ui.theme.BrandTextDark

/**
 * SHIRI Delivery Screen (Courier / Delivery App)
 * Matches master visual design:
 * 1. Screen 1: New Delivery (Map route, pickup/delivery addresses, "Accept Order" green button)
 * 2. Screen 2: On the Way (Map with scooter, "Arriving in 8 min", customer Ahmed Hassan ⭐ 4.9, "Contact" green button)
 * 3. Screen 3: Earnings ($85.50 Today, 8 Deliveries, 5.2h Online, Weekly Bar Chart, "View Details" button)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriDeliveryScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // Current driver tab: "new_delivery", "on_the_way", "earnings"
    var driverTab by remember { mutableStateOf("new_delivery") }
    var isDriverOnline by remember { mutableStateOf(true) }
    var orderAccepted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Surface(
                color = BrandCardWhite,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Dib",
                                tint = BrandTextDark
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (driverTab) {
                                "new_delivery" -> "New Delivery"
                                "on_the_way" -> "On the Way"
                                "earnings" -> "Earnings"
                                else -> "Delivery with SHIRI"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandTextDark
                        )
                    }

                    // Online toggle switch
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDriverOnline) BrandGreenPrimary else Color(0xFF64748B),
                        modifier = Modifier.clickable {
                            isDriverOnline = !isDriverOnline
                            Toast.makeText(
                                context,
                                if (isDriverOnline) "Darawalku waa ONLINE" else "Darawalku waa OFFLINE",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDriverOnline) "Online" else "Offline",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = BrandCardWhite,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DriverTabItem(
                        icon = Icons.Default.DirectionsBike,
                        label = "New Delivery",
                        isSelected = driverTab == "new_delivery",
                        onClick = { driverTab = "new_delivery" }
                    )
                    DriverTabItem(
                        icon = Icons.Default.Navigation,
                        label = "On the Way",
                        isSelected = driverTab == "on_the_way",
                        badge = if (orderAccepted) "1" else null,
                        onClick = { driverTab = "on_the_way" }
                    )
                    DriverTabItem(
                        icon = Icons.Default.AttachMoney,
                        label = "Earnings",
                        isSelected = driverTab == "earnings",
                        onClick = { driverTab = "earnings" }
                    )
                }
            }
        },
        containerColor = BrandBgLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (driverTab) {
                "new_delivery" -> {
                    NewDeliveryScreenView(
                        orderAccepted = orderAccepted,
                        onAcceptOrder = {
                            orderAccepted = true
                            ArrivalSoundManager.playBajajHornSequence(context)
                            Toast.makeText(context, "Dalabka waa la aqbalay! U dhaqaaq maqaayadda.", Toast.LENGTH_LONG).show()
                            driverTab = "on_the_way"
                        }
                    )
                }
                "on_the_way" -> {
                    OnTheWayScreenView(
                        onContactClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                    data = Uri.parse("tel:+252613456789")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Wacaya Ahmed Hassan: +252613456789", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDeliveredClick = {
                            orderAccepted = false
                            Toast.makeText(context, "Dalabka waa la gaarsiiyay! Dakhliga $3.50 ayaa lagu daray.", Toast.LENGTH_LONG).show()
                            driverTab = "earnings"
                        }
                    )
                }
                "earnings" -> {
                    DriverEarningsScreenView(
                        onViewDetails = {
                            Toast.makeText(context, "Dakhliga maanta: $85.50 (8 Safar oo la dhammeeyay)", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 1: NEW DELIVERY (Matching Master Mockup)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NewDeliveryScreenView(
    orderAccepted: Boolean,
    onAcceptOrder: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Map Canvas simulation (Upper Half)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFE2E8F0))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Road grid
                drawRect(color = Color(0xFFEDF2F7))

                // Roads (grey lines)
                val roadColor = Color(0xFFCBD5E1)
                drawLine(roadColor, Offset(0f, h * 0.3f), Offset(w, h * 0.3f), strokeWidth = 14f)
                drawLine(roadColor, Offset(0f, h * 0.7f), Offset(w, h * 0.7f), strokeWidth = 14f)
                drawLine(roadColor, Offset(w * 0.25f, 0f), Offset(w * 0.25f, h), strokeWidth = 14f)
                drawLine(roadColor, Offset(w * 0.75f, 0f), Offset(w * 0.75f, h), strokeWidth = 14f)

                // Park / Greenery patches
                drawRoundRect(
                    color = Color(0xFFDCFCE7),
                    topLeft = Offset(w * 0.05f, h * 0.05f),
                    size = androidx.compose.ui.geometry.Size(w * 0.16f, h * 0.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
                drawRoundRect(
                    color = Color(0xFFDCFCE7),
                    topLeft = Offset(w * 0.65f, h * 0.4f),
                    size = androidx.compose.ui.geometry.Size(w * 0.28f, h * 0.22f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )

                // Route Path (Blue line)
                val routePath = Path().apply {
                    moveTo(w * 0.25f, h * 0.65f) // Pizza Palace
                    cubicTo(
                        w * 0.4f, h * 0.75f,
                        w * 0.6f, h * 0.45f,
                        w * 0.75f, h * 0.35f  // Via Somalia
                    )
                }
                drawPath(
                    path = routePath,
                    color = Color(0xFF3B82F6),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )

                // Green Start Pin: Pizza Palace
                drawCircle(color = BrandGreenPrimary.copy(alpha = 0.25f), radius = 22f, center = Offset(w * 0.25f, h * 0.65f))
                drawCircle(color = BrandGreenPrimary, radius = 12f, center = Offset(w * 0.25f, h * 0.65f))
                drawCircle(color = Color.White, radius = 5f, center = Offset(w * 0.25f, h * 0.65f))

                // Red End Pin: Via Somalia
                drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.25f), radius = 22f, center = Offset(w * 0.75f, h * 0.35f))
                drawCircle(color = Color(0xFFEF4444), radius = 12f, center = Offset(w * 0.75f, h * 0.35f))
                drawCircle(color = Color.White, radius = 5f, center = Offset(w * 0.75f, h * 0.35f))
            }
        }

        // Bottom Details Card
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = BrandCardWhite,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Route Locations
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Pick up
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandGreenLight,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(BrandGreenPrimary)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pick up from",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Pizza Palace",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextDark
                            )
                        }
                    }

                    // Deliver to
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Deliver to",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Via Somalia",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextDark
                            )
                        }
                    }
                }

                // Accept Order Button (Mockup exact)
                Button(
                    onClick = onAcceptOrder,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (orderAccepted) "Order Accepted ✓" else "Accept Order",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 2: ON THE WAY (Matching Master Mockup)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnTheWayScreenView(
    onContactClick: () -> Unit,
    onDeliveredClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Map with Scooter & "Arriving in 8 min" floating pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xFFE2E8F0))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                drawRect(color = Color(0xFFEDF2F7))

                val roadColor = Color(0xFFCBD5E1)
                drawLine(roadColor, Offset(0f, h * 0.4f), Offset(w, h * 0.4f), strokeWidth = 14f)
                drawLine(roadColor, Offset(0f, h * 0.7f), Offset(w, h * 0.7f), strokeWidth = 14f)
                drawLine(roadColor, Offset(w * 0.35f, 0f), Offset(w * 0.35f, h), strokeWidth = 14f)
                drawLine(roadColor, Offset(w * 0.8f, 0f), Offset(w * 0.8f, h), strokeWidth = 14f)

                // Route
                val routePath = Path().apply {
                    moveTo(w * 0.2f, h * 0.75f)
                    cubicTo(
                        w * 0.4f, h * 0.8f,
                        w * 0.6f, h * 0.35f,
                        w * 0.8f, h * 0.25f
                    )
                }
                drawPath(
                    path = routePath,
                    color = Color(0xFF3B82F6),
                    style = Stroke(width = 10f, cap = StrokeCap.Round)
                )

                // Start
                drawCircle(color = BrandGreenPrimary, radius = 10f, center = Offset(w * 0.2f, h * 0.75f))
            }

            // Floating Scooter Badge on Route
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 40.dp, y = (-20.dp))
            ) {
                Surface(
                    shape = CircleShape,
                    color = BrandGreenPrimary,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Floating Pill: "Arriving in 8 min"
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = BrandCardWhite,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Arriving in",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "8 min",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandTextDark
                    )
                }
            }
        }

        // Bottom Customer Card (Ahmed Hassan ⭐ 4.9)
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = BrandCardWhite,
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Customer row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE2E8F0),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ahmed Hassan",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "★",
                                    fontSize = 12.sp,
                                    color = Color(0xFFF59E0B)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "4.9",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Phone & Chat icons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onContactClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    tint = BrandGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { onContactClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = "Message",
                                    tint = BrandGreenPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Big Green Contact Button
                Button(
                    onClick = onContactClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Contact",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Mark Delivered Text Button
                TextButton(
                    onClick = onDeliveredClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Dhammee Gaarsiinta (Delivered) ✓",
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN 3: EARNINGS (Matching Master Mockup)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DriverEarningsScreenView(
    onViewDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Earnings Today Card ($85.50 Today)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BrandCardWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "$85.50",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandTextDark
                )
                Text(
                    text = "Today",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )

                Divider(color = Color(0xFFF1F5F9))

                // Stat boxes: 8 Deliveries | 5.2h Online
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "8",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandTextDark
                        )
                        Text(
                            text = "Deliveries",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "5.2h",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandTextDark
                        )
                        Text(
                            text = "Online",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        // Weekly Earnings Bar Chart (Mon - Sun)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BrandCardWhite,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Weekly Activity",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(14.dp))
                ShiriWeeklyEarningsBarChart(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                )
            }
        }

        // Big Green View Details Button
        Button(
            onClick = onViewDetails,
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = "View Details",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Driver Bottom Tab Item Component
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DriverTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) BrandGreenPrimary else Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
            if (badge != null) {
                Surface(
                    color = BrandGreenPrimary,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) BrandGreenPrimary else Color(0xFF94A3B8)
        )
    }
}
