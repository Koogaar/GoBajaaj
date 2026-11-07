package com.example.somalitraffic.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.util.ArrivalSoundManager

// ─── Delivery Step Enum for Customer Flow ───────────────────────────────────
private enum class DeliveryStep {
    SELECT,     // Tallaabada 1: Dooro nooca + geli faahfaahin
    CHECKOUT,   // Tallaabada 2: Checkout - address, lacag-bixin, xaqiiji
    MERCHANT,   // Tallaabada 3: Merchant aqbalay
    TRACKING,   // Tallaabada 4: Live tracking + darawal
    HANDOVER    // Tallaabada 5: PIN gacangelinta
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriDeliveryScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    // View Mode: "customer" (5-Step Customer Flow) or "driver" (Image 3)
    var currentDeliveryView by remember { mutableStateOf("customer") }

    // Driver States (Image 3)
    var isDriverOnline by remember { mutableStateOf(true) }
    var availableOrdersCount by remember { mutableStateOf(0) }
    var driverPaymentNumber by remember { mutableStateOf("0613456789") }
    var driverDirectPhone by remember { mutableStateOf("+252613456789") }
    var driverWhatsappNumber by remember { mutableStateOf("252613456789") }
    var showEditDriverContactDialog by remember { mutableStateOf(false) }

    // Driver Bottom Navigation Tab: "dashboard", "orders", "navigation", "earnings"
    var driverBottomTab by remember { mutableStateOf("dashboard") }

    // Customer 5-Step Flow States
    var selectedServiceType by remember { mutableStateOf("package") }
    var pickupLocation by remember { mutableStateOf("") }
    var dropoffLocation by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var packageNotes by remember { mutableStateOf("") }
    var deliverySpeed by remember { mutableStateOf("express") }

    var shiriAddress by remember { mutableStateOf("") }
    var voiceLandmark by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("evc") }
    var payerPhone by remember { mutableStateOf("") }

    var orderTrackingId by remember { mutableStateOf("SHR-2026-636919") }
    var handoverPin by remember { mutableStateOf("4829") }
    var driverEnteredPin by remember { mutableStateOf("") }
    var merchantApproved by remember { mutableStateOf(false) }
    var isDelivered by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(DeliveryStep.SELECT) }

    val fareAmount = when {
        selectedServiceType == "shopping" -> if (deliverySpeed == "express") 3.50 else 2.50
        deliverySpeed == "express" -> 2.50
        else -> 1.50
    }

    val ussdCode = when (selectedPayment) {
        "evc"   -> "*712*618092060*${String.format("%.2f", fareAmount)}#"
        "edahab"-> "*712*628512131*${String.format("%.2f", fareAmount)}#"
        "zaad"  -> "*920*${String.format("%.2f", fareAmount)}#"
        "sahal" -> "*889*${String.format("%.2f", fareAmount)}#"
        else    -> "Lacag gacanta ah (Cash)"
    }

    // Dialog for Driver editing payment & contact details
    if (showEditDriverContactDialog) {
        var editEvc by remember { mutableStateOf(driverPaymentNumber) }
        var editDirect by remember { mutableStateOf(driverDirectPhone) }
        var editWa by remember { mutableStateOf(driverWhatsappNumber) }

        AlertDialog(
            onDismissRequest = { showEditDriverContactDialog = false },
            title = {
                Text(
                    text = "Wax ka beddel Xiriirka & Lacagta",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editEvc,
                        onValueChange = { editEvc = it },
                        label = { Text("Lambarka EVC / eDahab") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDirect,
                        onValueChange = { editDirect = it },
                        label = { Text("Wacitaanka Caadiga ah (Direct Call)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWa,
                        onValueChange = { editWa = it },
                        label = { Text("WhatsApp Toos ah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        driverPaymentNumber = editEvc.trim()
                        driverDirectPhone = editDirect.trim()
                        driverWhatsappNumber = editWa.trim()
                        showEditDriverContactDialog = false
                        Toast.makeText(context, "Xogta darawalka waa la cusbooneysiiyay!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Keydi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDriverContactDialog = false }) {
                    Text("Ka Noqo")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (currentDeliveryView == "customer" && currentStep != DeliveryStep.SELECT) {
                                        when (currentStep) {
                                            DeliveryStep.CHECKOUT -> currentStep = DeliveryStep.SELECT
                                            DeliveryStep.MERCHANT -> currentStep = DeliveryStep.CHECKOUT
                                            DeliveryStep.TRACKING -> currentStep = DeliveryStep.MERCHANT
                                            DeliveryStep.HANDOVER -> currentStep = DeliveryStep.TRACKING
                                            else -> onBackClick()
                                        }
                                    } else {
                                        onBackClick()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dib", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHIRI COURIER",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // Discrete Courier mode button
                        if (currentDeliveryView == "customer") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.clickable { currentDeliveryView = "driver" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🏍️", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Darawal Gaarsiiye",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable { currentDeliveryView = "customer" }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Ku Noqo Macmiilka",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (currentDeliveryView == "driver") {
                // Courier Driver Bottom Navigation (Image 3): Dashboard, Dalabyada, Navigation (badge 1), Dakhliga
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DriverBottomNavItem(
                            icon = Icons.Default.TwoWheeler,
                            label = "Dashboard",
                            isSelected = driverBottomTab == "dashboard",
                            badge = null,
                            onClick = { driverBottomTab = "dashboard" }
                        )
                        DriverBottomNavItem(
                            icon = Icons.Default.ShoppingBag,
                            label = "Dalabyada",
                            isSelected = driverBottomTab == "orders",
                            badge = if (availableOrdersCount > 0) availableOrdersCount.toString() else null,
                            onClick = { driverBottomTab = "orders" }
                        )
                        DriverBottomNavItem(
                            icon = Icons.Default.Navigation,
                            label = "Navigation",
                            isSelected = driverBottomTab == "navigation",
                            badge = "1",
                            onClick = { driverBottomTab = "navigation" }
                        )
                        DriverBottomNavItem(
                            icon = Icons.Default.AttachMoney,
                            label = "Dakhliga",
                            isSelected = driverBottomTab == "earnings",
                            badge = null,
                            onClick = { driverBottomTab = "earnings" }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->

        if (currentDeliveryView == "driver") {
            // ══════════════════════════════════════════════════════════════════
            // 1. COURIER / DRIVER APP VIEW (IMAGE 3)
            // ══════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Driver Profile Header Card (Image 3)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Ahmed Hassan (Canab)",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isDriverOnline) Color(0xFF10B981) else Color.Gray)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Bajaj • MG-4821-B",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "★", fontSize = 11.sp, color = Color(0xFFF59E0B))
                                    Text(
                                        text = "4.8",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Alert Sound Button (Image 3)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.clickable {
                                    ArrivalSoundManager.playBajajHornSequence(context)
                                    Toast.makeText(context, "Siren / Alert Sound dhacay!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Alert",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }

                            // Online Toggle Button (Image 3)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDriverOnline) Color(0xFF059669) else Color(0xFF64748B),
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isDriverOnline) "Online" else "Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 1: DALABYADA SOO DHACAYA (AVAILABLE ORDERS) (Image 3)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFDC2626),
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFF991B1B))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF97316))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "DALABYADA SOO DHACAYA\n(AVAILABLE ORDERS)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        lineHeight = 14.sp
                                    )
                                }

                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "$availableOrdersCount Banaan",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Dalabyo cusub oo laga dalbaday maqaayadaha iyo dukaamada Mogadishu ayaa diyaar u ah qaadasho.",
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )

                            // Action buttons: "Eeg Dalabyada Soo Dhacaya" & "+ Tijaabi Dalab"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        Toast.makeText(
                                            context,
                                            if (availableOrdersCount > 0) "Waxaa jira $availableOrdersCount dalab oo furan!" else "Waqtigan ma jiraan dalabyo furan. Guji 'Tijaabi Dalab'.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1.3f).height(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.MailOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Eeg Dalabyada ($availableOrdersCount)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFDC2626),
                                        maxLines = 1
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        availableOrdersCount += 1
                                        ArrivalSoundManager.playBajajHornSequence(context)
                                        Toast.makeText(context, "Dalab cusub ayaa soo dhacay! 🏍️", Toast.LENGTH_SHORT).show()
                                    },
                                    border = BorderStroke(1.dp, Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tijaabi Dalab",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 2: DAKHLIGA MAANTA (Dark Slate Card - Image 3)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF1E293B),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DAKHLIGA MAANTA",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "$",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981)
                            )
                        }

                        Text(
                            text = "$49.00",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF34D399)
                        )

                        Divider(color = Color(0xFF334155), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Gaarsiin la dhammeystiray:",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "13 safar",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Cash on Delivery:",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                                Text(
                                    text = "$18.00",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFBBF24)
                                )
                            }
                        }
                    }
                }

                // Card 3: Xiriirka & Lacag-Bixinta Darawalka (Image 3)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Xiriirka & Lacag-Bixinta Darawalka",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Nambarada lacag-bixinta EVC Plus & WhatsApp tooska ah",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { showEditDriverContactDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Wax ka beddel",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }

                        Divider(color = Color(0xFFF1F5F9))

                        // Details rows
                        ContactDetailRow(
                            label = "Lambarka Lacag-bixinta (EVC/eDahab):",
                            value = driverPaymentNumber,
                            icon = null,
                            valueColor = Color(0xFF059669)
                        )
                        ContactDetailRow(
                            label = "Wacitaanka Caadiga ah (Direct Call):",
                            value = driverDirectPhone,
                            icon = Icons.Default.Call,
                            valueColor = Color(0xFF0F172A)
                        )
                        ContactDetailRow(
                            label = "WhatsApp Toos ah:",
                            value = driverWhatsappNumber,
                            icon = Icons.Default.Chat,
                            valueColor = Color(0xFF059669)
                        )
                    }
                }

                // Card 4: ACTIVE DELIVERY IN PROGRESS (Image 3)
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.5.dp, Color(0xFFFCA5A5)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentDeliveryView = "customer"
                            currentStep = DeliveryStep.TRACKING
                            Toast.makeText(context, "Furaya shaashadda gaarsiinta...", Toast.LENGTH_SHORT).show()
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
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ACTIVE DELIVERY IN PROGRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "#SHR-2026-636919 • SHR-MG-73120",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "(Waberi)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // ══════════════════════════════════════════════════════════════════
            // 2. CUSTOMER 5-STEP DELIVERY FLOW
            // ══════════════════════════════════════════════════════════════════
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it } + fadeOut()
                },
                label = "customer_step"
            ) { step ->
                when (step) {
                    DeliveryStep.SELECT -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Step 1: Form
                            Text("1/5 — Xulashada Nooca Gaarsiinta", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DeliveryServiceCard("Send Package", "Documents", "📦", selectedServiceType == "package", { selectedServiceType = "package" }, Modifier.weight(1f))
                                DeliveryServiceCard("Send Shopping", "Market Items", "🛍️", selectedServiceType == "shopping", { selectedServiceType = "shopping" }, Modifier.weight(1f))
                                DeliveryServiceCard("Send Food", "Restaurant", "🍔", selectedServiceType == "food", { selectedServiceType = "food" }, Modifier.weight(1f))
                            }

                            Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = pickupLocation,
                                        onValueChange = { pickupLocation = it },
                                        label = { Text("Goobta laga qaadayo (Pickup)") },
                                        placeholder = { Text("KM4, Hodan, Muqdisho") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = dropoffLocation,
                                        onValueChange = { dropoffLocation = it },
                                        label = { Text("Goobta la geynayo (Dropoff)") },
                                        placeholder = { Text("Taleex, Waberi, Bakaaro...") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = recipientPhone,
                                        onValueChange = { recipientPhone = it },
                                        label = { Text("Telefoonka Qaataha") },
                                        placeholder = { Text("61XXXXXXX") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (dropoffLocation.isBlank()) {
                                        Toast.makeText(context, "Geli goobta la geynayo!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (recipientPhone.isBlank()) {
                                        Toast.makeText(context, "Geli telefoonka!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    currentStep = DeliveryStep.CHECKOUT
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Gudub Checkout (2/5) →", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }

                    DeliveryStep.CHECKOUT -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("2/5 — Checkout & Lacag-bixin", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            OutlinedTextField(
                                value = shiriAddress,
                                onValueChange = { shiriAddress = it },
                                label = { Text("Shiri Address (tusaale: SHR-MG-48291)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = voiceLandmark,
                                onValueChange = { voiceLandmark = it },
                                label = { Text("Voice Landmark / Tilmaanta Goobta") },
                                placeholder = { Text("Agta masaajidka, dabaq cagaar ah...") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { currentStep = DeliveryStep.MERCHANT },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Xaqiiji & Dalbo Hadda (3/5) →", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }

                    DeliveryStep.MERCHANT -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("3/5 — Aqbalida Meheradda", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(10.dp))
                            Text("Dalab #$orderTrackingId ayaa u gartay meheradda.")
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = { currentStep = DeliveryStep.TRACKING },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                            ) {
                                Text("Oggolow Lacagta & Dir Darawalka (4/5) →", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    DeliveryStep.TRACKING -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text("4/5 — Live Tracking & Darawal", fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFE0F2FE), modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🏍️ Khariidadda Live-ka ah — Darawalku wuu soo socdaa", fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                }
                            }
                            Button(
                                onClick = { currentStep = DeliveryStep.HANDOVER },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                modifier = Modifier.fillMaxWidth().height(50.dp)
                            ) {
                                Text("Fur Handover PIN (5/5) →", fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }

                    DeliveryStep.HANDOVER -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("5/5 — Handover PIN", fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(10.dp))
                            Text("Koodhkaaga sirta ah ee darawalka:", fontSize = 13.sp)
                            Spacer(Modifier.height(10.dp))
                            Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(16.dp)) {
                                Text(
                                    handoverPin,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 8.sp,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    Toast.makeText(context, "✅ Si nabad ah ayaa laguugu keenay!", Toast.LENGTH_LONG).show()
                                    currentStep = DeliveryStep.SELECT
                                    currentDeliveryView = "driver"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                            ) {
                                Text("Dhammee Dalabka (Delivered ✅)", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Contact Detail Row Component ────────────────────────────────────────────

@Composable
private fun ContactDetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun DriverBottomNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFDC2626) else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
            if (badge != null) {
                Surface(
                    color = Color(0xFFDC2626),
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) Color(0xFFDC2626) else Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun DeliveryServiceCard(
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
