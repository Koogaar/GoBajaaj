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

// ─── Delivery Step Enum ─────────────────────────────────────────────────────
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

    // ── Tallaabada 1: Xulashada ──────────────────────────────────────────────
    var selectedServiceType by remember { mutableStateOf("package") }
    var pickupLocation by remember { mutableStateOf("") }
    var dropoffLocation by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var packageNotes by remember { mutableStateOf("") }
    var deliverySpeed by remember { mutableStateOf("express") }

    // ── Tallaabada 2: Checkout ───────────────────────────────────────────────
    var shiriAddress by remember { mutableStateOf("") }
    var voiceLandmark by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("evc") }
    var payerPhone by remember { mutableStateOf("") }

    // ── Tallaabada 3-5: Tracking & PIN ──────────────────────────────────────
    var orderTrackingId by remember { mutableStateOf("") }
    var handoverPin by remember { mutableStateOf("") }
    var driverEnteredPin by remember { mutableStateOf("") }
    var merchantApproved by remember { mutableStateOf(false) }
    var isDelivered by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(DeliveryStep.SELECT) }

    // Qiimaha saacadda iyo nooca
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

    // ── Colors ───────────────────────────────────────────────────────────────
    val blueMain  = Color(0xFF0284C7)
    val blueDark  = Color(0xFF075985)
    val bgLight   = Color(0xFFF0F9FF)
    val bgPage    = Color(0xFFF8FAFC)

    // ── Top Bar ──────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            Surface(
                color = blueMain,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        // Haddii step kale jiraan, ka dib u noqo
                        when (currentStep) {
                            DeliveryStep.SELECT   -> onBackClick()
                            DeliveryStep.CHECKOUT -> currentStep = DeliveryStep.SELECT
                            DeliveryStep.MERCHANT -> currentStep = DeliveryStep.CHECKOUT
                            DeliveryStep.TRACKING -> currentStep = DeliveryStep.MERCHANT
                            DeliveryStep.HANDOVER -> currentStep = DeliveryStep.TRACKING
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dib", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHIRI DELIVERY",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = when (currentStep) {
                                DeliveryStep.SELECT   -> "Dooro Nooca Gaarsiinta"
                                DeliveryStep.CHECKOUT -> "Xaqiiji & Lacag-Bixin"
                                DeliveryStep.MERCHANT -> "Aqbalida Meheradda"
                                DeliveryStep.TRACKING -> "Live Tracking — Darawalka"
                                DeliveryStep.HANDOVER -> "PIN Gacangelinta"
                            },
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    // Step indicator badge
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = when (currentStep) {
                                DeliveryStep.SELECT   -> "1/5"
                                DeliveryStep.CHECKOUT -> "2/5"
                                DeliveryStep.MERCHANT -> "3/5"
                                DeliveryStep.TRACKING -> "4/5"
                                DeliveryStep.HANDOVER -> "5/5"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        containerColor = bgPage
    ) { padding ->

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                slideInHorizontally { it } + fadeIn() togetherWith
                slideOutHorizontally { -it } + fadeOut()
            },
            label = "delivery_step"
        ) { step ->
            when (step) {

                // ══════════════════════════════════════════════════════════════
                // TALLAABADA 1: XULASHADA & FAAHFAAHINTA
                // ══════════════════════════════════════════════════════════════
                DeliveryStep.SELECT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Hero Banner
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF), Color.White)))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Surface(color = blueMain, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                "MOOTO EXPRESS",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        Text("Fast Delivery", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                        Text("Anytime, Anywhere — Muqdisho", fontSize = 12.sp, color = Color(0xFF0369A1))
                                    }
                                    Surface(shape = CircleShape, color = blueMain.copy(alpha = 0.12f), modifier = Modifier.size(54.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.TwoWheeler, null, tint = blueMain, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Nooca Gaarsiinta
                        Text("Dooro Nooca Gaarsiinta", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DeliveryServiceCard("Send Package", "Documents", "📦", selectedServiceType == "package", { selectedServiceType = "package" }, Modifier.weight(1f))
                            DeliveryServiceCard("Send Shopping", "Market Items", "🛍️", selectedServiceType == "shopping", { selectedServiceType = "shopping" }, Modifier.weight(1f))
                            DeliveryServiceCard("Send Food", "Restaurant", "🍔", selectedServiceType == "food", { selectedServiceType = "food" }, Modifier.weight(1f))
                        }

                        // Form faahfaahinta
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shadowElevation = 1.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text("Faahfaahinta Gaarsiinta", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))

                                OutlinedTextField(
                                    value = pickupLocation,
                                    onValueChange = { pickupLocation = it },
                                    label = { Text("Goobta laga qaadayo (Pickup)") },
                                    placeholder = { Text("Tusaale: KM4, Hodan, Muqdisho") },
                                    leadingIcon = { Icon(Icons.Default.MyLocation, null, tint = blueMain) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = dropoffLocation,
                                    onValueChange = { dropoffLocation = it },
                                    label = { Text("Goobta la geynayo (Dropoff)") },
                                    placeholder = { Text("Tusaale: Taleex, Bakaaro, Sanaa...") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = Color(0xFFDC2626)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = recipientPhone,
                                    onValueChange = { recipientPhone = it },
                                    label = { Text("Telefoonka Qaataha (Recipient)") },
                                    placeholder = { Text("61XXXXXXX ama 68XXXXXXX") },
                                    leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF16A34A)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = packageNotes,
                                    onValueChange = { packageNotes = it },
                                    label = { Text("Tilmaam (Ikhtiyaari)") },
                                    placeholder = { Text("Waraaqo, Baakad yar...") },
                                    leadingIcon = { Icon(Icons.Default.Notes, null, tint = Color(0xFF64748B)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // Xawli & Qiimaha — chips ku jira scroll si overflow looga badbaadiyo
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shadowElevation = 1.dp) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Xawliga & Qiimaha", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = deliverySpeed == "express",
                                        onClick = { deliverySpeed = "express" },
                                        label = { Text("Express 20-30d", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                    FilterChip(
                                        selected = deliverySpeed == "standard",
                                        onClick = { deliverySpeed = "standard" },
                                        label = { Text("Standard 1 saac", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "\$${String.format("%.2f", fareAmount)}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = blueMain
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Qiimaha Gaarsiinta (USD)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                            }
                        }

                        // Fur CHECKOUT
                        Button(
                            onClick = {
                                if (dropoffLocation.isBlank()) {
                                    Toast.makeText(context, "Fadlan geli goobta la geynayo!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (recipientPhone.isBlank()) {
                                    Toast.makeText(context, "Fadlan geli telefoonka qaataha!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                currentStep = DeliveryStep.CHECKOUT
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = blueMain),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Gudub Checkout →", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // TALLAABADA 2: CHECKOUT — CIWAANKA, LACAG-BIXIN, XAQIIJIN
                // ══════════════════════════════════════════════════════════════
                DeliveryStep.CHECKOUT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Cinwaanka Shiri
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFBFDBFE)), shadowElevation = 2.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Home, null, tint = blueMain, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cinwaankaaga (Shiri Address)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                }
                                OutlinedTextField(
                                    value = shiriAddress,
                                    onValueChange = { shiriAddress = it },
                                    label = { Text("SHR-MG-XXXXX ama Cinwaanka Gurigaaga") },
                                    placeholder = { Text("Tusaale: SHR-MG-48291, Hodan") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null, tint = blueMain) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                // GPS info row
                                Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(10.dp)) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.GpsFixed, null, tint = blueMain, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "GPS: 2.0469, 45.3182 — Muqdisho (Darawalku wuxuu kuugu imaanayaa halkan)",
                                            fontSize = 11.sp,
                                            color = Color(0xFF0369A1),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                // Tilmaamaha Coddka (Voice Landmark)
                                Text("Tilmaanta Goobta (Voice Landmark)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                OutlinedTextField(
                                    value = voiceLandmark,
                                    onValueChange = { voiceLandmark = it },
                                    label = { Text("Tilmaanta Goobta (Ikhtiyaari)") },
                                    placeholder = { Text("Tusaale: Agta masaajidka, guri cagaar ah...") },
                                    leadingIcon = { Icon(Icons.Default.Mic, null, tint = Color(0xFF7C3AED)) },
                                    trailingIcon = {
                                        Surface(color = Color(0xFF7C3AED), shape = RoundedCornerShape(8.dp)) {
                                            Text(
                                                "🎤 15s",
                                                fontSize = 10.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // Tusaalooyinka diyaarsan
                                Text("Tusaalooyinka Degdegga ah:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Agta masaajidka",
                                        "Dukaan cas ah hortiisa",
                                        "Dabaq labaad",
                                        "Albaabka bidixda"
                                    ).forEach { example ->
                                        InputChip(
                                            selected = voiceLandmark == example,
                                            onClick = { voiceLandmark = example },
                                            label = { Text(example, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                        )
                                    }
                                }
                            }
                        }

                        // Lacag-bixinta
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shadowElevation = 1.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Payment, null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Habka Lacag-bixinta", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                }

                                // Payment chips row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PaymentChip("EVC+", "Hormuud", Color(0xFFDC2626), selectedPayment == "evc") { selectedPayment = "evc" }
                                    PaymentChip("eDahab", "Somtel", Color(0xFF0369A1), selectedPayment == "edahab") { selectedPayment = "edahab" }
                                    PaymentChip("ZAAD", "Telesom", Color(0xFF7C3AED), selectedPayment == "zaad") { selectedPayment = "zaad" }
                                    PaymentChip("Sahal", "Golis", Color(0xFFEA580C), selectedPayment == "sahal") { selectedPayment = "sahal" }
                                    PaymentChip("Cash", "Gacanta", Color(0xFF64748B), selectedPayment == "cash") { selectedPayment = "cash" }
                                }

                                // Lambarka lacag-bixinta
                                if (selectedPayment != "cash") {
                                    OutlinedTextField(
                                        value = payerPhone,
                                        onValueChange = { payerPhone = it },
                                        label = { Text("Lambarka Lacag-bixinta (Payer Phone)") },
                                        placeholder = { Text("61XXXXXXX") },
                                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF16A34A)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    // USSD code box
                                    Surface(color = Color(0xFFF0FDF4), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Color(0xFF86EFAC))) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text("Koodhka USSD (Nuqul/Copy):", fontSize = 11.sp, color = Color(0xFF64748B))
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                ussdCode,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF16A34A)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Koobaseynta dalabka
                        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF0F9FF), border = BorderStroke(1.dp, Color(0xFFBFDBFE))) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Koobaseynta Dalabka", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Divider(color = Color(0xFFBFDBFE), thickness = 0.8.dp)
                                SummaryRow("Nooca:", when(selectedServiceType) { "package" -> "📦 Send Package" "shopping" -> "🛍️ Send Shopping" else -> "🍔 Send Food" })
                                SummaryRow("Xawli:", if (deliverySpeed == "express") "⚡ Express (20-30 daqiiqo)" else "🕐 Standard (1 saac)")
                                SummaryRow("Destination:", dropoffLocation.take(30).ifBlank { "—" })
                                SummaryRow("Qiimaha:", "\$${String.format("%.2f", fareAmount)}")
                                SummaryRow("Lacag-bixin:", selectedPayment.uppercase())
                            }
                        }

                        // Xaqiiji & Dalbo
                        Button(
                            onClick = {
                                if (shiriAddress.isBlank() && voiceLandmark.isBlank()) {
                                    Toast.makeText(context, "Fadlan geli cinwaanka ama tilmaanta goobta!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (selectedPayment != "cash" && payerPhone.isBlank()) {
                                    Toast.makeText(context, "Fadlan geli lambarka lacag-bixinta!", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                orderTrackingId = "SH-DEL-" + (1000..9999).random()
                                handoverPin = (1000..9999).random().toString()
                                currentStep = DeliveryStep.MERCHANT
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("✅ Xaqiiji & Dalbo Hadda", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // TALLAABADA 3: MERCHANT AQBALIDA
                // ══════════════════════════════════════════════════════════════
                DeliveryStep.MERCHANT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(10.dp))

                        // Ding icon
                        Surface(shape = CircleShape, color = Color(0xFFFEF3C7), modifier = Modifier.size(80.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🔔", fontSize = 36.sp)
                            }
                        }

                        Text(
                            "Dalab Cusub Yimid!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Meheraddu waxay aragtaa dalabkaaga. Lacagtu waa la xaqiijinayaa...",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )

                        // Order summary card for merchant view
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(44.dp)) {
                                        Box(contentAlignment = Alignment.Center) { Text("🏪", fontSize = 22.sp) }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Meheradda • Merchant View", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("Dalabka #$orderTrackingId", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                }
                                Divider(color = Color(0xFFE2E8F0))
                                SummaryRow("Macmiilka:", recipientPhone.ifBlank { "Naqooyinka App" })
                                SummaryRow("Lacagta:", "\$${String.format("%.2f", fareAmount)}")
                                SummaryRow("Lacag-bixin:", selectedPayment.uppercase())
                                SummaryRow("Destination:", dropoffLocation.take(28).ifBlank { "—" })
                                if (voiceLandmark.isNotBlank()) {
                                    SummaryRow("Tilmaanta:", voiceLandmark.take(30))
                                }
                            }
                        }

                        if (!merchantApproved) {
                            Button(
                                onClick = { merchantApproved = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("✅ Oggolow Lacagta (Payment Approved)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        } else {
                            Surface(
                                color = Color(0xFFF0FDF4),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Lacagta Waa La Oggoladay! Diyaarinaya...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Button(
                                onClick = { currentStep = DeliveryStep.TRACKING },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) {
                                Icon(Icons.Default.DeliveryDining, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("🏍️ Darawalka Dir (Dispatch)", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // TALLAABADA 4: LIVE TRACKING & DARAWALKA
                // ══════════════════════════════════════════════════════════════
                DeliveryStep.TRACKING -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Darawalka Dispatch Alert
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = CircleShape, color = Color(0xFF0284C7), modifier = Modifier.size(44.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.TwoWheeler, null, tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Darawalka Ayaa Loo Qoondeeyay!", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                                    Text("Mooto Courier wuxuu kuu socda. Tracking: $orderTrackingId", fontSize = 11.sp, color = Color(0xFF475569))
                                }
                            }
                        }

                        // Live Map Placeholder (Low-Data Mode)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.radialGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD), Color(0xFF7DD3FC)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Map, null, tint = Color(0xFF0284C7), modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("🏍️ LIVE MAP — Darawalka", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                                    Text("Muqdisho, Degmada ${dropoffLocation.take(15).ifBlank { "Hodan" }}", fontSize = 11.sp, color = Color(0xFF0369A1))
                                    Spacer(Modifier.height(6.dp))
                                    Surface(color = Color(0xFF0284C7), shape = RoundedCornerShape(20.dp)) {
                                        Text("● Live — Dukaan ka 1.2km", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }

                        // Nidaamka Khad Yar (Low Data Mode)
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFFBEB), border = BorderStroke(1.dp, Color(0xFFFCD34D))) {
                            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SignalCellularAlt, null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Qoraalka Goobta (Low Data Mode)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                    Text("Darawalku wuxuu ku soo imaanayaa: ${voiceLandmark.ifBlank { dropoffLocation.take(25).ifBlank { "Destination-kaaga" } }}", fontSize = 11.sp, color = Color(0xFF78350F))
                                }
                            }
                        }

                        // Darawalka xiriirka
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), shadowElevation = 1.dp) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Xiriir Darawalka", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Call button
                                    Button(
                                        onClick = { Toast.makeText(context, "Waxaa la wacayo darawalka...", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("📞 Wac", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                    // WhatsApp button
                                    Button(
                                        onClick = { Toast.makeText(context, "WhatsApp fariinta la dirayaa...", Toast.LENGTH_SHORT).show() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(46.dp)
                                    ) {
                                        Icon(Icons.Default.Chat, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("💬 WhatsApp", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // Tracking info
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF0FDF4), border = BorderStroke(1.dp, Color(0xFF86EFAC))) {
                            Column(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                SummaryRow("Tracking ID:", orderTrackingId)
                                SummaryRow("Qiimaha:", "\$${String.format("%.2f", fareAmount)}")
                                SummaryRow("Lacag-bixin:", selectedPayment.uppercase())
                            }
                        }

                        Button(
                            onClick = { currentStep = DeliveryStep.HANDOVER },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        ) {
                            Icon(Icons.Default.Lock, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("🔐 Fur PIN Gacangelinta", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ══════════════════════════════════════════════════════════════
                // TALLAABADA 5: HANDOVER PIN
                // ══════════════════════════════════════════════════════════════
                DeliveryStep.HANDOVER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(Modifier.height(16.dp))

                        if (!isDelivered) {
                            // Customer sees PIN
                            Surface(shape = CircleShape, color = Color(0xFFF5F3FF), modifier = Modifier.size(80.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("🔐", fontSize = 36.sp) }
                            }
                            Text(
                                "Handover PIN",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Darawalka geli PIN-kan si uu u xaqiijiyo gaarsiinta",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )

                            // PIN display (customer side)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF7C3AED),
                                shadowElevation = 6.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text("PIN-kaaga (Sirta ah)", fontSize = 13.sp, color = Color.White.copy(0.8f))
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        handoverPin,
                                        fontSize = 54.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 10.sp
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text("Naga sii PIN-kan Darawalka kaliya!", fontSize = 11.sp, color = Color.White.copy(0.7f))
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            // Driver side — gelinta PIN
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TwoWheeler, null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Darawalka: Geli PIN-ka", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                    OutlinedTextField(
                                        value = driverEnteredPin,
                                        onValueChange = { if (it.length <= 4) driverEnteredPin = it },
                                        label = { Text("Geli PIN-ka 4-tirood") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (driverEnteredPin == handoverPin) {
                                                isDelivered = true
                                            } else {
                                                Toast.makeText(context, "❌ PIN-ku ma sax! Isku day mar kale.", Toast.LENGTH_SHORT).show()
                                                driverEnteredPin = ""
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("Xaqiiji PIN-ka", fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // ✅ DELIVERED!
                            Spacer(Modifier.height(24.dp))
                            Surface(shape = CircleShape, color = Color(0xFFF0FDF4), modifier = Modifier.size(100.dp)) {
                                Box(contentAlignment = Alignment.Center) { Text("✅", fontSize = 54.sp) }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Si Nabad ah Ayaa Laguugu Keenay!",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF16A34A),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Delivered ✅ — Dalabkaagu waa gaaray. Dakhligu wuxuu u dhacay jeebka darawalka.",
                                fontSize = 13.sp,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.5.dp, Color(0xFF86EFAC)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    SummaryRow("Tracking ID:", orderTrackingId)
                                    SummaryRow("Qiimaha:", "\$${String.format("%.2f", fareAmount)}")
                                    SummaryRow("Darawalka Dakhli:", "\$${String.format("%.2f", fareAmount * 0.8)}")
                                    SummaryRow("Xaalada:", "✅ Gaarsiinta Waxa Lagu Guuleystay")
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = onBackClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Icon(Icons.Default.Home, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("🏠 Ku Noqo Home", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

// ─── Helper Composables ───────────────────────────────────────────────────────

@Composable
private fun DeliveryServiceCard(
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
    val bgColor     = if (isSelected) Color(0xFFEFF6FF) else Color.White
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        color = bgColor,
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

@Composable
private fun PaymentChip(
    label: String,
    provider: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) color else Color(0xFFF8FAFC),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) color else Color(0xFFE2E8F0)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else Color(0xFF0F172A),
                maxLines = 1
            )
            Text(
                provider,
                fontSize = 9.sp,
                color = if (isSelected) Color.White.copy(0.8f) else Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
        Text(value, fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
