package com.example.somalitraffic.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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

    // Active order banner & Checkout states
    var hasActiveOrder by remember { mutableStateOf(true) }
    var showCheckout by remember { mutableStateOf(false) }
    var showLiveTracking by remember { mutableStateOf(false) }
    var trackingOrderNumber by remember { mutableStateOf("#SHR-28491") }
    var trackingHandoverPin by remember { mutableStateOf("4792") }
    var trackingStatus by remember { mutableStateOf("preparing") }
    var trackingStoreName by remember { mutableStateOf("Pizza House Mogadishu") }

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
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                        val orderId = "SHR-${System.currentTimeMillis() % 100000}"
                        trackingHandoverPin = pin
                        trackingOrderNumber = orderId
                        trackingStatus = "preparing"
                        trackingStoreName = currentStore.name
                        showCheckout = false
                        showLiveTracking = true
                        hasActiveOrder = true
                        cartItems.clear()
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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

                    // Advance Simulator Button
                    Button(
                        onClick = {
                            trackingStatus = when (trackingStatus) {
                                "preparing" -> "courier_assigned"
                                "courier_assigned" -> "in_transit"
                                "in_transit" -> "delivered"
                                else -> "preparing"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isSomaliLanguage) "Tijaabi Xaaladda Xigta →" else "Advance Status Demo →",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1)
                        )
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
                    // TOP BAR ROW 1: BRAND LOGO + ROLE SWITCHER BAR + LANGUAGE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Shiri Brand Pill
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFDC2626),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "S", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "SHIRI",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (currentAppMode == "customer") Color(0xFF0F172A) else Color.White
                                )
                                Text(
                                    text = if (currentAppMode == "customer")
                                        (if (isSomaliLanguage) "Eats & Market" else "Eats & Marketplace")
                                    else
                                        (if (isSomaliLanguage) "Ganacsatada" else "Merchant App"),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        // Right: Role Switcher & Language Pill
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (currentAppMode == "customer") {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFDC2626).copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { currentAppMode = "merchant" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🏪", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "Business", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF97316).copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { onNavigate("driver_pass") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🛺", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "Darawal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC2410C))
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                    modifier = Modifier.clickable { onNavigate("delivery") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🏍️", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "Delivery", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFDC2626),
                                    modifier = Modifier.clickable {
                                        currentAppMode = "customer"
                                        viewingStore = null
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(text = "Macmiilka", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }

                            // Language Toggle (SO / EN)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (currentAppMode == "customer") Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                modifier = Modifier.clickable {
                                    isSomaliLanguage = !isSomaliLanguage
                                    Toast.makeText(context, if (isSomaliLanguage) "Af-Soomaali" else "English Selected", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = if (isSomaliLanguage) "SO" else "EN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (currentAppMode == "customer") Color(0xFF0F172A) else Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
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
                            label = if (isSomaliLanguage) "Guriga" else "Home",
                            isSelected = customerBottomTab == "home",
                            badge = null,
                            onClick = { customerBottomTab = "home"; viewingStore = null }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Search,
                            label = if (isSomaliLanguage) "Baadh" else "Explore",
                            isSelected = customerBottomTab == "explore",
                            badge = null,
                            onClick = { customerBottomTab = "explore" }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.ReceiptLong,
                            label = if (isSomaliLanguage) "Dalabyada" else "Orders",
                            isSelected = customerBottomTab == "orders",
                            badge = if (hasActiveOrder) "1" else null,
                            onClick = { customerBottomTab = "orders"; showLiveTracking = true }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Person,
                            label = if (isSomaliLanguage) "Profile" else "Profile",
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
            // ═════════════════════════════════════════════════════════════════════════
            // CUSTOMER VIEW: MULTI-CATEGORY MARKETPLACE
            // ═════════════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Digital Address Bar + Admin Categories Trigger
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFDC2626).copy(alpha = 0.12f), modifier = Modifier.size(34.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isSomaliLanguage) "GOOBTAADA: " else "DELIVERY TO: ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text("SHR-HO-55897", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                            Text("Hodan, Mogadishu", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        }
                    }

                    // Admin Categories Tool Icon
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { showAdminCategoryDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isSomaliLanguage) "Categories" else "Categories",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }

                // Search Bar across all stores, products, and categories
                OutlinedTextField(
                    value = customerSearchQuery,
                    onValueChange = { customerSearchQuery = it },
                    placeholder = {
                        Text(
                            if (isSomaliLanguage)
                                "Raadi cunto, daawo, supermarket, dhar, qalab..."
                            else
                                "Search food, pharmacy, groceries, clothes, tech...",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (customerSearchQuery.isNotBlank()) {
                            IconButton(onClick = { customerSearchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFDC2626),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                // Active Order Banner (Image 1)
                if (hasActiveOrder) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFDC2626),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLiveTracking = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFFB91C1C))))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (isSomaliLanguage) "DALAB HADDA SOCDA" else "ACTIVE ORDER",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(color = Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
                                            Text(trackingOrderNumber, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                                        Text(
                                            text = if (isSomaliLanguage) "Raac Dalabka >" else "Track Order >",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$trackingStoreName • PIN: $trackingHandoverPin",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
                }

                // Hero Promo Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(Color(0xFFDC2626), Color(0xFF991B1B))))
                            .padding(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isSomaliLanguage) "MOGADISHU SUPER MARKETPLACE" else "MOGADISHU SUPER MARKETPLACE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isSomaliLanguage) "Cunto, Daawo, Supermarket & Ganacsiyo" else "Food, Medicine, Groceries & All Stores",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { selectedCategoryId = "all" },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(
                                        text = if (isSomaliLanguage) "Dhexdhufo Hadda" else "Explore All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                            }
                            Text(text = "🛍️", fontSize = 42.sp)
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────
                // CATEGORIES HORIZONTAL CAROUSEL (Shopping & Booking ONLY - Traffic Map isolated to Driver)
                // ─────────────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSomaliLanguage) "QAYBAHA GANACSIGA (${categories.size})" else "BUSINESS CATEGORIES (${categories.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = if (isSomaliLanguage) "Dhammaan" else "View All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626),
                        modifier = Modifier.clickable {
                            selectedCategoryId = "all"
                            selectedSubcategoryId = "all"
                        }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. All
                    CategorySquareCard(
                        title = if (isSomaliLanguage) "Dhammaan" else "All",
                        emoji = "✨",
                        isSelected = selectedCategoryId == "all",
                        onClick = {
                            selectedCategoryId = "all"
                            selectedSubcategoryId = "all"
                        },
                        modifier = Modifier.width(82.dp)
                    )

                    // 2. Dalbo Bajaaj
                    CategorySquareCard(
                        title = if (isSomaliLanguage) "Dalbo Bajaaj" else "Book Bajaj",
                        emoji = "🛺",
                        isSelected = false,
                        onClick = { onNavigate("ride") },
                        modifier = Modifier.width(86.dp)
                    )

                    // 3. Gaarsiin Mooto
                    CategorySquareCard(
                        title = if (isSomaliLanguage) "Dir Baakad" else "Parcel Express",
                        emoji = "🏍️",
                        isSelected = false,
                        onClick = { onNavigate("delivery") },
                        modifier = Modifier.width(86.dp)
                    )

                    // 4..N Dynamic Business Categories
                    categories.forEach { cat ->
                        CategorySquareCard(
                            title = if (isSomaliLanguage) cat.nameSo else cat.nameEn,
                            emoji = cat.emoji,
                            isSelected = selectedCategoryId == cat.id,
                            onClick = {
                                selectedCategoryId = cat.id
                                selectedSubcategoryId = "all"
                            },
                            modifier = Modifier.width(96.dp)
                        )
                    }
                }

                // ─────────────────────────────────────────────────────────────────
                // SUBCATEGORY PILLS (Shown when a category is selected)
                // ─────────────────────────────────────────────────────────────────
                val activeCategoryObj = categories.find { it.id == selectedCategoryId }
                if (activeCategoryObj != null && activeCategoryObj.subcategories.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubcategoryId == "all",
                            onClick = { selectedSubcategoryId = "all" },
                            label = { Text(if (isSomaliLanguage) "Dhammaan Qeybaha" else "All Subcategories", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFDC2626),
                                selectedLabelColor = Color.White
                            )
                        )
                        activeCategoryObj.subcategories.forEach { sub ->
                            FilterChip(
                                selected = selectedSubcategoryId == sub.id,
                                onClick = { selectedSubcategoryId = sub.id },
                                label = { Text("${sub.emoji} ${if (isSomaliLanguage) sub.nameSo else sub.nameEn}", fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFDC2626),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // ─────────────────────────────────────────────────────────────────
                // STORE FILTERING LOGIC
                // ─────────────────────────────────────────────────────────────────
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

                // Header for filtered list
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSomaliLanguage)
                            "GANACSIYADA MUQDISHO (${filteredStores.size})"
                        else
                            "MOGADISHU STORES (${filteredStores.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text("Mogadishu, SO", fontSize = 11.sp, color = Color(0xFF64748B))
                }

                // Empty State
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
                                text = if (isSomaliLanguage) "Ma jiro dukaan u dhigma baaritaankaaga" else "No stores match your search or filter",
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
                                Text(if (isSomaliLanguage) "Arag Dhammaan Dukaamada" else "View All Stores", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }

                // Store Cards
                filteredStores.forEach { store ->
                    CustomerStoreCard(
                        store = store,
                        onClick = { viewingStore = store }
                    )
                }

                // ─────────────────────────────────────────────────────────────────
                // STORE PRODUCTS MODAL / VIEW (When a user clicks on ANY store)
                // ─────────────────────────────────────────────────────────────────
                if (viewingStore != null) {
                    val currentStore = viewingStore!!
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Store Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Surface(shape = CircleShape, color = Color(0xFFFEF2F2), modifier = Modifier.size(42.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(currentStore.emoji, fontSize = 22.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(currentStore.name, fontSize = 14.5.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                        Text("${currentStore.district} • ⏱️ ${currentStore.deliveryTimeMin} min • ⭐ ${currentStore.rating}", fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                                IconButton(onClick = { viewingStore = null }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF64748B))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Text(
                                text = if (isSomaliLanguage) "ALAABTA DUKAANKAN KA HELI KARTO (${currentStore.products.size})" else "AVAILABLE PRODUCTS (${currentStore.products.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDC2626)
                            )

                            // Products Grid / Column
                            currentStore.products.forEach { prod ->
                                val qty = cartItems[prod.id] ?: 0
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(54.dp), color = Color(0xFFE2E8F0)) {
                                                AsyncImage(model = prod.imageUrl, contentDescription = prod.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text(prod.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                                Text("$${String.format("%.2f", prod.price)} • ${prod.category}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                                if (prod.description.isNotBlank()) {
                                                    Text(prod.description, fontSize = 9.5.sp, color = Color(0xFF64748B), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                        }

                                        // Cart Quantity Controls (+ / -)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (qty > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFDC2626),
                                                    modifier = Modifier.size(26.dp).clickable {
                                                        if (qty == 1) cartItems.remove(prod.id)
                                                        else cartItems[prod.id] = qty - 1
                                                    }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    }
                                                }
                                                Text("$qty", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF0F172A))
                                            }

                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFDC2626),
                                                modifier = Modifier.size(26.dp).clickable {
                                                    cartItems[prod.id] = qty + 1
                                                }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // If items are in the cart for this store, show Checkout Button
                            if (cartItemCount > 0) {
                                Button(
                                    onClick = { showCheckout = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(44.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("$cartItemCount alaab boorsada ku jirta", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("Gudub Checkout ($${String.format("%.2f", cartTotal)}) →", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        } else {
            // ═════════════════════════════════════════════════════════════════════════
            // MERCHANT / BUSINESS VIEW: STORE & CATALOG MANAGEMENT
            // ═════════════════════════════════════════════════════════════════════════
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Merchant Profile Banner
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
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDC2626).copy(alpha = 0.12f), modifier = Modifier.clickable { showAdminCategoryDialog = true }) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Qeybaha", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }

                // Add Product Button
                Button(
                    onClick = { showAddProductDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
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

                // Stats Dashboard
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Dakhliga Maanta", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("$142.50", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF16A34A))
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Dalabyada Maanta", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("18 Dalab", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        }
                    }
                }

                // Master Product List for Merchant
                Text(
                    text = if (isSomaliLanguage) "Liiska Alaabtaada (Products)" else "Your Products Catalog",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                val merchantStore = stores.firstOrNull()
                merchantStore?.products?.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(shape = RoundedCornerShape(8.dp), modifier = Modifier.size(50.dp), color = Color(0xFFE2E8F0)) {
                                    AsyncImage(model = item.imageUrl, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(item.name, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text("$${String.format("%.2f", item.price)} • ${item.category}", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = if (item.inStock) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)) {
                                Text(
                                    text = if (item.inStock) "In Stock" else "Out of Stock",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (item.inStock) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
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
// REUSABLE UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CategorySquareCard(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xFFDC2626) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFDC2626) else Color(0xFFE2E8F0)),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color(0xFFF1F5F9),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = emoji, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CustomerStoreCard(
    store: ShiriMerchantStore,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(76.dp),
                color = Color(0xFFF1F5F9)
            ) {
                AsyncImage(
                    model = store.imageUrl,
                    contentDescription = store.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = store.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFEF3C7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⭐", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "${store.rating}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E))
                        }
                    }
                }

                Text(
                    text = "${store.shiriCode} • ${store.district}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ ${store.deliveryTimeMin} daq",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "•",
                        fontSize = 10.5.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Text(
                        text = "🛵 Gaarsiin $${String.format("%.2f", store.deliveryFee)}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }

                Text(
                    text = "${store.products.size} alaab oo diyaar ah",
                    fontSize = 10.sp,
                    color = Color(0xFF16A34A),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
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
