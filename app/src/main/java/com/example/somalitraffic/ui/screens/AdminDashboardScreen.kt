package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.*
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.somalitraffic.ui.components.TrafficStatusBadge
import com.example.ui.theme.*

@Composable
fun AdminDashboardScreen(
    viewModel: SomaliTrafficViewModel,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val intersections by viewModel.intersections.collectAsState()
    val incidents by viewModel.incidents.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val supportTickets by viewModel.supportTickets.collectAsState()
    val allSubscriptions by viewModel.allSubscriptions.collectAsState()
    val allDevices by viewModel.allDevices.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "Guudmar",
        "Macaamiisha",
        "Lacagaha",
        "Saxmadda",
        "Wariyeyaasha",
        "Isgoysyada",
        "Taleefannada",
        "Taageerada",
        "Shaqaalaha",
        "Diiwaanka"
    )

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Maamulka GoBajaaj",
                actions = {
                    IconButton(onClick = { viewModel.logout(onLogout) }) {
                        Icon(Icons.Default.Logout, contentDescription = "Ka bax", tint = Color.White)
                    }
                }
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Admin Info banner
            Surface(
                color = BrandBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = currentUser?.fullName ?: "Maamule",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser?.role?.titleSomali ?: "Super Admin",
                            color = BrandRedPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Amni Buuxa (RLS)",
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Scrollable Admin Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = BrandWhite,
                contentColor = BrandRedPrimary,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) BrandRedPrimary else TextBlack
                            )
                        }
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                when (selectedTab) {
                    0 -> AdminOverviewTab(intersections.size, incidents.size, auditLogs.size, allSubscriptions)
                    1 -> AdminUsersTab(allSubscriptions)
                    2 -> AdminSubscriptionsTab(allSubscriptions, viewModel)
                    3 -> AdminTrafficTab(incidents)
                    4 -> AdminReportersTab(intersections, viewModel)
                    5 -> AdminIntersectionsTab(intersections, viewModel)
                    6 -> AdminDevicesTab(allDevices)
                    7 -> AdminSupportTab(supportTickets)
                    8 -> AdminStaffTab(currentUser)
                    9 -> AdminAuditLogsTab(auditLogs)
                }
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(
    intersectionCount: Int,
    incidentCount: Int,
    logCount: Int,
    subscriptions: List<Subscription>
) {
    val activeDrivers = subscriptions.count { it.status.equals("active", ignoreCase = true) }
    val totalRevenue = subscriptions.filter { it.status.equals("active", ignoreCase = true) }.sumOf { it.amount }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Qaybaha Guud ee Nidaamka", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextBlack)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard("Wadarta Isgoysyada", "$intersectionCount", Icons.Default.Traffic, BrandRedPrimary, Modifier.weight(1f))
                AdminStatCard("Digniinaha Socda", "$incidentCount", Icons.Default.Warning, BrandRedDeep, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard("Darawallada Firfircoon", "$activeDrivers", Icons.Default.DirectionsCar, TextBlack, Modifier.weight(1f))
                AdminStatCard("Dakhliga Guud", "$${String.format(java.util.Locale.US, "%.2f", totalRevenue)}", Icons.Default.AttachMoney, BrandRedPrimary, Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminStatCard("Is-diiwaangelinta", "${subscriptions.size}", Icons.Default.Shield, BrandBlack, Modifier.weight(1f))
                AdminStatCard("Diiwaanka Audit", "$logCount", Icons.Default.History, BrandRedPrimary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AdminStatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = BrandWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextBlack)
            Text(text = title, fontSize = 11.sp, color = TextBlack)
        }
    }
}

@Composable
private fun AdminUsersTab(subscriptions: List<Subscription>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Darawallada & Isticmaalayaasha Diiwaangashan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        if (subscriptions.isEmpty()) {
            item {
                Text(
                    text = "Ma jiraan macaamiil diiwaangashan hadda.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(subscriptions) { sub ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(sub.senderPhone ?: sub.userId, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                            Text("Ref: ${sub.paymentReference ?: sub.id.take(8)} • ${sub.planName}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Surface(
                            color = if (sub.status.equals("active", ignoreCase = true)) Color(0xFFDCFCE7) else BrandLightGray,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = sub.status.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sub.status.equals("active", ignoreCase = true)) Color(0xFF15803D) else BrandRedPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSubscriptionsTab(subscriptions: List<Subscription>, viewModel: SomaliTrafficViewModel) {
    if (subscriptions.isEmpty()) {
        Text("Ma jiraan macluumaad lacag-bixineed.", color = TextBlack, modifier = Modifier.padding(16.dp))
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Lacag-bixinta Driver Pass (Pending Payments)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        items(subscriptions) { sub ->
            val isPending = sub.status == "pending"
            
            val statusColor = when (sub.status) {
                "active" -> TrafficOpenGreen
                "pending" -> Color(0xFFF59E0B)
                "rejected" -> BrandRedPrimary
                else -> Color.Gray
            }
            val statusText = when (sub.status) {
                "active" -> "Shaqaynaysa (Active)"
                "pending" -> "Sugaysa (Pending)"
                "rejected" -> "La Diiday (Rejected)"
                else -> sub.status
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BrandWhite),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Macaamiil / Driver ID: ${sub.userId.take(8)}...", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                        Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                            Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Habka Bixinta: ${sub.paymentMethod}", fontSize = 12.sp, color = TextBlack)
                    Text("Lambarka laga diray (Sender): ${sub.senderPhone ?: "N/A"}", fontSize = 12.sp, color = TextBlack, fontWeight = FontWeight.SemiBold)
                    Text("Lambarka loo diray (Destination): ${sub.destinationPhone ?: "N/A"}", fontSize = 12.sp, color = TextBlack)
                    Text("Transaction ID / Ref: ${sub.paymentReference ?: "N/A"}", fontSize = 12.sp, color = TextBlack, fontWeight = FontWeight.Bold)
                    Text("Qiimaha (Amount): $${sub.amount} ${sub.currency}", fontSize = 12.sp, color = TextBlack)
                    Text("Taariikhda la diray: ${sub.startDate}", fontSize = 11.sp, color = Color.Gray)

                    if (!sub.rejectionReason.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sababta loo diiday: ${sub.rejectionReason}", fontSize = 12.sp, color = BrandRedPrimary)
                    }

                    if (isPending) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(
                                onClick = { viewModel.updateSubscriptionStatus(sub.id, "rejected", "Transaction ID lama xaqiijin karo ama lambarku waa khalad") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandRedPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandRedPrimary)
                            ) {
                                Text("Diid (Reject)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.updateSubscriptionStatus(sub.id, "active") },
                                colors = ButtonDefaults.buttonColors(containerColor = TrafficOpenGreen)
                            ) {
                                Text("Oggolow (Approve)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminTrafficTab(incidents: List<TrafficIncident>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Maamulka Saxmadda & Shilalka", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        items(incidents) { inc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandWhite),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(inc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextBlack)
                        Text(inc.severity, color = BrandRedDeep, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text("${inc.locationName} • ${inc.createdAt}", fontSize = 11.sp, color = TextBlack)
                }
            }
        }
    }
}

@Composable
private fun AdminReportersTab(
    intersections: List<Intersection>,
    viewModel: SomaliTrafficViewModel
) {
    var showRegisterDialog by remember { mutableStateOf(false) }
    var reporterName by remember { mutableStateOf("") }
    var reporterPhone by remember { mutableStateOf("") }
    var selectedIntId by remember { mutableStateOf(intersections.firstOrNull()?.id ?: "") }

    val reporters = remember {
        mutableStateListOf(
            Triple("Wariye Cali Xasan", "Isgoyska KM4", "2.0335, 45.3190"),
            Triple("Wariye Maryan Jaamac", "Isgoyska Zoobe", "2.0298, 45.3125"),
            Triple("Wariye Cabdi Shire", "Isgoyska Dabka", "2.0381, 45.3280")
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wariyeyaasha Diiwaangashan (${reporters.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack)
            Button(
                onClick = { showRegisterDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Diiwaangeli Wariye", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reporters) { (name, locName, coords) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                            Text("$locName • GPS: $coords", fontSize = 12.sp, color = TextBlack)
                        }
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                            Text("Active", fontSize = 10.sp, color = TrafficOpenGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }

    if (showRegisterDialog) {
        AlertDialog(
            onDismissRequest = { showRegisterDialog = false },
            title = { Text("Diiwaangeli Wariye Cusub", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reporterName,
                        onValueChange = { reporterName = it },
                        label = { Text("Magaca Wariyaha") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = reporterPhone,
                        onValueChange = { reporterPhone = it },
                        label = { Text("Lambarka Taleefanka") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Text("Isgoyska Loo Xirayo:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                    intersections.take(5).forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIntId = item.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedIntId == item.id,
                                onClick = { selectedIntId = item.id }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item.name, fontSize = 13.sp, color = TextBlack)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reporterName.isNotBlank() && reporterPhone.isNotBlank()) {
                            val matched = intersections.find { it.id == selectedIntId }
                            val loc = matched?.name ?: "Isgoyska Cusub"
                            val coords = "${matched?.latitude ?: 2.0335}, ${matched?.longitude ?: 45.3190}"
                            reporters.add(Triple(reporterName, loc, coords))
                            showRegisterDialog = false
                            reporterName = ""
                            reporterPhone = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary)
                ) {
                    Text("Diiwaangeli", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterDialog = false }) { Text("Ka baaq", color = TextBlack) }
            },
            containerColor = BrandWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AdminIntersectionsTab(
    intersections: List<Intersection>,
    viewModel: SomaliTrafficViewModel
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Hodan") }
    var latText by remember { mutableStateOf("2.0335") }
    var lngText by remember { mutableStateOf("45.3190") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Isgoysyada Muqdisho (${intersections.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack)
            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Isgoys Cusub", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(intersections) { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextBlack)
                            Text("GPS: ${item.latitude}, ${item.longitude} • Radius: ${item.allowedRadius}m", fontSize = 11.sp, color = TextBlack)
                        }
                        TrafficStatusBadge(status = item.currentStatus)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Diiwaangeli Isgoys Cusub & Location", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextBlack) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Magaca Isgoyska (m.s. Isgoyska KM5)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it },
                        label = { Text("Degmada") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = latText,
                            onValueChange = { latText = it },
                            label = { Text("Latitude") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = lngText,
                            onValueChange = { lngText = it },
                            label = { Text("Longitude") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull() ?: 2.0335
                        val lng = lngText.toDoubleOrNull() ?: 45.3190
                        if (name.isNotBlank()) {
                            viewModel.registerIntersection(name, lat, lng, 400, "Muqdisho", district) {
                                showAddDialog = false
                                name = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary)
                ) {
                    Text("Kaydi Isgoyska", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Ka baaq", color = TextBlack) }
            },
            containerColor = BrandWhite,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun AdminDevicesTab(devices: List<com.example.somalitraffic.data.model.Device>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Taleefannada Ku Jira Nidaamka Ilaalinta (${devices.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        if (devices.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Ma jiraan taleefanno ilaa hadda la diiwaangeliyay.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(devices) { dev ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val title = dev.deviceName.ifBlank { dev.deviceModel.ifBlank { "Taleefanka #${dev.id.takeLast(4)}" } }
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                            val stText = if (dev.isLost) "🔴 WAA LUMAY" else if (dev.isProtected) "🟢 La ilaaliyay • Online" else "⚪ Lama ilaalin"
                            Text(stText, fontSize = 12.sp, color = if (dev.isLost) BrandRedPrimary else Color(0xFF16A34A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminSupportTab(tickets: List<SupportTicket>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Xarunta Caawinta & Cabashooyinka", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        if (tickets.isEmpty()) {
            item { Text("Ma jiraan cabashooyin cusub.", fontSize = 13.sp, color = TextBlack) }
        } else {
            items(tickets) { t ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${t.ticketNumber}: ${t.subject}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextBlack)
                            Text(t.status, color = TrafficOpenGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Text(t.message, fontSize = 12.sp, color = TextBlack)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStaffTab(currentUser: UserProfile?) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Shaqaalaha & Xilalka (Staff & Roles)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = BrandWhite),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(currentUser?.fullName ?: "Maamulaha Guud", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextBlack)
                        Text("${currentUser?.role?.titleSomali ?: "Super Admin"} • ${currentUser?.phoneNumber ?: ""}", fontSize = 12.sp, color = TextBlack)
                    }
                    Surface(color = BrandLightGray, shape = RoundedCornerShape(6.dp)) {
                        Text("Active", fontSize = 10.sp, color = BrandRedPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAuditLogsTab(logs: List<AuditLog>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Diiwaanka Dhaqdhaqaaqa (Audit Logs)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextBlack) }
        if (logs.isEmpty()) {
            item { Text("Wali ma jiraan diiwaanno la kaydiyay.", fontSize = 13.sp, color = TextBlack) }
        } else {
            items(logs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.action, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandRedPrimary)
                            Text(log.createdAt, fontSize = 11.sp, color = TextBlack)
                        }
                        Text("${log.actorName} - ${log.metadata ?: ""}", fontSize = 12.sp, color = TextBlack)
                    }
                }
            }
        }
    }
}
