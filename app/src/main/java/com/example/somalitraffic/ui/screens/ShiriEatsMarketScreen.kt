package com.example.somalitraffic.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.somalitraffic.data.model.*
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.util.ArrivalSoundManager
import java.util.UUID

data class MerchantOrder(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val itemsSummary: String,
    val total: Double,
    var status: String, // "pending", "preparing", "ready", "delivered"
    val timeAgo: String,
    val handoverPin: String,
    val address: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriEatsMarketScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit,
    onNavigate: (String) -> Unit = {},
    initialMode: String = "customer"
) {
    val context = LocalContext.current

    // App Mode: "customer" or "merchant"
    var currentAppMode by remember(initialMode) { mutableStateOf(initialMode) }

    // Bilingual Language Toggle: true = Somali, false = English
    var isSomaliLanguage by remember { mutableStateOf(true) }

    // Customer Navigation: "home", "explore", "orders", "profile"
    var customerBottomTab by remember { mutableStateOf("home") }

    // Merchant Navigation: "dashboard", "orders", "products", "finance"
    var merchantBottomTab by remember { mutableStateOf("products") }

    // Search query
    var customerSearchQuery by remember { mutableStateOf("") }

    // Voice & Chat Order Assistant State
    var showVoiceOrderDialog by remember { mutableStateOf(false) }
    var voiceOrderIsRecording by remember { mutableStateOf(false) }
    var voiceOrderText by remember { mutableStateOf("") }
    var voiceOrderDuration by remember { mutableIntStateOf(0) }
    var voiceOrderPlacedSuccess by remember { mutableStateOf(false) }

    // Real MediaRecorder state for voice ordering
    val mediaRecorderRef = remember { mutableStateOf<MediaRecorder?>(null) }
    val mediaPlayerRef = remember { mutableStateOf<MediaPlayer?>(null) }
    val voiceRecordFilePath = remember { mutableStateOf("") }
    var audioPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> audioPermissionGranted = granted }

    // Master list of Categories (Admin extensible)
    val categories = remember {
        mutableStateListOf<ShiriBusinessCategory>().apply {
            addAll(ShiriMarketRepository.getDefaultCategories())
        }
    }

    // Master list of Stores
    val stores = remember {
        mutableStateListOf<ShiriMerchantStore>().apply {
            addAll(ShiriMarketRepository.getDefaultStores())
        }
    }

    // Selected Category Filter: "all" or category.id
    var selectedCategoryId by remember { mutableStateOf("all") }
    var selectedSubcategoryId by remember { mutableStateOf("all") }

    // Viewing specific store products
    var viewingStore by remember { mutableStateOf<ShiriMerchantStore?>(null) }

    // Cart items: productId -> quantity (per store)
    val cartItems = remember { mutableStateMapOf<String, Int>() }
    val cartStoreId by remember(cartItems.keys) {
        derivedStateOf {
            if (cartItems.isEmpty()) null
            else viewingStore?.id
        }
    }

    // Cart total & item count
    val cartTotal by derivedStateOf {
        val currentStore = viewingStore
        if (currentStore == null) 0.0
        else {
            cartItems.entries.sumOf { (prodId, qty) ->
                val p = currentStore.products.find { it.id == prodId }
                (p?.price ?: 0.0) * qty
            }
        }
    }
    val cartItemCount by derivedStateOf { cartItems.values.sum() }

    // Active order banner & Checkout states (Production: false by default)
    var hasActiveOrder by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var showLiveTracking by remember { mutableStateOf(false) }
    var trackingOrderNumber by remember { mutableStateOf("#SHR-28491") }
    var trackingHandoverPin by remember { mutableStateOf("4792") }
    var trackingStatus by remember { mutableStateOf("preparing") }
    var trackingStoreName by remember { mutableStateOf("Pizza House Mogadishu") }

    // Shiri Address Card Dialog State
    var showAddressCardDialog by remember { mutableStateOf(false) }
    var selectedShiriAddressCode by remember { mutableStateOf("SHR-MG-48291") }
    var selectedShiriDistrict by remember { mutableStateOf("Hodan, Muqdisho") }
    var selectedShiriStreet by remember { mutableStateOf("Waddo 21, Guri 12") }
    var selectedShiriGps by remember { mutableStateOf("2.0469° N, 45.3182° E") }

    // Voice Landmark Note State
    var hasVoiceLandmarkNote by remember { mutableStateOf(true) }
    var isRecordingVoiceNote by remember { mutableStateOf(false) }
    var isPlayingVoiceNote by remember { mutableStateOf(false) }
    var voiceRecordDuration by remember { mutableIntStateOf(15) }
    var voiceLandmarkText by remember { mutableStateOf("Gurigu waa midab cagaar ah, albaabka birta ah ee agtiisa masaajidka") }

    // Kitchen Alarm & Merchant Lifecycle State (Production: false by default, activates only when real order placed)
    var merchantKitchenAlarm by remember { mutableStateOf(false) }
    var merchantPaymentVerified by remember { mutableStateOf(false) }
    var orderReadyForCourier by remember { mutableStateOf(false) }

    // Courier PIN Keypad Input State
    var enteredCourierPin by remember { mutableStateOf("") }

    // Merchant Orders List (Dynamic - starts empty, populated when customers place orders)
    val merchantOrders = remember {
        mutableStateListOf<MerchantOrder>()
    }

    var selectedOrderForDetail by remember { mutableStateOf<MerchantOrder?>(null) }
    var selectedProductForEdit by remember { mutableStateOf<ShiriProductItem?>(null) }
    var editProductName by remember { mutableStateOf("") }
    var editProductPrice by remember { mutableStateOf("") }
    var editProductCategory by remember { mutableStateOf("") }
    var editProductInStock by remember { mutableStateOf(true) }

    var merchantOrderStatusFilter by remember { mutableStateOf("all") }
    var merchantProductSearch by remember { mutableStateOf("") }
    var merchantProductCategoryFilter by remember { mutableStateOf("all") }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawPhone by remember { mutableStateOf("0615551024") }
    var isStoreOpen by remember { mutableStateOf(true) }

    // Admin Category Manager Dialog
    var showAdminCategoryDialog by remember { mutableStateOf(false) }
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var newCatNameSo by remember { mutableStateOf("") }
    var newCatNameEn by remember { mutableStateOf("") }
    var newCatEmoji by remember { mutableStateOf("✨") }
    var newCatSubcategoriesRaw by remember { mutableStateOf("") }

    // Merchant Add Product Dialog
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var newProductPrice by remember { mutableStateOf("") }
    var newProductCategory by remember { mutableStateOf("cunto") }
    var newProductSubcategory by remember { mutableStateOf("") }
    var newProductExtraCategories by remember { mutableStateOf(setOf<String>()) }
    var newProductInStock by remember { mutableStateOf(true) }

    // Sound toggle state
    var isSoundEnabled by remember { mutableStateOf(true) }

    // Auto image pair for newly typed product
    val autoImagePair = remember(newProductName) {
        ShiriMarketRepository.getAutoImageForProduct(newProductName)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1. ADMIN CATEGORY MANAGER DIALOG
    // ─────────────────────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────────
    // VOICE & CHAT ORDERING DIALOG (Dalbo wax kasta cod ama qoraal)
    // ─────────────────────────────────────────────────────────────────────────────
    if (showVoiceOrderDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!voiceOrderIsRecording) showVoiceOrderDialog = false
            },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎙️", fontSize = 18.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSomaliLanguage) "Dalabka Codka & Qoraalka" else "Voice & Chat Ordering",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = if (isSomaliLanguage) "SHIRI Express Order" else "Fast Instant Order",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = { showVoiceOrderDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (voiceOrderPlacedSuccess) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("✅", fontSize = 32.sp)
                                Text(
                                    text = if (isSomaliLanguage) "Dalabkaaga waa la helay!" else "Order Received Successfully!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF166534)
                                )
                                Text(
                                    text = if (isSomaliLanguage) "Mooto Courier ayaa laguu soo diri doonaa isla markiiba." else "A courier has been dispatched immediately.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF15803D),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        // Voice Recording Card
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (voiceOrderIsRecording) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (voiceOrderIsRecording) Color(0xFFF87171) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (voiceOrderIsRecording) "🔴 Duubista codka ayaa socota... 0:${voiceOrderDuration}s" else "🎤 Riix badhanka si aad codka u duubto",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (voiceOrderIsRecording) Color(0xFFDC2626) else Color(0xFF334155)
                                )

                                // Real-time timer while recording
                                LaunchedEffect(voiceOrderIsRecording) {
                                    if (voiceOrderIsRecording) {
                                        while (voiceOrderIsRecording) {
                                            kotlinx.coroutines.delay(1000L)
                                            voiceOrderDuration++
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (voiceOrderIsRecording) {
                                            // STOP recording
                                            try {
                                                mediaRecorderRef.value?.stop()
                                                mediaRecorderRef.value?.release()
                                                mediaRecorderRef.value = null
                                            } catch (_: Exception) {}
                                            voiceOrderIsRecording = false
                                        } else {
                                            // START recording
                                            if (!audioPermissionGranted) {
                                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            } else {
                                                val file = java.io.File(context.cacheDir, "shiri_voice_${System.currentTimeMillis()}.3gp")
                                                voiceRecordFilePath.value = file.absolutePath
                                                val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                    MediaRecorder(context)
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    MediaRecorder()
                                                }
                                                recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
                                                recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                                                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                                recorder.setOutputFile(file.absolutePath)
                                                try {
                                                    recorder.prepare()
                                                    recorder.start()
                                                    mediaRecorderRef.value = recorder
                                                    voiceOrderIsRecording = true
                                                    voiceOrderDuration = 0
                                                } catch (e: Exception) {
                                                    recorder.release()
                                                    Toast.makeText(context, "Codka duubista ayaa fashilmay: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (voiceOrderIsRecording) Color(0xFFDC2626) else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text(
                                        text = if (voiceOrderIsRecording) "Jooji Duubista ⏹️" else "Bilaaw Duubista Codka 🎙️",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                // Playback button — shown after recording stops
                                if (!voiceOrderIsRecording && voiceOrderDuration > 0 && voiceRecordFilePath.value.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                mediaPlayerRef.value?.release()
                                                val player = MediaPlayer()
                                                player.setDataSource(voiceRecordFilePath.value)
                                                player.prepare()
                                                player.start()
                                                mediaPlayerRef.value = player
                                                player.setOnCompletionListener { it.release() }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Dhegaysiga wuu fashilmay", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                        modifier = Modifier.fillMaxWidth().height(38.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("▶ Dhegeyso Codka (${voiceOrderDuration}s)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }

                        // Chat / Text Order
                        Text(
                            text = if (isSomaliLanguage) "Ama ku qor qoraal waxaad rabto:" else "Or type what you need:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )

                        OutlinedTextField(
                            value = voiceOrderText,
                            onValueChange = { voiceOrderText = it },
                            placeholder = {
                                Text(
                                    text = if (isSomaliLanguage) "Tusaale: 'Waxaan rabaa 2 Pizza, 1 Canjeero iyo Cabitaan...'" else "E.g., 'I need 2 Pizzas, Burger and cold drink...'",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (voiceOrderPlacedSuccess) {
                    Button(
                        onClick = {
                            showVoiceOrderDialog = false
                            voiceOrderPlacedSuccess = false
                            voiceOrderText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Waayahay / Xir", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = {
                            if (voiceOrderText.isNotBlank() || voiceOrderDuration > 0 || voiceOrderIsRecording) {
                                // Stop recorder if still running when user submits
                                if (voiceOrderIsRecording) {
                                    try {
                                        mediaRecorderRef.value?.stop()
                                        mediaRecorderRef.value?.release()
                                        mediaRecorderRef.value = null
                                    } catch (_: Exception) {}
                                }
                                voiceOrderIsRecording = false
                                voiceOrderPlacedSuccess = true
                                val newOrderCode = "#SHR-V" + (1000..9999).random()
                                merchantOrders.add(0, MerchantOrder(
                                    id = UUID.randomUUID().toString(),
                                    orderNumber = newOrderCode,
                                    customerName = viewModel.currentUser.value?.fullName ?: "Macmiil Cod/Chat",
                                    customerPhone = viewModel.currentUser.value?.phoneNumber ?: "0612345678",
                                    itemsSummary = if (voiceOrderText.isNotBlank()) voiceOrderText else "Dalab Cod Ah (${voiceOrderDuration}s)",
                                    total = 10.0,
                                    status = "pending",
                                    timeAgo = "Hadda",
                                    handoverPin = (1000..9999).random().toString(),
                                    address = "$selectedShiriDistrict ($selectedShiriStreet)"
                                ))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSomaliLanguage) "Dir Dalabka Hadda 🚀" else "Submit Order Now 🚀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        )
    }

    if (showAdminCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAdminCategoryDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = Color(0xFFDC2626).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSomaliLanguage) "Maamulka Qeybaha (Admin)" else "Category Manager (Admin)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(onClick = { showAdminCategoryDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isSomaliLanguage)
                            "Halkan waxaad kaga dari kartaa, kaga beddeli kartaa ama kaga saari kartaa qeybaha ganacsiga ee app-ka ka muuqda."
                        else
                            "Add, edit, or delete business categories and subcategories shown across the platform.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    // Button: Add New Category
                    Button(
                        onClick = { showAddCategorySheet = !showAddCategorySheet },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(if (showAddCategorySheet) Icons.Default.ExpandLess else Icons.Default.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (showAddCategorySheet)
                                (if (isSomaliLanguage) "Xir Form-ka" else "Close Form")
                            else
                                (if (isSomaliLanguage) "+ Kudar Qeyb Cusub" else "+ Add New Category"),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    // Form: Add New Category
                    if (showAddCategorySheet) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = if (isSomaliLanguage) "Faahfaahinta Qeybta Cusub" else "New Category Details",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626)
                                )
                                OutlinedTextField(
                                    value = newCatNameSo,
                                    onValueChange = { newCatNameSo = it },
                                    label = { Text(if (isSomaliLanguage) "Magaca Soomaaliga" else "Somali Name") },
                                    placeholder = { Text("Tusaale: Gaadiidka & Baabuurta") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newCatNameEn,
                                    onValueChange = { newCatNameEn = it },
                                    label = { Text("English Name") },
                                    placeholder = { Text("e.g. Vehicles & Transport") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCatEmoji,
                                        onValueChange = { newCatEmoji = it },
                                        label = { Text("Emoji") },
                                        placeholder = { Text("🚗") },
                                        singleLine = true,
                                        modifier = Modifier.width(90.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    Text(
                                        text = "Astaanta/Emoji",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                OutlinedTextField(
                                    value = newCatSubcategoriesRaw,
                                    onValueChange = { newCatSubcategoriesRaw = it },
                                    label = { Text(if (isSomaliLanguage) "Qeybaha Hoose (Subcategories)" else "Subcategories") },
                                    placeholder = { Text("Tusaale: Kireynta Baabuurta, Qalabka Spare Parts, iwm") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Button(
                                    onClick = {
                                        if (newCatNameSo.isNotBlank()) {
                                            val subList = newCatSubcategoriesRaw.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotBlank() }
                                                .map { subName ->
                                                    ShiriBusinessSubcategory(
                                                        id = UUID.randomUUID().toString(),
                                                        nameSo = subName,
                                                        nameEn = subName,
                                                        emoji = newCatEmoji
                                                    )
                                                }
                                            val newCat = ShiriBusinessCategory(
                                                id = "cat_${System.currentTimeMillis()}",
                                                nameSo = newCatNameSo.trim(),
                                                nameEn = if (newCatNameEn.isNotBlank()) newCatNameEn.trim() else newCatNameSo.trim(),
                                                emoji = if (newCatEmoji.isNotBlank()) newCatEmoji.trim() else "✨",
                                                bannerImageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500&auto=format&fit=crop&q=80",
                                                subcategories = subList
                                            )
                                            categories.add(newCat)
                                            newCatNameSo = ""
                                            newCatNameEn = ""
                                            newCatEmoji = "✨"
                                            newCatSubcategoriesRaw = ""
                                            showAddCategorySheet = false
                                            Toast.makeText(context, "Qeyb cusub '${newCat.nameSo}' waa lagu daray!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isSomaliLanguage) "Kaydi Qeybta Cusub" else "Save Category",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Text(
                        text = if (isSomaliLanguage) "Qeybaha Hadda Jira (${categories.size})" else "Active Categories (${categories.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // List existing categories with Delete option
                    categories.forEach { cat ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = cat.emoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isSomaliLanguage) cat.nameSo else cat.nameEn,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${cat.subcategories.size} qeybaha hoose",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        if (categories.size > 1) {
                                            categories.remove(cat)
                                            Toast.makeText(context, "Waa laga saaray: ${cat.nameSo}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Lama saari karo qeybta ugu dambeysa", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAdminCategoryDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isSomaliLanguage) "Xir" else "Close", color = Color.White)
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 1B. SHIRI ADDRESS CARD MODAL (Guri Number Digital ah: SHR-MG-48291)
    // ─────────────────────────────────────────────────────────────────────────────
    if (showAddressCardDialog) {
        AlertDialog(
            onDismissRequest = { showAddressCardDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSomaliLanguage) "Ciwaankaaga Digital-ka ah" else "Shiri Digital Address",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Guri Number Rasmi ah",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = { showAddressCardDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Digital Code Card & QR Code Mockup
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Text(
                                    text = selectedShiriAddressCode,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFDC2626),
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }

                            // Simulated Visual QR Code Grid
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier.size(110.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.QrCode,
                                            contentDescription = "QR Code",
                                            tint = Color.White,
                                            modifier = Modifier.size(80.dp)
                                        )
                                        Text(
                                            "SHIRI SCAN",
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Koodhka rasmiga ah ee aqoonsiga goobtaada",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Address Detail Fields
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Degmada:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(selectedShiriDistrict, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Waddada & Guriga:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(selectedShiriStreet, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("GPS Toos ah:", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text(selectedShiriGps, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A))
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                            Text(
                                text = "📍 Tilmaan: Ka soo horjeedka Masjidka Nuur, albaabka cagaaran.",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    // Copy & Share buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Ciwaanka $selectedShiriAddressCode waa la koobiyeeyay!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F172A))
                            Spacer(Modifier.width(4.dp))
                            Text("Koobiye", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }

                        Button(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Ciwaankeyga Shiri waa $selectedShiriAddressCode ($selectedShiriDistrict - $selectedShiriStreet). GPS: $selectedShiriGps")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "La wadaag Ciwaanka"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            modifier = Modifier.weight(1f).height(40.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(Modifier.width(4.dp))
                            Text("La Wadaag", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAddressCardDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSomaliLanguage) "Xaqiiji Ciwaanka" else "Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 2. CHECKOUT MODAL (Dynamic for ANY store & its items)
    // ─────────────────────────────────────────────────────────────────────────────
    if (showCheckout && viewingStore != null) {
        val currentStore = viewingStore!!
        val deliveryFee = currentStore.deliveryFee
        val serviceFee = ShiriMarketRepository.currentPricing.serviceFeeUsd
        val checkoutTotal = cartTotal + deliveryFee + serviceFee
        var checkoutPayment by remember { mutableStateOf("evc") }
        var checkoutPayerPhone by remember { mutableStateOf("") }
        var checkoutCopied by remember { mutableStateOf(false) }

        val ussdCode = when (checkoutPayment) {
            "evc" -> "*712*618092060*${String.format("%.2f", checkoutTotal)}#"
            "edahab" -> "*789*618092060*${String.format("%.2f", checkoutTotal)}#"
            "zaad" -> "*920*618092060*${String.format("%.2f", checkoutTotal)}#"
            "sahal" -> "*889*618092060*${String.format("%.2f", checkoutTotal)}#"
            else -> "Caddaan (Cash on Delivery)"
        }

        AlertDialog(
            onDismissRequest = { showCheckout = false },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFDC2626).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(34.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(currentStore.emoji, fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isSomaliLanguage) "Gudubka Dalabka" else "Order Checkout",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = currentStore.name,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    IconButton(onClick = { showCheckout = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Shiri Address Destination Selector Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddressCardDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isSomaliLanguage) "CIWAANKA LAGUU KEENAYO (SHIRI ADDRESS)" else "DELIVERY ADDRESS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "$selectedShiriAddressCode • $selectedShiriDistrict",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                            Text(
                                text = "Bedel →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }

                    // Duubista Codka Tilmaamaha (Voice Landmark Note)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = if (isSomaliLanguage) "TILMAANTA CODKA (VOICE NOTE)" else "VOICE LANDMARK NOTE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDC2626)) {
                                    Text(
                                        text = if (isRecordingVoiceNote) "Duubista... 0:${voiceRecordDuration}" else "15-Sec",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "\"$voiceLandmarkText\"",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        isRecordingVoiceNote = !isRecordingVoiceNote
                                        if (!isRecordingVoiceNote) {
                                            hasVoiceLandmarkNote = true
                                            Toast.makeText(context, "Codka tilmaanta guriga waa la duubay (15s)!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRecordingVoiceNote) Color(0xFFDC2626) else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isRecordingVoiceNote) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = Color.White
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = if (isRecordingVoiceNote) "Jooji" else "Duub Cod (15s)",
                                        fontSize = 10.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (hasVoiceLandmarkNote) {
                                    OutlinedButton(
                                        onClick = { isPlayingVoiceNote = !isPlayingVoiceNote },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, Color(0xFFDC2626))
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingVoiceNote) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = Color(0xFFDC2626)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (isPlayingVoiceNote) "Dhegeyso..." else "Dhageyso (0:15)",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFDC2626),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Selected Items Summary
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isSomaliLanguage) "ALAABTA AAD QAADATAY ($cartItemCount)" else "ORDER ITEMS ($cartItemCount)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF64748B)
                            )
                            cartItems.forEach { (prodId, qty) ->
                                val p = currentStore.products.find { it.id == prodId }
                                if (p != null) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${p.emoji} ${p.name} x$qty", fontSize = 11.5.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                        Text("$${String.format("%.2f", p.price * qty)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    }

                    // Payment Method selector
                    Text(
                        text = if (isSomaliLanguage) "HABKA LACAG-BIXINTA" else "PAYMENT METHOD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF64748B)
                    )
                    val paymentOptions = listOf(
                        Triple("evc", "EVC Plus", "Hormuud • 📱"),
                        Triple("edahab", "eDahab", "Somtel • 💳"),
                        Triple("zaad", "ZAAD", "Telesom • 📶"),
                        Triple("sahal", "Sahal", "Golis • 📲"),
                        Triple("cash", "Cash", "Caddaan • 💵")
                    )
                    paymentOptions.chunked(2).forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { (id, name, prov) ->
                                val selected = checkoutPayment == id
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFDC2626) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f).clickable { checkoutPayment = id }
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) Color(0xFFDC2626) else Color(0xFF0F172A))
                                        Text(prov, fontSize = 9.5.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }

                    // USSD Prompt if not cash
                    if (checkoutPayment != "cash") {
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFFBEB), border = BorderStroke(1.dp, Color(0xFFFDE68A)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isSomaliLanguage) "U Dir Lacagta Number-kan (USSD)" else "Direct Merchant Payment USSD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                        Text(ussdCode, fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                    Button(
                                        onClick = { checkoutCopied = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(if (checkoutCopied) "✓ La Qaaday" else "Koobiye", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                OutlinedTextField(
                                    value = checkoutPayerPhone,
                                    onValueChange = { checkoutPayerPhone = it },
                                    label = { Text(if (isSomaliLanguage) "Numberkaaga Lacagta Diraya" else "Payer Phone Number", fontSize = 11.sp) },
                                    placeholder = { Text("061xxxxxxx") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Bill Breakdown
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isSomaliLanguage) "XISAABTA" else "TOTAL SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isSomaliLanguage) "Alaabta" else "Subtotal", fontSize = 11.5.sp, color = Color(0xFF475569))
                                Text("$${String.format("%.2f", cartTotal)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isSomaliLanguage) "Gaarsiinta (Delivery)" else "Delivery Fee", fontSize = 11.5.sp, color = Color(0xFF475569))
                                Text("$${String.format("%.2f", deliveryFee)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isSomaliLanguage) "Adeegga SHIRI" else "Service Fee", fontSize = 11.5.sp, color = Color(0xFF475569))
                                Text("$${String.format("%.2f", serviceFee)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isSomaliLanguage) "WADARTA GUUD" else "TOTAL", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Text("$${String.format("%.2f", checkoutTotal)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pin = (1000..9999).random().toString()
                        val orderId = "SHR-MG-" + (10000..99999).random()
                        trackingHandoverPin = pin
                        trackingOrderNumber = orderId
                        trackingStatus = "preparing"
                        trackingStoreName = currentStore.name
                        merchantKitchenAlarm = true
                        merchantPaymentVerified = false
                        orderReadyForCourier = false
                        enteredCourierPin = ""
                        showCheckout = false
                        showLiveTracking = true
                        hasActiveOrder = true

                        val itemsSummaryStr = cartItems.entries.joinToString(", ") { (prodId, qty) ->
                            val p = currentStore.products.find { it.id == prodId }
                            "${p?.name ?: "Alaab"} x$qty"
                        }
                        merchantOrders.add(0, MerchantOrder(
                            id = orderId,
                            orderNumber = "#$orderId",
                            customerName = viewModel.currentUser.value?.fullName ?: "Macmiil Shiri",
                            customerPhone = viewModel.currentUser.value?.phoneNumber ?: "0612345678",
                            itemsSummary = itemsSummaryStr,
                            total = cartTotal + currentStore.deliveryFee,
                            status = "pending",
                            timeAgo = "Hadda",
                            handoverPin = pin,
                            address = "$selectedShiriDistrict ($selectedShiriStreet)"
                        ))

                        cartItems.clear()
                        Toast.makeText(context, "Dalabkaagii waa la xaqiijiyay! Digniinta Jikada (Kitchen Alarm) ayaa dirtay.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isSomaliLanguage) "Xaqiiji & Dalbo Hadda →" else "Confirm & Place Order →",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckout = false }) {
                    Text(if (isSomaliLanguage) "Ka Noqo" else "Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 3. LIVE TRACKING MODAL (Handover PIN & Delivery Flow)
    // ─────────────────────────────────────────────────────────────────────────────
    if (showLiveTracking) {
        AlertDialog(
            onDismissRequest = { showLiveTracking = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(22.dp),
            title = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF22C55E).copy(alpha = 0.2f)) {
                            Text(
                                text = "LIVE TRACKING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF4ADE80),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        IconButton(onClick = { showLiveTracking = false }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(trackingOrderNumber, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(
                        text = if (isSomaliLanguage) "Laga keenayo: $trackingStoreName" else "From: $trackingStoreName",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Handover PIN Card
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF1E293B), border = BorderStroke(1.5.dp, Color(0xFFDC2626)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isSomaliLanguage) "KOODHKA SIRTA AH (HANDOVER PIN)" else "HANDOVER SECURITY PIN",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                            Text(trackingHandoverPin, fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626), letterSpacing = 8.sp)
                            Text(
                                text = if (isSomaliLanguage) "Darawalka mootada sii marka uu xirmada kuugu keeno" else "Give this PIN to the courier upon delivery",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Courier Info & Voice Landmark Note Audio Playback
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color(0xFFDC2626).copy(alpha = 0.2f), modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("🏍️", fontSize = 18.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("Axmed Xasan (Mooto)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("SHR-MC-104 • ★ 4.9 • Dakhliga: +$6.50", fontSize = 10.sp, color = Color(0xFF4ADE80))
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+252612345678"))
                                            context.startActivity(callIntent)
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            // Dhageysiga Codka Macmiilka (Voice Landmark Player)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF0F172A),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isPlayingVoiceNote = !isPlayingVoiceNote
                                        Toast.makeText(context, if (isPlayingVoiceNote) "Tilmaanta codka macmiilka ayaa dhaceysa..." else "Waa la hakiyay", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isPlayingVoiceNote) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Column {
                                            Text("Codka Tilmaanta Guriga (15s)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text(voiceLandmarkText, fontSize = 9.sp, color = Color(0xFF94A3B8), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    Text(if (isPlayingVoiceNote) "0:15 / 0:15" else "▶ Yeeri", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }

                    // Handover PIN Verification Keypad (Courier Delivery Completion)
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, if (trackingStatus == "delivered") Color(0xFF22C55E) else Color(0xFF334155)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "PIN-KA GACAN-GELINTA (CONFIRMATION PIN)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24)
                            )

                            if (trackingStatus == "delivered") {
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF22C55E).copy(alpha = 0.2f), modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("✓ WAA LA GAARSIIYAY! (DELIVERED)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF4ADE80))
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isSomaliLanguage) "Codkaaga PIN-ka:" else "Your Delivery PIN:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFCBD5E1)
                                            )
                                            Text(
                                                text = if (isSomaliLanguage) "U aqri darawalka marka uu kuu keeno" else "Read aloud to your driver upon arrival",
                                                fontSize = 9.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFBBF24),
                                            modifier = Modifier.padding(start = 8.dp)
                                        ) {
                                            Text(
                                                text = trackingHandoverPin.ifEmpty { "4792" },
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A),
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = if (isSomaliLanguage) "⚠️ Darawalka ha siin PIN-kan ilaa aad xirmada gacanta ku dhigto." else "⚠️ Do not share this PIN until you receive your items.",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }

                    // Status Timeline
                    val statuses = listOf(
                        Triple("preparing", if (isSomaliLanguage) "Diyaarinta" else "Preparing", "👨‍🍳"),
                        Triple("courier_assigned", if (isSomaliLanguage) "Mooto loo xil-saaray" else "Courier Assigned", "🏍️"),
                        Triple("in_transit", if (isSomaliLanguage) "Waddada ku soo jira" else "In Transit", "📍"),
                        Triple("delivered", if (isSomaliLanguage) "Waa la gaarsiiyay" else "Delivered", "✅")
                    )
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF1E293B), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            statuses.forEach { (stKey, stLabel, stIcon) ->
                                val isDone = when (trackingStatus) {
                                    "delivered" -> true
                                    "in_transit" -> stKey != "delivered"
                                    "courier_assigned" -> stKey == "preparing" || stKey == "courier_assigned"
                                    else -> stKey == "preparing"
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = if (isDone) Color(0xFF22C55E) else Color(0xFF334155), modifier = Modifier.size(24.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(stIcon, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(stLabel, fontSize = 12.sp, fontWeight = if (isDone) FontWeight.Bold else FontWeight.Normal, color = if (isDone) Color.White else Color(0xFF64748B))
                                }
                            }
                        }
                    }


                }
            },
            confirmButton = {
                Button(
                    onClick = { showLiveTracking = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isSomaliLanguage) "Waan Fahmay" else "OK, Got It", color = Color.White)
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 4. MERCHANT ADD PRODUCT DIALOG (with multi-category support)
    // ─────────────────────────────────────────────────────────────────────────────
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFFDC2626).copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AddBusiness, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSomaliLanguage) "+ Kudar Alaab Cusub" else "+ Add New Product",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Product Name
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text(if (isSomaliLanguage) "Magaca Alaabta" else "Product Name") },
                        placeholder = { Text("Tusaale: Panadol Extra, Bariis, Pizza...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Image preview
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth().height(90.dp)) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(74.dp), color = Color(0xFFE2E8F0)) {
                                AsyncImage(model = autoImagePair.first, contentDescription = newProductName, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (newProductName.isNotBlank()) newProductName else "Magaca qor...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("✓ Auto Image & Emoji: ${autoImagePair.second}", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Price
                    OutlinedTextField(
                        value = newProductPrice,
                        onValueChange = { newProductPrice = it },
                        label = { Text(if (isSomaliLanguage) "Qiimaha ($ USD)" else "Price ($ USD)") },
                        placeholder = { Text("Tusaale: 4.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Category Selection
                    Text(
                        text = if (isSomaliLanguage) "Dooro Qeybta Guud (Primary Category):" else "Primary Category:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            FilterChip(
                                selected = newProductCategory == cat.id,
                                onClick = { newProductCategory = cat.id },
                                label = { Text("${cat.emoji} ${if (isSomaliLanguage) cat.nameSo else cat.nameEn}", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Subcategory Selection (based on selected category)
                    val catObj = categories.find { it.id == newProductCategory }
                    if (catObj != null && catObj.subcategories.isNotEmpty()) {
                        Text(
                            text = if (isSomaliLanguage) "Dooro Qeybta Hoose (Subcategory):" else "Subcategory:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            catObj.subcategories.forEach { sub ->
                                FilterChip(
                                    selected = newProductSubcategory == sub.id,
                                    onClick = { newProductSubcategory = sub.id },
                                    label = { Text("${sub.emoji} ${if (isSomaliLanguage) sub.nameSo else sub.nameEn}", fontSize = 10.5.sp) }
                                )
                            }
                        }
                    }

                    // Multi-Category: Qeybo Dheeraad ah
                    Text(
                        text = if (isSomaliLanguage) "Qeybo Dheeraad ah (Extra Categories):" else "Extra Categories (Multi-service):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.filter { it.id != newProductCategory }.forEach { cat ->
                            val isSelected = newProductExtraCategories.contains(cat.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    newProductExtraCategories = if (isSelected) newProductExtraCategories - cat.id else newProductExtraCategories + cat.id
                                },
                                label = { Text("${cat.emoji} ${if (isSomaliLanguage) cat.nameSo else cat.nameEn}", fontSize = 10.5.sp) }
                            )
                        }
                    }

                    // Stock toggle
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isSomaliLanguage) "Alaabta Bakhaarka ma ku jirtaa (In Stock)?" else "Is product in stock?", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = newProductInStock,
                            onCheckedChange = { newProductInStock = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF16A34A))
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPrice = newProductPrice.toDoubleOrNull() ?: 0.0
                        if (newProductName.isNotBlank() && parsedPrice > 0.0) {
                            val newItem = ShiriProductItem(
                                name = newProductName.trim(),
                                price = parsedPrice,
                                category = newProductCategory,
                                subCategory = newProductSubcategory,
                                imageUrl = autoImagePair.first,
                                emoji = autoImagePair.second,
                                inStock = newProductInStock
                            )
                            val targetStore = viewingStore ?: stores.firstOrNull()
                            if (targetStore != null) {
                                val updatedStores = stores.map { s ->
                                    if (s.id == targetStore.id) {
                                        s.copy(products = s.products + newItem)
                                    } else s
                                }
                                stores.clear()
                                stores.addAll(updatedStores)
                                if (viewingStore != null) {
                                    viewingStore = stores.find { it.id == viewingStore!!.id }
                                }
                            }
                            newProductName = ""
                            newProductPrice = ""
                            showAddProductDialog = false
                            Toast.makeText(context, "Alaab '${newItem.name}' waa lagu daray!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Fadlan gali magaca iyo qiimaha saxda ah", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(if (isSomaliLanguage) "Kaydi Alaabta" else "Save Product", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text(if (isSomaliLanguage) "Ka Noqo" else "Cancel", color = Color(0xFF64748B))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 5. MERCHANT ORDER DETAIL DIALOG
    // ─────────────────────────────────────────────────────────────────────────────
    if (selectedOrderForDetail != null) {
        val ord = selectedOrderForDetail!!
        AlertDialog(
            onDismissRequest = { selectedOrderForDetail = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ord.orderNumber, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Spacer(Modifier.width(8.dp))
                            val (badgeBg, badgeColor, badgeText) = when (ord.status) {
                                "pending" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), "Cusub")
                                "preparing" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Diyaarinta")
                                "ready" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "Diyaar ah")
                                else -> Triple(Color(0xFFF0FDF4), Color(0xFF16A34A), "Delivered")
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = badgeBg) {
                                Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(ord.timeAgo, fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = { selectedOrderForDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Customer & Phone Card
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(ord.customerName, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(ord.customerPhone, fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            IconButton(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ord.customerPhone}"))
                                    context.startActivity(callIntent)
                                },
                                modifier = Modifier.size(34.dp).background(Color(0xFF22C55E).copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Address
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(ord.address, fontSize = 11.sp, color = Color(0xFF334155), fontWeight = FontWeight.Medium)
                        }
                    }

                    // Items & Total
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ALAABTA DALABKA:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            Text(ord.itemsSummary, fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Wadarta:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                Text("$${String.format("%.2f", ord.total)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                        }
                    }

                    // Handover PIN
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF0F172A), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Handover Security PIN:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(ord.handoverPin, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 3.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    when (ord.status) {
                        "pending" -> {
                            Button(
                                onClick = {
                                    val idx = merchantOrders.indexOfFirst { it.id == ord.id }
                                    if (idx != -1) {
                                        merchantOrders[idx] = ord.copy(status = "preparing")
                                    }
                                    selectedOrderForDetail = null
                                    Toast.makeText(context, "Dalabka waa la aqbalay, diyaarinta ayaa bilaabatay!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✓ Oggolow Lacagta & Bilow Diyaarinta", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        "preparing" -> {
                            Button(
                                onClick = {
                                    val idx = merchantOrders.indexOfFirst { it.id == ord.id }
                                    if (idx != -1) {
                                        merchantOrders[idx] = ord.copy(status = "ready")
                                    }
                                    selectedOrderForDetail = null
                                    Toast.makeText(context, "Mootada ugu dhow ayaa loo yeeray!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🏍️ Waa Diyaar, U Yeer Mooto", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        "ready" -> {
                            Button(
                                onClick = {
                                    val idx = merchantOrders.indexOfFirst { it.id == ord.id }
                                    if (idx != -1) {
                                        merchantOrders[idx] = ord.copy(status = "delivered")
                                    }
                                    selectedOrderForDetail = null
                                    Toast.makeText(context, "Dalabka waa la gacan-galiyay!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("✅ Xaqiiji Gacan-gelinta", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFF0FDF4), modifier = Modifier.fillMaxWidth()) {
                                Text("Dalabkan waa la dhammeystiray (Delivered)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A), textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedOrderForDetail = null }) {
                    Text("Xir", color = Color(0xFF64748B))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 6. PRODUCT DETAIL & EDITOR DIALOG (Interactive Product Management)
    // ─────────────────────────────────────────────────────────────────────────────
    if (selectedProductForEdit != null) {
        val prod = selectedProductForEdit!!
        AlertDialog(
            onDismissRequest = { selectedProductForEdit = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF2F2), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(prod.emoji, fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Wax Ka Bedel Alaabta", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text(prod.category.uppercase(), fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }
                    IconButton(onClick = { selectedProductForEdit = null }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editProductName,
                        onValueChange = { editProductName = it },
                        label = { Text("Magaca Alaabta", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editProductPrice,
                        onValueChange = { editProductPrice = it },
                        label = { Text("Qiimaha ($)", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editProductCategory,
                        onValueChange = { editProductCategory = it },
                        label = { Text("Qeybta (Category)", fontSize = 11.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Stock Toggle Switch
                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Xaaladda Kaydka (In Stock)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text(if (editProductInStock) "Macmiilku wuu dalban karaa" else "Waa ka dhammaatay dukaanka", fontSize = 10.5.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = editProductInStock,
                                onCheckedChange = { editProductInStock = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF16A34A))
                            )
                        }
                    }

                    // Delete button
                    Button(
                        onClick = {
                            val targetStore = viewingStore ?: stores.firstOrNull()
                            if (targetStore != null) {
                                val updated = stores.map { s ->
                                    if (s.id == targetStore.id) {
                                        s.copy(products = s.products.filter { it.id != prod.id })
                                    } else s
                                }
                                stores.clear()
                                stores.addAll(updated)
                            }
                            selectedProductForEdit = null
                            Toast.makeText(context, "Alaab '${prod.name}' waa la tirtiray!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tirtir Alaabtan", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = editProductPrice.toDoubleOrNull() ?: prod.price
                        val targetStore = viewingStore ?: stores.firstOrNull()
                        if (targetStore != null) {
                            val updated = stores.map { s ->
                                if (s.id == targetStore.id) {
                                    s.copy(products = s.products.map { p ->
                                        if (p.id == prod.id) {
                                            p.copy(
                                                name = editProductName.trim().ifBlank { p.name },
                                                price = parsed,
                                                category = editProductCategory.trim().ifBlank { p.category },
                                                inStock = editProductInStock
                                            )
                                        } else p
                                    })
                                } else s
                            }
                            stores.clear()
                            stores.addAll(updated)
                        }
                        selectedProductForEdit = null
                        Toast.makeText(context, "Alaabta waa la cusbooneysiiyay!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Keydi Isbedelka", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProductForEdit = null }) {
                    Text("Ka Noqo", color = Color(0xFF64748B))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 7. MERCHANT FINANCE WITHDRAWAL DIALOG
    // ─────────────────────────────────────────────────────────────────────────────
    if (showWithdrawDialog) {
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Kala Bax Dakhliga (Payout)", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Text("Dakhliga la heli karo: $385.20", fontSize = 11.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = { showWithdrawDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Dooro Adeegga Lacagta:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("EVC Plus", "eDahab", "ZAAD").forEach { method ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(method, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = withdrawPhone,
                        onValueChange = { withdrawPhone = it },
                        label = { Text("Taleefanka Lacagta Lagu Dirayo", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showWithdrawDialog = false
                        Toast.makeText(context, "Dakhliga $385.20 waxaa si guul leh loogu diray $withdrawPhone!", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xaqiiji Kala Bixidda →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) {
                    Text("Ka Noqo", color = Color(0xFF64748B))
                }
            }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // MAIN SCAFFOLD & TOP APP BAR
    // ─────────────────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            Surface(
                color = if (currentAppMode == "customer") Color.White else Color(0xFF0F172A),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // TOP BAR: DOORDASH STYLE (Muqdisho location dropdown + Notification Bell)
                    if (currentAppMode == "customer" && !showLiveTracking && viewingStore == null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Location Selector: 📍 Muqdisho ⌵
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showAddressCardDialog = true }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedShiriDistrict.ifBlank { "Muqdisho" },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            // Notification Bell with badge '2'
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable {
                                        Toast.makeText(context, "Fariimo: Dalabkaaga wuu socdaa (Live)", Toast.LENGTH_SHORT).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier.size(20.dp)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "2",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    } else if (currentAppMode == "customer" && showLiveTracking) {
                        // Top bar for Live Order Tracking
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { showLiveTracking = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Dib u noqo", tint = Color(0xFF0F172A))
                            }
                            Text(
                                text = "Dalabkaaga $trackingOrderNumber",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(Modifier.size(36.dp))
                        }
                    } else if (currentAppMode == "customer" && viewingStore != null) {
                        // Top bar for store detail handled inside storeDetail banner
                    } else {
                        // Merchant / Driver Top Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🏪", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Restaurant Dashboard",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Al-Furqan Restaurant • Verified ✔",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(text = "Muqdisho", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentAppMode == "customer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomerNavTabItem(
                            icon = Icons.Default.Home,
                            label = if (isSomaliLanguage) "Hoyga" else "Home",
                            isSelected = customerBottomTab == "home" && !showLiveTracking,
                            badge = null,
                            onClick = { customerBottomTab = "home"; viewingStore = null; showLiveTracking = false }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.ReceiptLong,
                            label = if (isSomaliLanguage) "Dalabaadka" else "Orders",
                            isSelected = customerBottomTab == "orders" || showLiveTracking,
                            badge = if (hasActiveOrder) "1" else null,
                            onClick = { customerBottomTab = "orders"; showLiveTracking = true }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Favorite,
                            label = if (isSomaliLanguage) "Jeceshahay" else "Favorites",
                            isSelected = customerBottomTab == "favorites",
                            badge = null,
                            onClick = { customerBottomTab = "favorites"; viewingStore = null; showLiveTracking = false }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Person,
                            label = if (isSomaliLanguage) "Koontada" else "Profile",
                            isSelected = customerBottomTab == "profile",
                            badge = null,
                            onClick = { customerBottomTab = "profile"; onNavigate("profile_settings") }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CustomerNavTabItem(
                            icon = Icons.Default.Storefront,
                            label = "Dashboard",
                            isSelected = merchantBottomTab == "dashboard",
                            badge = null,
                            onClick = { merchantBottomTab = "dashboard" }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.ShoppingBag,
                            label = "Orders",
                            isSelected = merchantBottomTab == "orders",
                            badge = "3",
                            onClick = { merchantBottomTab = "orders" }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Inventory2,
                            label = "Products",
                            isSelected = merchantBottomTab == "products",
                            badge = null,
                            onClick = { merchantBottomTab = "products" }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.AttachMoney,
                            label = "Finance",
                            isSelected = merchantBottomTab == "finance",
                            badge = null,
                            onClick = { merchantBottomTab = "finance" }
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->

        if (currentAppMode == "customer") {

            // ═══════════════════════════════════════════════════════════════════════
            // 1. LIVE ORDER TRACKING SCREEN (Screen 4 of Image 1)
            // ═══════════════════════════════════════════════════════════════════════
            if (showLiveTracking) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 4-Step Progress Horizontal Stepper
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val steps = listOf("La Shaqeynayo", "Waa la Diyaarinayaa", "Waa la Bixiyay", "La Keenay")
                                steps.forEachIndexed { index, _ ->
                                    val isDone = index <= when (trackingStatus) {
                                        "delivered" -> 3
                                        "in_transit" -> 2
                                        "preparing", "courier_assigned" -> 1
                                        else -> 0
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDone) Color(0xFFDC2626) else Color(0xFFE2E8F0),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (isDone) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                    if (index < steps.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(3.dp)
                                                .background(if (index < when (trackingStatus) { "delivered" -> 3; "in_transit" -> 2; else -> 1 }) Color(0xFFDC2626) else Color(0xFFE2E8F0))
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("La Shaqeynayo", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center, modifier = Modifier.width(72.dp))
                                Text("Waa la Diyaarinayaa", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center, modifier = Modifier.width(76.dp))
                                Text("Waa la Bixiyay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center, modifier = Modifier.width(72.dp))
                                Text("La Keenay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), textAlign = TextAlign.Center, modifier = Modifier.width(72.dp))
                            }
                        }
                    }

                    // Live Route Map Canvas
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?w=600&auto=format&fit=crop&q=80",
                                contentDescription = "Delivery Route Map",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(modifier = Modifier.fillMaxSize().background(Color(0x33000000)))

                            // Floating ETA badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🏍️", fontSize = 14.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "5 min ≈ 1.2 km",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }

                            // House icon
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Courier Card: Ahmed Hassan ⭐ 4.9
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("👨‍✈️", fontSize = 24.sp)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Ahmed Hassan", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("4.9", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                                        Spacer(Modifier.width(2.dp))
                                        Text("⭐", fontSize = 10.sp)
                                        Spacer(Modifier.width(6.dp))
                                        Text("• Mooto Express", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+252612345678"))
                                        context.startActivity(callIntent)
                                    },
                                    modifier = Modifier.size(38.dp).background(Color(0xFFF1F5F9), CircleShape)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Wac", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = {
                                        val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:+252612345678"))
                                        context.startActivity(smsIntent)
                                    },
                                    modifier = Modifier.size(38.dp).background(Color(0xFFF1F5F9), CircleShape)
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "Fariin", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // ETA Box
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Waqtiga la qiyaasay",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "5 min",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "$trackingStoreName • PIN: $trackingHandoverPin",
                                fontSize = 11.sp,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Button: Faahfaahin Dalabka
                    Button(
                        onClick = {
                            Toast.makeText(context, "Dalabka: $trackingOrderNumber • Handover PIN: $trackingHandoverPin", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text(
                            text = "Faahfaahin Dalabka",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }
            } else if (viewingStore != null) {
                // ═══════════════════════════════════════════════════════════════════════
                // 2. RESTAURANT DETAIL SCREEN (Screen 3 of Image 1)
                // ═══════════════════════════════════════════════════════════════════════
                val storeDetail = viewingStore!!
                var detailTab by remember { mutableStateOf("menu") }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFF8FAFC))
                ) {
                    // Top Hero Cover Image with Back, Share, and Heart buttons
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                    ) {
                        AsyncImage(
                            model = storeDetail.imageUrl,
                            contentDescription = storeDetail.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Top gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x66000000),
                                            Color.Transparent,
                                            Color(0x33000000)
                                        )
                                    )
                                )
                        )

                        // Top Row: Back arrow (left), Share & Heart (right)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewingStore = null; cartItems.clear() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0x80000000), CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "Ka dalbo cunto macaan ${storeDetail.name} gudaha SHIRI App!")
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "La wadaag"))
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0x80000000), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        Toast.makeText(context, "${storeDetail.name} waa lagu daray Favorites!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Color(0x80000000), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "Favorite",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Store Info White Card
                    Surface(
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Title Row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = storeDetail.name,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Subtitle Rating & Reviews
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⭐", fontSize = 12.sp)
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "${storeDetail.rating} (2.5k reviews)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("•", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "⏱️ ${storeDetail.deliveryTimeMin} min",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            // Cuisine Tags
                            Text(
                                text = "Cunto • Somali Food • Fast Food",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            // 3 Metric Cards: Delivery | Min Order | Furan
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Delivery", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("\$${String.format("%.2f", storeDetail.deliveryFee)}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Min Order", fontSize = 10.sp, color = Color(0xFF64748B))
                                        Text("\$5.00", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("Furan", fontSize = 10.sp, color = Color(0xFF16A34A), fontWeight = FontWeight.Bold)
                                        Text("8:00 AM - 11:00 PM", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                }
                            }

                            // Navigation Tabs: [ Menu ] [ Reviews ] [ Info ]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                listOf("menu" to "Menu", "reviews" to "Reviews", "info" to "Info").forEach { (tabKey, tabLabel) ->
                                    val isTabActive = detailTab == tabKey
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clickable { detailTab = tabKey }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = tabLabel,
                                            fontSize = 13.sp,
                                            fontWeight = if (isTabActive) FontWeight.Black else FontWeight.Medium,
                                            color = if (isTabActive) Color(0xFFDC2626) else Color(0xFF64748B)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .width(28.dp)
                                                .height(2.5.dp)
                                                .background(if (isTabActive) Color(0xFFDC2626) else Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section Title: Cuntooyinka
                    Text(
                        text = "Cuntooyinka",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    // Menu Items List (matching Screen 3 of Image 1)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        storeDetail.products.forEach { prod ->
                            val qty = cartItems[prod.id] ?: 0
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, if (qty > 0) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                                shadowElevation = if (qty > 0) 2.dp else 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.size(68.dp),
                                            color = Color(0xFFF1F5F9)
                                        ) {
                                            AsyncImage(
                                                model = prod.imageUrl,
                                                contentDescription = prod.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = prod.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "\$${String.format("%.2f", prod.price)}",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF0F172A)
                                            )
                                        }
                                    }

                                    // Circular Red '+' button or quantity controls
                                    if (qty > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFF1F5F9),
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clickable {
                                                        if (qty == 1) cartItems.remove(prod.id)
                                                        else cartItems[prod.id] = qty - 1
                                                    }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("−", color = Color(0xFFDC2626), fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                }
                                            }
                                            Text("$qty", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF0F172A))
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFDC2626),
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clickable { cartItems[prod.id] = qty + 1 }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("+", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                                }
                                            }
                                        }
                                    } else {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clickable { cartItems[prod.id] = 1 }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Add",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(100.dp))
                }

                // Sticky Bottom Checkout Bar (matching Screen 3 of Image 1)
                if (cartItemCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 10.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFEF2F2),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.TopEnd)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("$cartItemCount", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "\$${String.format("%.2f", cartTotal)}",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Button(
                                    onClick = { showCheckout = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = "Checkout",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

            } else {
            // ═════════════════════════════════════════════════════════════════════════
            // CUSTOMER VIEW: DOORDASH HOME MARKETPLACE (Screen 2 of Image 1)
            // ═════════════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. CAPSULE SEARCH BAR with Voice Order Mic Button
                OutlinedTextField(
                    value = customerSearchQuery,
                    onValueChange = { customerSearchQuery = it },
                    placeholder = {
                        Text(
                            text = if (isSomaliLanguage) "Raadi maqaayado, dukaamo..." else "Search restaurants, stores...",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (customerSearchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { customerSearchQuery = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = { showVoiceOrderDialog = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Order", tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFFE2E8F0),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // 2. HERO PROMO BANNER: Ku Dalbo Cunto Qiimo Jaban (30% OFF)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFDC2626),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFFE50914),
                                        Color(0xFFDC2626),
                                        Color(0xFFB91C1C)
                                    )
                                )
                            )
                            .padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isSomaliLanguage) "Ku Dalbo Cunto\nQiimo Jaban" else "Order Food at\nBest Prices",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFBBF24)
                                    ) {
                                        Text(
                                            text = "30% OFF",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "SHIRI",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = CircleShape,
                                    modifier = Modifier.size(96.dp),
                                    color = Color.Transparent
                                ) {
                                    AsyncImage(
                                        model = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=300&auto=format&fit=crop&q=80",
                                        contentDescription = "Pizza Promo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. CATEGORY GRID (8 ICONS in 2 ROWS x 4 COLUMNS — Exactly matching Screen 2 of Image 1)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Row 1 (4 items)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Cunto" else "Food",
                            iconEmoji = "🍴",
                            bgColor = Color(0xFFFEE2E2),
                            isSelected = selectedCategoryId == "cunto",
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == "cunto") "all" else "cunto"
                                selectedSubcategoryId = "all"
                            }
                        )
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Dukaamo" else "Stores",
                            iconEmoji = "🛒",
                            bgColor = Color(0xFFE0F2FE),
                            isSelected = selectedCategoryId == "supermarket",
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == "supermarket") "all" else "supermarket"
                                selectedSubcategoryId = "all"
                            }
                        )
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Caafimaad" else "Health",
                            iconEmoji = "➕",
                            bgColor = Color(0xFFFCE7F3),
                            isSelected = selectedCategoryId == "daawo",
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == "daawo") "all" else "daawo"
                                selectedSubcategoryId = "all"
                            }
                        )
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Gaadiid" else "Rides",
                            iconEmoji = "🚗",
                            bgColor = Color(0xFFDBEAFE),
                            isSelected = false,
                            onClick = {
                                onNavigate("ride")
                            }
                        )
                    }

                    // Row 2 (4 items)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Guryo" else "Houses",
                            iconEmoji = "🏠",
                            bgColor = Color(0xFFFFEDD5),
                            isSelected = false,
                            onClick = {
                                showAddressCardDialog = true
                            }
                        )
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Waxbarasho" else "Education",
                            iconEmoji = "🎓",
                            bgColor = Color(0xFFE0E7FF),
                            isSelected = selectedCategoryId == "stationery",
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == "stationery") "all" else "stationery"
                                selectedSubcategoryId = "all"
                            }
                        )
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Ubax" else "Flowers",
                            iconEmoji = "🌸",
                            bgColor = Color(0xFFFEF3C7),
                            isSelected = selectedCategoryId == "perfume",
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == "perfume") "all" else "perfume"
                                selectedSubcategoryId = "all"
                            }
                        )
                        DoorDashCategoryTile(
                            title = if (isSomaliLanguage) "Dhammaan" else "All",
                            iconEmoji = "🔲",
                            bgColor = Color(0xFFF1F5F9),
                            isSelected = selectedCategoryId == "all",
                            onClick = {
                                selectedCategoryId = "all"
                                selectedSubcategoryId = "all"
                            }
                        )
                    }
                }

                // 4. POPULAR RESTAURANTS SECTION (Maqaayada Caanka Ah)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSomaliLanguage) "Maqaayada Caanka Ah" else "Popular Restaurants",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isSomaliLanguage) "Arag Dhammaan >" else "View All >",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.clickable {
                            selectedCategoryId = "all"
                            selectedSubcategoryId = "all"
                        }
                    )
                }

                // Horizontal Carousel of Popular Restaurants
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    stores.take(4).forEach { store ->
                        PopularRestaurantHorizontalCard(
                            store = store,
                            onClick = { viewingStore = store }
                        )
                    }
                }

                // Filtered Stores Feed (DoorDash Full Cards)
                val filteredStores = stores.filter { store ->
                    val matchesCategory = when {
                        selectedCategoryId == "all" -> true
                        store.categoryId == selectedCategoryId -> true
                        store.extraCategories.contains(selectedCategoryId) -> true
                        else -> false
                    }
                    val matchesSubcategory = when {
                        selectedSubcategoryId == "all" -> true
                        store.subcategoryId == selectedSubcategoryId -> true
                        else -> false
                    }
                    val matchesSearch = if (customerSearchQuery.isBlank()) true else {
                        val q = customerSearchQuery.lowercase()
                        store.name.lowercase().contains(q) ||
                        store.district.lowercase().contains(q) ||
                        store.products.any { it.name.lowercase().contains(q) || it.nameEn.lowercase().contains(q) }
                    }
                    matchesCategory && matchesSubcategory && matchesSearch
                }

                // 5. ALL STORES & RESTAURANTS FEED HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSomaliLanguage)
                            "Dhammaan Meheradaha & Dukaamada (${filteredStores.size})"
                        else
                            "All Stores & Restaurants (${filteredStores.size})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isSomaliLanguage) "Muqdisho" else "Mogadishu",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Active Order Floating Pill Banner (if order active)
                if (hasActiveOrder) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLiveTracking = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🛵", fontSize = 16.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Dalabkaaga $trackingOrderNumber wuu socdaa",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "$trackingStoreName • PIN: $trackingHandoverPin",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                            Text(
                                text = "La Soco >",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                if (filteredStores.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🔍", fontSize = 32.sp)
                            Text(
                                text = if (isSomaliLanguage) "Ma jirto meherad u dhigma baaritaankaaga" else "No stores match your search or filter",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Button(
                                onClick = {
                                    selectedCategoryId = "all"
                                    selectedSubcategoryId = "all"
                                    customerSearchQuery = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(if (isSomaliLanguage) "Arag Dhammaan Meheradaha" else "View All Stores", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                } else {
                    filteredStores.forEach { store ->
                        DoorDashFullStoreCard(
                            store = store,
                            onClick = { viewingStore = store }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
            } // end: viewingStore == null branch
        } else {
            // ═════════════════════════════════════════════════════════════════════════
            // MERCHANT / BUSINESS VIEW: STORE & CATALOG MANAGEMENT (INTERACTIVE TABS)
            // ═════════════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Merchant Profile Banner with Open/Close Toggle
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(shape = CircleShape, color = Color(0xFFFEF2F2), modifier = Modifier.size(46.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🏪", fontSize = 24.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Pizza House Mogadishu", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Text("SHR-BO-1024 • Hodan, Mogadishu", fontSize = 10.5.sp, color = Color(0xFF64748B))
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isStoreOpen) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                            modifier = Modifier.clickable {
                                isStoreOpen = !isStoreOpen
                                Toast.makeText(context, if (isStoreOpen) "Dukaanku hadda waa FURAN YAHAY (Open)" else "Dukaanku hadda waa XIRAN YAHAY (Closed)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = if (isStoreOpen) "✓ Furan" else "✕ Xiran",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isStoreOpen) Color(0xFF16A34A) else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────
                // KITCHEN ALARM & LOUD ORDER ALERT TONE (Always on top if triggered)
                // ─────────────────────────────────────────────────────────────────
                if (merchantKitchenAlarm) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(2.dp, Color(0xFFDC2626)),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = Color(0xFFDC2626), modifier = Modifier.size(32.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("🚨 KITCHEN ALARM: DALAB CUSUB!", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                                        Text("Dhawaaqa Jikada (Loud Tone) wuu yeerayaa...", fontSize = 10.sp, color = Color(0xFF64748B))
                                    }
                                }
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDC2626)) {
                                    Text("DEG DEG", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Surface(shape = RoundedCornerShape(10.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFFECACA)), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Dalabka: #SHR-MG-48291", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                        Text("$14.50 (EVC Plus)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                                    }
                                    Text("Macmiil: Cabdifataax • 0612345678", fontSize = 11.sp, color = Color(0xFF475569))
                                    Text("Goobta: SHR-MG-48291 (Hodan, Muqdisho)", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                    Text("Tilmaanta Codka: \"Gurigu waa cagaar agtiisa masaajidka\"", fontSize = 10.5.sp, color = Color(0xFFDC2626), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        merchantPaymentVerified = true
                                        Toast.makeText(context, "Lacagtii waa la xaqiijiyay (Payment Approved)!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (merchantPaymentVerified) Color(0xFF16A34A) else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = if (merchantPaymentVerified) "✓ Lacagta Waa La Oggolaaday" else "Oggolow Lacagta",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Button(
                                    onClick = {
                                        orderReadyForCourier = true
                                        merchantKitchenAlarm = false
                                        trackingStatus = "courier_assigned"
                                        Toast.makeText(context, "Darawalka mootada ayaa loo yeeray (Ready for Pickup)!", Toast.LENGTH_LONG).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = "Waa Diyaar, U Yeer Darawal",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────
                // DYNAMIC TABS SWITCHER (Dashboard, Orders, Products, Finance)
                // ─────────────────────────────────────────────────────────────────
                when (merchantBottomTab) {
                    "orders" -> {
                        // ═════════════════════════════════════════════════════════
                        // ORDERS MANAGEMENT SCREEN (Interactive & Clickable)
                        // ═════════════════════════════════════════════════════════
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Maamulka Dalabyada (Orders)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF2F2)) {
                                Text(
                                    text = "${merchantOrders.size} Dalab",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Order Status Filter Chips
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "all" to "Dhammaan",
                                "pending" to "Cusub (Pending)",
                                "preparing" to "Diyaarinta",
                                "ready" to "Diyaar ah",
                                "delivered" to "La Gaarsiiyay"
                            ).forEach { (stKey, stLabel) ->
                                val isSelected = merchantOrderStatusFilter == stKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { merchantOrderStatusFilter = stKey },
                                    label = { Text(stLabel, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFDC2626),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // Orders List
                        val filteredOrders = merchantOrders.filter {
                            merchantOrderStatusFilter == "all" || it.status == merchantOrderStatusFilter
                        }

                        if (filteredOrders.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Wax dalabyo ah lama helin qaybtaan", fontSize = 12.sp, color = Color(0xFF64748B))
                                }
                            }
                        } else {
                            filteredOrders.forEach { ord ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedOrderForDetail = ord }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(ord.orderNumber, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                                Spacer(Modifier.width(6.dp))
                                                val (bg, fg, txt) = when (ord.status) {
                                                    "pending" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), "Cusub")
                                                    "preparing" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Diyaarinta")
                                                    "ready" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "Diyaar")
                                                    else -> Triple(Color(0xFFF0FDF4), Color(0xFF16A34A), "Delivered")
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = bg) {
                                                    Text(txt, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(
                                                text = "$${String.format("%.2f", ord.total)}",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF16A34A)
                                            )
                                        }

                                        Text(ord.customerName + " • " + ord.customerPhone, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                        Text(ord.itemsSummary, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "PIN: ${ord.handoverPin} • ${ord.timeAgo}",
                                                fontSize = 10.5.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF2F2),
                                                modifier = Modifier.clickable { selectedOrderForDetail = ord }
                                            ) {
                                                Text(
                                                    text = "Faahfaahin →",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "products" -> {
                        // ═════════════════════════════════════════════════════════
                        // PRODUCTS & CATALOG SCREEN (Interactive & Clickable)
                        // ═════════════════════════════════════════════════════════
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isSomaliLanguage) "+ Kudar Alaab Cusub (Product)" else "+ Add New Product",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }

                        // Search Bar for products
                        OutlinedTextField(
                            value = merchantProductSearch,
                            onValueChange = { merchantProductSearch = it },
                            placeholder = { Text("Raadi alaab magaceeda...", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFFDC2626),
                                unfocusedBorderColor = Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Products List Header
                        val currentMerchantStore = stores.firstOrNull()
                        val allProducts = currentMerchantStore?.products ?: emptyList()
                        val filteredProducts = allProducts.filter {
                            merchantProductSearch.isBlank() || it.name.contains(merchantProductSearch, ignoreCase = true)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Liiska Alaabtaada (${filteredProducts.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Guji si aad wax uga bedesho",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        filteredProducts.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProductForEdit = item
                                        editProductName = item.name
                                        editProductPrice = String.format(java.util.Locale.US, "%.2f", item.price)
                                        editProductCategory = item.category
                                        editProductInStock = item.inStock
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = RoundedCornerShape(10.dp), modifier = Modifier.size(52.dp), color = Color(0xFFE2E8F0)) {
                                            AsyncImage(
                                                model = item.imageUrl,
                                                contentDescription = item.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Text("$${String.format("%.2f", item.price)} • ${item.category}", fontSize = 11.5.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (item.inStock) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                                            modifier = Modifier.clickable {
                                                val targetStore = viewingStore ?: stores.firstOrNull()
                                                if (targetStore != null) {
                                                    val updated = stores.map { s ->
                                                        if (s.id == targetStore.id) {
                                                            s.copy(products = s.products.map { p ->
                                                                if (p.id == item.id) p.copy(inStock = !p.inStock) else p
                                                            })
                                                        } else s
                                                    }
                                                    stores.clear()
                                                    stores.addAll(updated)
                                                    Toast.makeText(context, "${item.name} xaaladdiisa waa la bedelay!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = if (item.inStock) "✓ In Stock" else "✕ Out of Stock",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (item.inStock) Color(0xFF16A34A) else Color(0xFFDC2626),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                            )
                                        }

                                        Text(
                                            text = "✎ Wax ka bedel",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    "finance" -> {
                        // ═════════════════════════════════════════════════════════
                        // FINANCE & EARNINGS SCREEN
                        // ═════════════════════════════════════════════════════════
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Dakhliga La Heli Karo (Wallet)", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
                                Text("$385.20", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Button(
                                    onClick = { showWithdrawDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(42.dp)
                                ) {
                                    Text("Kala Bax Dakhliga (Withdraw) →", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Text("Xisaabinta Maanta (Today's Breakdown)", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Iibka Guud (Total Sales):", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    Text("$142.50", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Commission Shiri (10%):", fontSize = 11.5.sp, color = Color(0xFF64748B))
                                    Text("-$14.25", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Dakhliga Saafiga ah:", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    Text("$128.25", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                                }
                            }
                        }
                    }

                    else -> {
                        // ═════════════════════════════════════════════════════════
                        // DEFAULT: DASHBOARD TAB (Quick Stats & Clickable Overview)
                        // ═════════════════════════════════════════════════════════

                        // Merchant Store Info Banner with Digital Guri Number
                        val merchantGuriNumber = viewModel.currentUser.value?.digitalGuriNumber ?: "SH-4029"
                        val merchantStoreName = viewModel.currentUser.value?.businessName ?: "Meheradda Shiri"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "🏪 $merchantStoreName",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Degmada: ${viewModel.currentUser.value?.district ?: "Hodan, Muqdisho"}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1D4ED8)) {
                                    Text(
                                        text = "🏠 Guri: $merchantGuriNumber",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Quick Action Buttons
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddProductDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(4.dp))
                                Text(if (isSomaliLanguage) "+ Kudar Alaab" else "+ Add Product", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { showAdminCategoryDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Qeybaha", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }
                        }

                        // Stats Dashboard - ALL CLICKABLE
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f).clickable { merchantBottomTab = "finance" }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Dakhliga Maanta", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text("$142.50", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                                    Text("Arag Maaliyadda →", fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f).clickable { merchantBottomTab = "orders" }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("Dalabyada Maanta", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text("${merchantOrders.size} Dalab", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    Text("Maamul Dalabyada →", fontSize = 9.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Recent Orders Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Dalabyadii Ugu Dambeeyay",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Arag Dhammaan →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.clickable { merchantBottomTab = "orders" }
                            )
                        }

                        merchantOrders.take(3).forEach { ord ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedOrderForDetail = ord }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(ord.orderNumber, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                        Text(ord.customerName, fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("$${String.format("%.2f", ord.total)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                        val (bg, fg, txt) = when (ord.status) {
                                            "pending" -> Triple(Color(0xFFFEF2F2), Color(0xFFDC2626), "Cusub")
                                            "preparing" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "Diyaarinta")
                                            "ready" -> Triple(Color(0xFFEFF6FF), Color(0xFF2563EB), "Diyaar")
                                            else -> Triple(Color(0xFFF0FDF4), Color(0xFF16A34A), "Delivered")
                                        }
                                        Surface(shape = RoundedCornerShape(4.dp), color = bg) {
                                            Text(txt, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }

                        // Products Catalog Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alaabta Dukaanka (Products)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Maamul Alaabta →",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.clickable { merchantBottomTab = "products" }
                            )
                        }

                        val merchantStore = stores.firstOrNull()
                        merchantStore?.products?.take(3)?.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedProductForEdit = item
                                        editProductName = item.name
                                        editProductPrice = String.format(java.util.Locale.US, "%.2f", item.price)
                                        editProductCategory = item.category
                                        editProductInStock = item.inStock
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(44.dp), color = Color(0xFFE2E8F0)) {
                                            AsyncImage(model = item.imageUrl, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                            Text("$${String.format("%.2f", item.price)} • ${item.category}", fontSize = 10.5.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                    Surface(shape = RoundedCornerShape(6.dp), color = if (item.inStock) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)) {
                                        Text(
                                            text = if (item.inStock) "In Stock" else "Out of Stock",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.inStock) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE UI COMPONENTS (DOORDASH STYLE)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DoorDashCategoryTile(
    title: String,
    iconEmoji: String,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = if (isSelected) Color(0xFFDC2626) else bgColor,
            modifier = Modifier.size(56.dp),
            shadowElevation = if (isSelected) 3.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = iconEmoji,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = if (isSelected) Color(0xFFDC2626) else Color(0xFF0F172A),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PopularRestaurantHorizontalCard(
    store: ShiriMerchantStore,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .width(230.dp)
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            ) {
                AsyncImage(
                    model = store.imageUrl,
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "⏱️ ${store.deliveryTimeMin} min",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⭐", fontSize = 10.sp)
                        Text(text = " ${store.rating}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                    }
                }
                Text(
                    text = "Cunto • Somali Food • Fast Food",
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "🛵 Delivery $${String.format("%.2f", store.deliveryFee)}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            }
        }
    }
}

@Composable
fun DoorDashFullStoreCard(
    store: ShiriMerchantStore,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                AsyncImage(
                    model = store.imageUrl,
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "⏱️ ${store.deliveryTimeMin} min",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = store.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(text = "⭐", fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${store.rating} (2.5k+)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Text(
                    text = "Cunto • Somali Food • Fast Food",
                    fontSize = 11.5.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛵 Delivery $${String.format("%.2f", store.deliveryFee)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Text("•", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Text(
                        text = "🏠 Guri: ${store.shiriCode}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                    Text("•", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                    Text(
                        text = store.district,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerStoreCard(
    store: ShiriMerchantStore,
    onClick: () -> Unit
) {
    DoorDashFullStoreCard(store = store, onClick = onClick)
}

@Composable
fun CustomerNavTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { onClick() }
    ) {
        BadgedBox(
            badge = {
                if (badge != null) {
                    Badge(containerColor = Color(0xFFDC2626), contentColor = Color.White) {
                        Text(text = badge, fontSize = 9.sp)
                    }
                }
            }
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color(0xFFDC2626) else Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Normal,
            color = if (isSelected) Color(0xFFDC2626) else Color(0xFF64748B)
        )
    }
}
