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
import androidx.compose.runtime.snapshots.SnapshotStateMap
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
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.util.ArrivalSoundManager
import java.util.UUID

// ─── Data Models ─────────────────────────────────────────────────────────────
data class ShiriProductItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val price: Double,
    val category: String,
    val imageUrl: String,
    val inStock: Boolean = true,
    val emoji: String = "🍽️"
)

data class ShiriMerchantStore(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val shiriCode: String,
    val district: String,
    val category: String, // "food", "market", "pharmacy"
    val rating: Double,
    val deliveryTimeMin: Int,
    val deliveryFee: Double,
    val imageUrl: String,
    val emoji: String = "🏪",
    val phone: String = "0612345678"
)

// Helper: auto-detect image & emoji based on product name
fun getAutoImageForProduct(name: String): Pair<String, String> {
    val q = name.lowercase().trim()
    return when {
        q.contains("pizza") -> Pair(
            "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
            "🍕"
        )
        q.contains("shuwarma") || q.contains("shawarma") || q.contains("shawerma") -> Pair(
            "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?w=400&auto=format&fit=crop&q=80",
            "🌯"
        )
        q.contains("burger") -> Pair(
            "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&auto=format&fit=crop&q=80",
            "🍔"
        )
        q.contains("fries") || q.contains("chips") || q.contains("baradho") -> Pair(
            "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&auto=format&fit=crop&q=80",
            "🍟"
        )
        q.contains("cola") || q.contains("coca") || q.contains("pepsi") || q.contains("soda") || q.contains("drink") -> Pair(
            "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80",
            "🥤"
        )
        q.contains("chicken") || q.contains("digaag") -> Pair(
            "https://images.unsplash.com/photo-1626082927389-6cd097cdc6ec?w=400&auto=format&fit=crop&q=80",
            "🍗"
        )
        q.contains("rice") || q.contains("bariis") -> Pair(
            "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&auto=format&fit=crop&q=80",
            "🍚"
        )
        q.contains("sambusa") || q.contains("samosa") -> Pair(
            "https://images.unsplash.com/photo-1601050690597-df0568f70950?w=400&auto=format&fit=crop&q=80",
            "🥟"
        )
        q.contains("coffee") || q.contains("qaxwo") -> Pair(
            "https://images.unsplash.com/photo-1509042239860-f550ce710b93?w=400&auto=format&fit=crop&q=80",
            "☕"
        )
        q.contains("tea") || q.contains("shaah") -> Pair(
            "https://images.unsplash.com/photo-1576092768241-dec231879fc3?w=400&auto=format&fit=crop&q=80",
            "🫖"
        )
        q.contains("cake") || q.contains("macmacaan") -> Pair(
            "https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400&auto=format&fit=crop&q=80",
            "🍰"
        )
        q.contains("fish") || q.contains("kalluun") -> Pair(
            "https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400&auto=format&fit=crop&q=80",
            "🐟"
        )
        q.contains("milk") || q.contains("caano") -> Pair(
            "https://images.unsplash.com/photo-1550583724-b2692b85b150?w=400&auto=format&fit=crop&q=80",
            "🥛"
        )
        q.contains("water") || q.contains("biyo") -> Pair(
            "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=400&auto=format&fit=crop&q=80",
            "💧"
        )
        q.contains("medicine") || q.contains("daawo") || q.contains("panadol") -> Pair(
            "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
            "💊"
        )
        else -> Pair(
            "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&auto=format&fit=crop&q=80",
            "🍽️"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriEatsMarketScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val context = LocalContext.current

    // App Mode: "customer" (Image 1) or "merchant" (Image 2)
    var currentAppMode by remember { mutableStateOf("customer") }

    // Bottom tab inside Customer App: "home", "explore", "orders", "profile"
    var customerBottomTab by remember { mutableStateOf("home") }

    // Bottom tab inside Merchant App: "dashboard", "orders", "products", "finance"
    var merchantBottomTab by remember { mutableStateOf("products") }

    // Customer Category Filter: "all", "cunto", "supermarket", "daawo"
    var selectedCategoryFilter by remember { mutableStateOf("all") }
    var customerSearchQuery by remember { mutableStateOf("") }

    // Sound toggle state
    var isSoundEnabled by remember { mutableStateOf(true) }

    // Active order banner visibility
    var hasActiveOrder by remember { mutableStateOf(true) }

    // Selected merchant to view products from customer side
    var viewingStore by remember { mutableStateOf<ShiriMerchantStore?>(null) }

    // Master Product List for Pizza House Mogadishu (as shown in Image 2)
    val pizzaHouseProducts = remember {
        mutableStateListOf(
            ShiriProductItem(
                name = "chicken shuwarma",
                price = 2.00,
                category = "Main Dish",
                imageUrl = "https://images.unsplash.com/photo-1529006557810-274b9b2fc783?w=400&auto=format&fit=crop&q=80",
                emoji = "🌯"
            ),
            ShiriProductItem(
                name = "Margherita Pizza",
                price = 8.50,
                category = "Pizza",
                imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
                emoji = "🍕"
            ),
            ShiriProductItem(
                name = "Crispy Chicken Burger",
                price = 6.50,
                category = "Burgers",
                imageUrl = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=400&auto=format&fit=crop&q=80",
                emoji = "🍔"
            ),
            ShiriProductItem(
                name = "French Fries",
                price = 3.00,
                category = "Sides",
                imageUrl = "https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=400&auto=format&fit=crop&q=80",
                emoji = "🍟"
            ),
            ShiriProductItem(
                name = "Coca Cola 330ml Can",
                price = 1.50,
                category = "Drinks",
                imageUrl = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400&auto=format&fit=crop&q=80",
                emoji = "🥤"
            )
        )
    }

    // ── Cart State: productId -> quantity ──────────────────────────────────────
    val cartItems = remember { mutableStateMapOf<String, Int>() }
    val cartTotal by derivedStateOf {
        cartItems.entries.sumOf { (productId, qty) ->
            (pizzaHouseProducts.find { it.id == productId }?.price ?: 0.0) * qty
        }
    }
    val cartItemCount by derivedStateOf { cartItems.values.sum() }

    // ── Checkout / Tracking State ──────────────────────────────────────────────
    var showCheckout by remember { mutableStateOf(false) }
    var showLiveTracking by remember { mutableStateOf(false) }
    var trackingOrderNumber by remember { mutableStateOf("") }
    var trackingHandoverPin by remember { mutableStateOf("") }
    var trackingStatus by remember { mutableStateOf("preparing") } // preparing -> courier_assigned -> in_transit -> delivered

    // List of Famous Mogadishu Businesses (Image 1)
    val famousStores = remember {
        mutableStateListOf(
            ShiriMerchantStore(
                name = "Pizza House Mogadishu",
                shiriCode = "SHR-BO-1024",
                district = "Hodan",
                category = "cunto",
                rating = 4.8,
                deliveryTimeMin = 25,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
                emoji = "🍕"
            ),
            ShiriMerchantStore(
                name = "Sahra Somali Restaurant",
                shiriCode = "SHR-BO-2041",
                district = "Waberi",
                category = "cunto",
                rating = 4.9,
                deliveryTimeMin = 20,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1512058564366-18510be2db19?w=400&auto=format&fit=crop&q=80",
                emoji = "🍲"
            ),
            ShiriMerchantStore(
                name = "Bakaaro Fresh Supermarket",
                shiriCode = "SHR-BO-3392",
                district = "Bakaaro",
                category = "supermarket",
                rating = 4.7,
                deliveryTimeMin = 30,
                deliveryFee = 2.00,
                imageUrl = "https://images.unsplash.com/photo-1542838132-92c53300491e?w=400&auto=format&fit=crop&q=80",
                emoji = "🛒"
            ),
            ShiriMerchantStore(
                name = "Al-Najax Pharmacy",
                shiriCode = "SHR-BO-4911",
                district = "Taleex",
                category = "daawo",
                rating = 4.9,
                deliveryTimeMin = 15,
                deliveryFee = 1.50,
                imageUrl = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400&auto=format&fit=crop&q=80",
                emoji = "💊"
            )
        )
    }

    // Dialog state for "+ Kudar Alaab Cusub"
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var newProductPrice by remember { mutableStateOf("") }
    var newProductCategory by remember { mutableStateOf("Main Dish") }
    var newProductInStock by remember { mutableStateOf(true) }

    // Auto-detected image for newly typed product
    val autoImagePair = remember(newProductName) {
        getAutoImageForProduct(newProductName)
    }

    // Modal to add new product
    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFDC2626).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = autoImagePair.second, fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Kudar Alaab Cusub",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
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
                    Text(
                        text = "Markaad qorto magaca, sawrka alaabta si toos ah ayuu u soo baxayaa!",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    // Product Name
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("Magaca Alaabta (Product Name)") },
                        placeholder = { Text("Tusaale: Pizza, Burger, Shuwarma, Fries...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Quick suggestion chips
                    Text(
                        text = "Tusaalooyinka Degdegga ah:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "Pizza" to "🍕",
                            "Burger" to "🍔",
                            "Shuwarma" to "🌯",
                            "Fries" to "🍟",
                            "Coca Cola" to "🥤",
                            "Bariis" to "🍚",
                            "Sambusa" to "🥟",
                            "Shaah" to "🫖"
                        ).forEach { (sugName, sugEmoji) ->
                            FilterChip(
                                selected = newProductName.equals(sugName, ignoreCase = true),
                                onClick = { newProductName = sugName },
                                label = { Text("$sugEmoji $sugName", fontSize = 11.sp) }
                            )
                        }
                    }

                    // Live Image Preview Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier.fillMaxWidth().height(110.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(90.dp),
                                color = Color(0xFFE2E8F0)
                            ) {
                                AsyncImage(
                                    model = autoImagePair.first,
                                    contentDescription = newProductName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Surface(
                                    color = Color(0xFF16A34A).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "✓ Sawir Toos ah",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (newProductName.isNotBlank()) newProductName else "Magaca qor...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Waxaa loo diyaariyay Customer App",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }

                    // Price
                    OutlinedTextField(
                        value = newProductPrice,
                        onValueChange = { newProductPrice = it },
                        label = { Text("Qiimaha ($ USD)") },
                        placeholder = { Text("Tusaale: 4.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Category
                    OutlinedTextField(
                        value = newProductCategory,
                        onValueChange = { newProductCategory = it },
                        label = { Text("Qeybta (Category)") },
                        placeholder = { Text("Main Dish, Pizza, Burgers, Drinks...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // In Stock Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { newProductInStock = !newProductInStock }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Waa Diyaar (In Stock)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Switch(
                            checked = newProductInStock,
                            onCheckedChange = { newProductInStock = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProductName.isBlank()) {
                            Toast.makeText(context, "Fadlan geli magaca alaabta!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val parsedPrice = newProductPrice.toDoubleOrNull() ?: 2.00
                        pizzaHouseProducts.add(
                            ShiriProductItem(
                                name = newProductName.trim(),
                                price = parsedPrice,
                                category = newProductCategory.trim().ifBlank { "General" },
                                imageUrl = autoImagePair.first,
                                emoji = autoImagePair.second,
                                inStock = newProductInStock
                            )
                        )
                        Toast.makeText(context, "'${newProductName.trim()}' waa lagu daray liiska!", Toast.LENGTH_SHORT).show()
                        newProductName = ""
                        newProductPrice = ""
                        showAddProductDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Kudar Alaabta Hadda", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("Ka Noqo", color = Color.Gray)
                }
            }
        )
    }

    // ─── Checkout Modal (1-Step: Address, Payment, USSD, Confirm) ────────────
    if (showCheckout && viewingStore != null) {
        val store = viewingStore!!
        var checkoutAddress by remember { mutableStateOf("SHR-HO-55897 • Hodan, Mogadishu") }
        var checkoutLandmark by remember { mutableStateOf("Agta masaajidka, wac markaad soo gaarto") }
        var checkoutPayment by remember { mutableStateOf("evc") }
        var checkoutPayerPhone by remember { mutableStateOf("") }
        var checkoutCopied by remember { mutableStateOf(false) }
        var checkoutSubmitting by remember { mutableStateOf(false) }

        val deliveryFee = store.deliveryFee
        val serviceFee = 0.50
        val checkoutTotal = cartTotal + deliveryFee + serviceFee

        val ussdCode = when (checkoutPayment) {
            "evc"    -> "*712*${store.phone.replace(Regex("\\D"), "")}*${String.format("%.2f", checkoutTotal)}#"
            "edahab" -> "*789*${store.phone.replace(Regex("\\D"), "")}*${String.format("%.2f", checkoutTotal)}#"
            "zaad"   -> "*880*${store.phone.replace(Regex("\\D"), "")}*${String.format("%.2f", checkoutTotal)}#"
            "sahal"  -> "*550*${store.phone.replace(Regex("\\D"), "")}*${String.format("%.2f", checkoutTotal)}#"
            else     -> ""
        }

        AlertDialog(
            onDismissRequest = { showCheckout = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("🛒", fontSize = 20.sp) }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Checkout & Dalbo (1-Step)", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                            Text("${store.name} · ${cartItemCount} alaab", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    // 1. Delivery Address
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CINWAANKA GAARSIINTA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        }
                        OutlinedTextField(
                            value = checkoutAddress,
                            onValueChange = { checkoutAddress = it },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFDC2626))
                        )
                        OutlinedTextField(
                            value = checkoutLandmark,
                            onValueChange = { checkoutLandmark = it },
                            label = { Text("Calaamad / Tilmaan") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFDC2626))
                        )
                    }

                    // 2. Payment Methods
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("LACAG-BIXINTA", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                        val paymentOptions = listOf(
                            Triple("evc",    "EVC Plus",   "Hormuud • 📱"),
                            Triple("edahab", "eDahab",     "Somtel • 💳"),
                            Triple("zaad",   "ZAAD",       "Telesom • 📶"),
                            Triple("sahal",  "Sahal",      "Golis • 📲"),
                            Triple("cash",   "Cash",       "Lacag Caddaan • 💵")
                        )
                        paymentOptions.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { (id, name, provider) ->
                                    val selected = checkoutPayment == id
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selected) Color(0xFFFEF2F2) else Color(0xFFF8FAFC),
                                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Color(0xFFDC2626) else Color(0xFFE2E8F0)),
                                        modifier = Modifier.weight(1f).clickable { checkoutPayment = id }
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Black, color = if (selected) Color(0xFFDC2626) else Color(0xFF0F172A))
                                            Text(provider, fontSize = 10.sp, color = Color(0xFF64748B))
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    // 3. USSD + Payer Phone
                    if (checkoutPayment != "cash") {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFFBEB), border = BorderStroke(1.dp, Color(0xFFFDE68A)), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Xaqiijinta Lacag-Bixinta", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEF3C7)) {
                                        Text(ussdCode, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
                                    }
                                    Button(
                                        onClick = { checkoutCopied = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(if (checkoutCopied) "✓ La Qaaday" else "Koobiye", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                                OutlinedTextField(
                                    value = checkoutPayerPhone,
                                    onValueChange = { checkoutPayerPhone = it },
                                    label = { Text("Numberkaaga Lacagta (Payer Phone)") },
                                    placeholder = { Text("0618xxxxxx") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD97706))
                                )
                            }
                        }
                    }

                    // 4. Bill Breakdown
                    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("XISAABTA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Alaabta Wadarta", fontSize = 12.sp, color = Color(0xFF475569))
                                Text("$${String.format("%.2f", cartTotal)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lacagta Gaarsiinta", fontSize = 12.sp, color = Color(0xFF475569))
                                Text("$${String.format("%.2f", deliveryFee)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Adeega SHIRI", fontSize = 12.sp, color = Color(0xFF475569))
                                Text("$${String.format("%.2f", serviceFee)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("WADARTA", fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                Text("$${String.format("%.2f", checkoutTotal)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!checkoutSubmitting) {
                            checkoutSubmitting = true
                            val pin = (1000..9999).random().toString()
                            val orderId = "SHR-${System.currentTimeMillis() % 100000}"
                            trackingHandoverPin = pin
                            trackingOrderNumber = orderId
                            trackingStatus = "preparing"
                            showCheckout = false
                            showLiveTracking = true
                            hasActiveOrder = true
                            cartItems.clear()
                            viewingStore = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xaqiiji & Dalbo Hadda →", fontWeight = FontWeight.Black, color = Color.White, fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckout = false }) {
                    Text("Ka Noqo", color = Color(0xFF64748B))
                }
            }
        )
    }

    // ─── Live Tracking Full-Screen Modal ─────────────────────────────────────
    if (showLiveTracking) {
        AlertDialog(
            onDismissRequest = { showLiveTracking = false },
            containerColor = Color(0xFF0F172A),
            shape = RoundedCornerShape(24.dp),
            title = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("🗺️  Live Tracking", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text(trackingOrderNumber, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                        Surface(color = Color(0xFF16A34A), shape = RoundedCornerShape(10.dp)) {
                            Text("● SOCDA", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Status Stepper
                    val stages = listOf(
                        "pending"          to "La Diray ✓",
                        "preparing"        to "Diyaarinta Cuntada",
                        "courier_assigned" to "Darawal loo Qoondeeyay",
                        "in_transit"       to "Waddada Kusoo Jira 🛵",
                        "delivered"        to "Gacanta Loo Geliyay ✓"
                    )
                    val activeIdx = stages.indexOfFirst { it.first == trackingStatus }.coerceAtLeast(0)
                    stages.forEachIndexed { idx, (_, label) ->
                        val done = idx < activeIdx
                        val active = idx == activeIdx
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = when { done -> Color(0xFF16A34A); active -> Color(0xFFDC2626); else -> Color(0xFF334155) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (done) "✓" else "${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(label, fontSize = 12.sp, fontWeight = if (active) FontWeight.Black else FontWeight.Normal, color = if (active) Color.White else Color(0xFF94A3B8))
                        }
                    }

                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Handover PIN
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E293B), border = BorderStroke(1.dp, Color(0xFFDC2626)), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("HANDOVER PIN — Darawalka Siiso", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(trackingHandoverPin, fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626), letterSpacing = 10.sp)
                            Text("Kaliya darawalka la siiso markuu yimaado", fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }

                    // Contact Courier Buttons
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val phoneIntent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:+252613456789"))
                        val waUrl = "https://wa.me/252613456789?text=Salaan! Dalabaygii SHIRI $trackingOrderNumber"
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Wac Darawal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💬 WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLiveTracking = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Dib u Noqo Home", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {}
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFF0F172A),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Top App Role Switcher Bar (Customer App vs Business App)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    if (viewingStore != null) {
                                        viewingStore = null
                                    } else {
                                        onBackClick()
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Dib",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "SHIRI",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        // Role Switcher buttons for quick access
                        if (currentAppMode == "customer") {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.White.copy(alpha = 0.12f),
                                    modifier = Modifier.clickable { currentAppMode = "merchant" }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🏪", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Business",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF97316).copy(alpha = 0.25f),
                                    modifier = Modifier.clickable { onNavigate("driver_pass") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🛺", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Darawal",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFED7AA)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                    modifier = Modifier.clickable { onNavigate("delivery") }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "🏍️", fontSize = 11.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Delivery",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFBAE6FD)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
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
            // Bottom Nav Bar matching the Screenshots
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentAppMode == "customer") {
                    // Customer Bottom Navigation (Image 1): Home, Explore, Orders (badge 1), Profile
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
                            label = "Home",
                            isSelected = customerBottomTab == "home",
                            badge = null,
                            onClick = { customerBottomTab = "home" }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Search,
                            label = "Explore",
                            isSelected = customerBottomTab == "explore",
                            badge = null,
                            onClick = { customerBottomTab = "explore" }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.ReceiptLong,
                            label = "Orders",
                            isSelected = customerBottomTab == "orders",
                            badge = "1",
                            onClick = { 
                                customerBottomTab = "orders"
                                showLiveTracking = true
                            }
                        )
                        CustomerNavTabItem(
                            icon = Icons.Default.Person,
                            label = "Profile",
                            isSelected = customerBottomTab == "profile",
                            badge = null,
                            onClick = { 
                                customerBottomTab = "profile"
                                onNavigate("profile_settings")
                            }
                        )
                    }
                } else {
                    // Merchant Bottom Navigation (Image 2): Dashboard, Orders, Products, Finance
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

        AnimatedContent(
            targetState = currentAppMode,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen_mode"
        ) { mode ->
            if (mode == "customer") {
                // ══════════════════════════════════════════════════════════════
                // 1. CUSTOMER APP VIEW (IMAGE 1)
                // ══════════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Location Header Row: "GOOBTA HADDA: SHR-HO-55897 | Hodan, Mogadishu >" + QR + EN
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    Toast.makeText(context, "Goobta: SHR-HO-55897 (Hodan)", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "GOOBTA HADDA: ",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "SHR-HO-55897",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Hodan, Mogadishu",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .size(34.dp)
                                    .clickable {
                                        Toast.makeText(context, "QR Scanner Diyaar", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "QR",
                                        tint = Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.clickable {
                                    Toast.makeText(context, "Af Soomaali / English", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    text = "EN",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    // Search Bar: "Raadi cunto, dukaamo, adeegyo..."
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { customerSearchQuery = it },
                        placeholder = {
                            Text(
                                "Raadi cunto, dukaamo, adeegyo...",
                                fontSize = 12.5.sp,
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFFDC2626),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )

                    // Active Order Banner (Image 1): "DALAB HADDA SOCDA #SHR-28... Raac Dalabka >"
                    if (hasActiveOrder) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFDC2626),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Furaya live tracking-ka dalabka...", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color(0xFFDC2626), Color(0xFFB91C1C), Color(0xFF991B1B))
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "DALAB HADDA SOCDA",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color.White.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "#SHR-28491",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = Color.White,
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Raac Dalabka",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFDC2626)
                                                )
                                                Icon(
                                                    Icons.Default.ChevronRight,
                                                    contentDescription = null,
                                                    tint = Color(0xFFDC2626),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.TwoWheeler,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Darawal ayaa loo qoondeeyay",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                        }

                                        Surface(
                                            color = Color.White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = "~15 min",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Hero Banner (Big Red Card with Scooter graphic - Image 1)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
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
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "KU DHOWAA, KU GAARA",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White.copy(alpha = 0.85f),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Dalbo Cunto Kuna\nRaaxee",
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        lineHeight = 22.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Gaarsiin degdeg ah 20-30 daqiiqo gudahood",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            selectedCategoryFilter = "cunto"
                                            Toast.makeText(context, "Dooro maqaayad hoos ka muuqata!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                        shape = RoundedCornerShape(20.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text(
                                            text = "Dalbo Hadda",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.size(70.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🛵", fontSize = 40.sp)
                                    }
                                }
                            }
                        }
                    }

                    // QAYBAHA ADEEGYADA (Categories) with "Arag Dhammaan" (Image 1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QAYBAHA ADEEGYADA",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Arag Dhammaan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.clickable { selectedCategoryFilter = "all" }
                        )
                    }

                    // Categories Carousel (Matching CustomerApp.tsx in zip)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategorySquareCard(
                            title = "Dhammaan",
                            emoji = "✨",
                            isSelected = selectedCategoryFilter == "all",
                            onClick = { selectedCategoryFilter = "all" },
                            modifier = Modifier.width(82.dp)
                        )
                        CategorySquareCard(
                            title = "Dalbo Bajaaj",
                            emoji = "🛺",
                            isSelected = false,
                            onClick = { onNavigate("ride") },
                            modifier = Modifier.width(86.dp)
                        )
                        CategorySquareCard(
                            title = "Cunto",
                            emoji = "🍔",
                            isSelected = selectedCategoryFilter == "cunto",
                            onClick = { selectedCategoryFilter = "cunto" },
                            modifier = Modifier.width(82.dp)
                        )
                        CategorySquareCard(
                            title = "Supermarket",
                            emoji = "🛒",
                            isSelected = selectedCategoryFilter == "supermarket",
                            onClick = { selectedCategoryFilter = "supermarket" },
                            modifier = Modifier.width(90.dp)
                        )
                        CategorySquareCard(
                            title = "Daawo",
                            emoji = "💊",
                            isSelected = selectedCategoryFilter == "daawo",
                            onClick = { selectedCategoryFilter = "daawo" },
                            modifier = Modifier.width(82.dp)
                        )
                        CategorySquareCard(
                            title = "Gaarsiin",
                            emoji = "🏍️",
                            isSelected = false,
                            onClick = { onNavigate("delivery") },
                            modifier = Modifier.width(84.dp)
                        )
                        CategorySquareCard(
                            title = "Traffic Map",
                            emoji = "🚦",
                            isSelected = false,
                            onClick = { onNavigate("live_traffic") },
                            modifier = Modifier.width(86.dp)
                        )
                    }

                    // GANACSIYO CAAN AH (4) - Mogadishu Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filteredCount = famousStores.filter {
                            selectedCategoryFilter == "all" || it.category == selectedCategoryFilter
                        }.size
                        Text(
                            text = "GANACSIYO CAAN AH ($filteredCount)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Mogadishu",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                    }

                    // Business Store Cards (Image 1)
                    val storesToDisplay = famousStores.filter {
                        (selectedCategoryFilter == "all" || it.category == selectedCategoryFilter) &&
                        (customerSearchQuery.isBlank() || it.name.contains(customerSearchQuery, ignoreCase = true) ||
                         it.district.contains(customerSearchQuery, ignoreCase = true))
                    }

                    storesToDisplay.forEach { store ->
                        CustomerStoreCard(
                            store = store,
                            onClick = {
                                viewingStore = store
                            }
                        )
                    }

                    // Viewing Store Products Modal / Section if a store is tapped!
                    if (viewingStore != null) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.5.dp, Color(0xFFDC2626)),
                            shadowElevation = 4.dp,
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
                                    Column {
                                        Text(
                                            text = "Liiska Alaabta: ${viewingStore?.name}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "Dooro alaabta aad rabto inaad dalbato",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    IconButton(onClick = { viewingStore = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "Xir", tint = Color.Gray)
                                    }
                                }

                                Divider(color = Color(0xFFE2E8F0))

                                pizzaHouseProducts.forEach { product ->
                                    val qty = cartItems[product.id] ?: 0
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.size(46.dp),
                                                color = Color(0xFFF1F5F9)
                                            ) {
                                                AsyncImage(
                                                    model = product.imageUrl,
                                                    contentDescription = product.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = product.name,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Text(
                                                    text = "$${String.format("%.2f", product.price)} • ${product.category}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (qty > 0) {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFFFEF2F2),
                                                    border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                                    modifier = Modifier.size(28.dp).clickable {
                                                        if (qty == 1) cartItems.remove(product.id)
                                                        else cartItems[product.id] = qty - 1
                                                    }
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFFDC2626))
                                                    }
                                                }
                                                Text("$qty", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFDC2626),
                                                modifier = Modifier.size(28.dp).clickable {
                                                    cartItems[product.id] = qty + 1
                                                }
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("+", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (cartItemCount > 0) {
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { showCheckout = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Checkout - $cartItemCount alaab", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            } else {
                // ══════════════════════════════════════════════════════════════
                // 2. MERCHANT / BUSINESS APP VIEW (IMAGE 2)
                // ══════════════════════════════════════════════════════════════
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Merchant Profile Card (Image 2)
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
                                    color = Color(0xFFFEF2F2),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = "🍕", fontSize = 24.sp)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Pizza House Mogadishu",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Shiri Code: SHR-BO-1024 • Hodan",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Sound Alert Button (Image 2)
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSoundEnabled) Color(0xFFFEF2F2) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSoundEnabled) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
                                    modifier = Modifier.clickable {
                                        isSoundEnabled = !isSoundEnabled
                                        if (isSoundEnabled) {
                                            ArrivalSoundManager.playBajajHornSequence(context)
                                            Toast.makeText(context, "Dhawaaqa dalabyada: ON", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Dhawaaqa dalabyada: OFF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "Sound",
                                            tint = if (isSoundEnabled) Color(0xFFDC2626) else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Sound",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSoundEnabled) Color(0xFFDC2626) else Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section: "Liiska Alaabta & Qiimaha" + "+ Kudar Alaab Cusub" (Image 2)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liiska Alaabta & Qiimaha",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Isbeddel kasta wuxuu isla markiiba ka muuqanayaa Customer App",
                                fontSize = 10.5.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Big Red "+ Kudar Alaab Cusub" button (Image 2)
                        Button(
                            onClick = { showAddProductDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text(
                                text = "+ Kudar Alaab Cusub",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Product List Items (Image 2)
                    pizzaHouseProducts.forEach { product ->
                        MerchantProductCard(
                            product = product,
                            onToggleStock = {
                                val idx = pizzaHouseProducts.indexOf(product)
                                if (idx != -1) {
                                    pizzaHouseProducts[idx] = product.copy(inStock = !product.inStock)
                                    Toast.makeText(
                                        context,
                                        "${product.name}: ${if (!product.inStock) "Waa diyaar (Stock)" else "Wuu go'ay"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// ─── Helper UI Components ────────────────────────────────────────────────────

@Composable
private fun CategorySquareCard(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFDC2626) else Color.White,
        border = BorderStroke(1.dp, if (isSelected) Color(0xFFDC2626) else Color(0xFFE2E8F0)),
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = modifier
            .height(82.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CustomerStoreCard(
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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Store Photo
            Surface(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(66.dp),
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

            Column(modifier = Modifier.weight(1f)) {
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "★", fontSize = 12.sp, color = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "${store.rating}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = "${store.category.replaceFirstChar { it.uppercase() }} & Restaurant - ${store.district}",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${store.deliveryTimeMin} min",
                        fontSize = 11.sp,
                        color = Color(0xFF475569)
                    )
                    Text(text = "  •  ", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Text(
                        text = "Gaarsiin $${String.format("%.2f", store.deliveryFee)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
private fun MerchantProductCard(
    product: ShiriProductItem,
    onToggleStock: () -> Unit
) {
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image (as seen in Image 2)
            Surface(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(62.dp),
                color = Color(0xFFF1F5F9)
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$${String.format("%.2f", product.price)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFDC2626)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.category,
                    fontSize = 10.5.sp,
                    color = Color(0xFF64748B)
                )
            }

            // "Waa diyaar (Stock)" pill button (as seen in Image 2)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (product.inStock) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, if (product.inStock) Color(0xFFA7F3D0) else Color(0xFFFECACA)),
                modifier = Modifier.clickable { onToggleStock() }
            ) {
                Text(
                    text = if (product.inStock) "Waa diyaar (Stock)" else "Wuu go'ay (Out)",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (product.inStock) Color(0xFF059669) else Color(0xFFDC2626),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CustomerNavTabItem(
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
