package com.example.somalitraffic.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun ReporterDashboardScreen(
    viewModel: SomaliTrafficViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val intersections by viewModel.intersections.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedIntersectionId by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isWarning by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showCloseRoadDialog by remember { mutableStateOf(false) }
    var showIntersectionPicker by remember { mutableStateOf(false) }

    // Custom cause for Closed Road
    var selectedPresetCause by remember { mutableStateOf("Dhismo Waddo") }
    var customCauseText by remember { mutableStateOf("") }

    // Live Reporter Device GPS Location
    var reporterLat by remember { mutableDoubleStateOf(2.0335) }
    var reporterLng by remember { mutableDoubleStateOf(45.3190) }
    var isGpsLive by remember { mutableStateOf(false) }

    // Try fetching actual device location
    LaunchedEffect(Unit) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            if (locationManager != null) {
                val locGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                val locNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                val bestLoc = locGps ?: locNet
                if (bestLoc != null) {
                    reporterLat = bestLoc.latitude
                    reporterLng = bestLoc.longitude
                    isGpsLive = true
                }
            }
        } catch (_: Exception) {}
    }

    val currentIntersection = intersections.find { it.id == selectedIntersectionId }
        ?: intersections.firstOrNull()

    // Calculate distance between reporter GPS and intersection
    val distanceMeters = remember(reporterLat, reporterLng, currentIntersection) {
        if (currentIntersection != null) {
            calculateDistance(reporterLat, reporterLng, currentIntersection.latitude, currentIntersection.longitude)
        } else {
            0.0
        }
    }

    val isWithinRange = distanceMeters <= (currentIntersection?.allowedRadius ?: 500) * 1.5

    fun submitQuickReport(status: String, cause: String? = null) {
        val targetIntId = currentIntersection?.id ?: "int-1"
        viewModel.submitTrafficReport(
            intersectionId = targetIntId,
            status = status,
            cause = cause ?: "",
            notes = if (isWithinRange) "GPS Verified (${distanceMeters.toInt()}m)" else "Remote (${distanceMeters.toInt()}m)",
            lat = reporterLat,
            lng = reporterLng,
            accuracy = if (isGpsLive) 5.0 else 15.0,
            onError = { err ->
                feedbackMessage = "⚠️ Khalad: $err"
                isWarning = true
            }
        ) {
            feedbackMessage = "✅ Xaaladda ($status) si toos ah ayaa loogu gudbiyay darawallada iyo nidaamka!"
            isWarning = false
            showCloseRoadDialog = false
            customCauseText = ""
        }
    }

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Dashboard-ka Wariyaha",
                actions = {
                    IconButton(onClick = { showProfileDialog = true }) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Profile & Active Status
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE50914)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = currentUser?.fullName ?: "Wariye Gaadiid",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Wariyaha Waddooyinka Muqdisho",
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF16A34A).copy(alpha = 0.25f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color(0xFF16A34A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Shaqada: Socota", color = Color(0xFF4ADE80), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Selected Intersection Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { showIntersectionPicker = true }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Isgoyska: ${currentIntersection?.name ?: "Dooro Isgoys"}",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Riix si aad u beddesho isgoyska aad joogto",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
            }

            // GPS Accountability Card (Xaqiijinta Goobta)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isWithinRange) Color(0xFFBBF7D0) else Color(0xFFFED7AA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (isWithinRange) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isWithinRange) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                                contentDescription = null,
                                tint = if (isWithinRange) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isWithinRange) "📍 Goobta La Xaqiijiyay" else "⚠️ Ka Fog Isgoyska",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWithinRange) Color(0xFF166534) else Color(0xFF9A3412)
                                )
                            }
                            Text(
                                text = if (distanceMeters < 1000) {
                                    "${distanceMeters.toInt()}m u jira ${currentIntersection?.name ?: "isgoyska"}"
                                } else {
                                    "%.1f km u jira ${currentIntersection?.name ?: "isgoyska"}".format(distanceMeters / 1000.0)
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        color = if (isWithinRange) Color(0xFF16A34A) else Color(0xFFEA580C),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isWithinRange) "On-Site" else "Remote",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // ONE-TAP QUICK REPORTING SECTION (Taabasho Kaliya)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Taabo Xaaladda Hadda (1-Tap Status)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Toos u gudbi",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE50914)
                    )
                }

                // 1. BUTTON: CAADI / FURAN (Green - 1 Tap)
                OneTapStatusButton(
                    title = "FURAN (Clear)",
                    description = "Gaadiidku si habsami leh ayuu u socdaa, saxmad ma jirto",
                    icon = Icons.Default.CheckCircle,
                    backgroundColor = Color(0xFF16A34A),
                    textColor = Color.White,
                    onClick = { submitQuickReport("Furan") },
                    isLoading = isLoading
                )

                // 2. BUTTON: NORMAL (Yellow/Amber - 1 Tap)
                OneTapStatusButton(
                    title = "NORMAL (Moderate)",
                    description = "Gaadiidku waa badan yahay balse wuu socdaa",
                    icon = Icons.Default.DirectionsCar,
                    backgroundColor = Color(0xFFD97706),
                    textColor = Color.White,
                    onClick = { submitQuickReport("Normal") },
                    isLoading = isLoading
                )

                // 3. BUTTON: JAAM (Red - 1 Tap)
                OneTapStatusButton(
                    title = "JAAM / SAXMAD (Heavy)",
                    description = "Isgoysku waa saxmad daran yahay oo gaadiidku waa xanniban yahay",
                    icon = Icons.Default.Traffic,
                    backgroundColor = Color(0xFFE50914),
                    textColor = Color.White,
                    onClick = { submitQuickReport("Jaam") },
                    isLoading = isLoading
                )

                // 4. BUTTON: XERAN / WADDADA XIRAN (Dark/Black - Opens Cause Sheet)
                OneTapStatusButton(
                    title = "XERAN / WADDADA XIRAN (Closed Road)",
                    description = "Waddadu waa xiran tahay (Dhismo, Shil, Ciidan, iwm)",
                    icon = Icons.Default.Block,
                    backgroundColor = Color(0xFF1E293B),
                    textColor = Color.White,
                    onClick = { showCloseRoadDialog = true },
                    isLoading = isLoading
                )
            }

            // Feedback Alert Banner
            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (feedbackMessage != null) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isWarning) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isWarning) Color(0xFFFCA5A5) else Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isWarning) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isWarning) Color(0xFFDC2626) else Color(0xFF16A34A),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = feedbackMessage!!,
                                color = if (isWarning) Color(0xFF991B1B) else Color(0xFF166534),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal: Road Closed / Special Cause Dialog
    if (showCloseRoadDialog) {
        AlertDialog(
            onDismissRequest = { showCloseRoadDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sababta Xirnaanshaha", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Dooro sababta isgoysku u xiran yahay ama u saxmadsan yahay:",
                        fontSize = 13.sp,
                        color = Color(0xFF334155)
                    )

                    val presetCauses = listOf(
                        "🚧 Dhismo Waddo",
                        "💥 Shil Gaari",
                        "🌧️ Roob & Daad",
                        "👮 Ciidan / Howlgal",
                        "🛑 Bajaaj Ciladoobay"
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetCauses.forEach { cause ->
                            val isSelected = selectedPresetCause == cause
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedPresetCause = cause },
                                color = if (isSelected) Color(0xFFE50914).copy(alpha = 0.1f) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFFE50914) else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedPresetCause = cause },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFE50914))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = cause,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFFE50914) else Color(0xFF0F172A),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customCauseText,
                        onValueChange = { customCauseText = it },
                        label = { Text("Faahfaahin kale (Optional)") },
                        placeholder = { Text("Tusaale: Laamiga KM4 dhanka Hodan...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFE50914),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCause = if (customCauseText.isNotBlank()) {
                            "$selectedPresetCause: $customCauseText"
                        } else {
                            selectedPresetCause
                        }
                        submitQuickReport("Xeran", finalCause)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gudbi Xirnaanshaha", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseRoadDialog = false }) {
                    Text("Ka Noqo", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Modal: Intersection Picker
    if (showIntersectionPicker) {
        AlertDialog(
            onDismissRequest = { showIntersectionPicker = false },
            title = {
                Text("Dooro Isgoyska Aad Joogto", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intersections.forEach { int ->
                        val isSelected = int.id == (currentIntersection?.id ?: "")
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedIntersectionId = int.id
                                    showIntersectionPicker = false
                                },
                            color = if (isSelected) Color(0xFFE50914).copy(alpha = 0.1f) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFFE50914) else Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = int.name,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFFE50914) else Color(0xFF0F172A),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Xaaladda Hadda: ${int.currentStatus}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFFE50914))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntersectionPicker = false }) {
                    Text("Xir", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Profile & Logout Dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = Color(0xFFE50914), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Akoonka Wariyaha", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Magaca: ${currentUser?.fullName ?: "Wariye Gaadiid"}", fontWeight = FontWeight.Medium)
                    Text("Taleefanka: ${currentUser?.phoneNumber ?: "Lama hayo"}")
                    Text("Doorka: Wariye Waddooyin (Reporter)")
                    
                    HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                    
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Amniga & Xaqiijinta GPS", fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8), fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Nidaamku wuxuu si toos ah u xaqiijinayaa goobta aad ka soo dirayso warbixinnada si loo hubiyo saxnaanta xogta la siinayo darawallada.",
                                fontSize = 11.sp,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Xir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        showProfileDialog = false
                        viewModel.logout(onLogout)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Ka Bax (Log Out)", color = Color.White)
                }
            }
        )
    }
}

@Composable
private fun OneTapStatusButton(
    title: String,
    description: String,
    icon: ImageVector,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = textColor.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }

            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Taabo",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}
