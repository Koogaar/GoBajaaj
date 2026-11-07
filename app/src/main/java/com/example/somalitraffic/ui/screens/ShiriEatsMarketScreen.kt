package com.example.somalitraffic.ui.screens

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel

data class FoodRestaurant(
    val name: String,
    val location: String,
    val rating: String,
    val reviewCount: String,
    val time: String,
    val fee: String,
    val popularItem: String,
    val price: Double,
    val emoji: String
)

data class MarketProduct(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val unit: String,
    val emoji: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriEatsMarketScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf("food") } // "food" or "market"
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var cartCount by remember { mutableStateOf(0) }
    var cartTotal by remember { mutableStateOf(0.0) }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val restaurants = listOf(
        FoodRestaurant("Pizza House", "Hodan, Taleex", "4.7", "1.2k", "25-30 min", "$2.50", "Pizza Margherita Special", 6.00, "🍕"),
        FoodRestaurant("Royal Restaurant", "KM4, Wadada Garoonka", "4.6", "856", "20-25 min", "$2.00", "Bariis & Hilib Geel", 5.00, "🍛"),
        FoodRestaurant("Al-Basha Fast Food", "Maka Al-Mukarama", "4.8", "2.1k", "15-20 min", "$1.50", "Crispy Burger & Fries", 4.00, "🍔"),
        FoodRestaurant("Bakaaro Sweet & Coffee", "Suuqa Bakaaro", "4.5", "640", "30-35 min", "$2.50", "Sambusa & Somali Spiced Tea", 2.00, "☕")
    )

    val products = listOf(
        MarketProduct("1", "Biyo Caafimaad (1.5L x 6)", "Biyo", 2.00, "Kartoon", "💧"),
        MarketProduct("2", "Bariis Basmati 5kg", "Raashin", 8.50, "5kg Boorso", "🍚"),
        MarketProduct("3", "Saliid Cadceedda 3L", "Raashin", 6.00, "Gallon", "🌻"),
        MarketProduct("4", "Sokor Macaan 5kg", "Raashin", 5.00, "5kg Boorso", "🍬"),
        MarketProduct("5", "Caano Boore 900g", "Caano", 9.00, "Daasad", "🥛"),
        MarketProduct("6", "Baasto Saldaato 1kg", "Raashin", 1.80, "Xirmo", "🍝"),
        MarketProduct("7", "Shaah Soomaali Cagaaran", "Cabitaan", 2.00, "Xirmo", "🍵"),
        MarketProduct("8", "Saabuunta Nadiifinta Dhaqda", "Guriga", 3.00, "Xirmo", "🧼")
    )

    if (showCheckoutDialog) {
        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF16A34A),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Dalabkaaga Waa La Guddoomay!",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Waxaad dalbatay $cartCount alaab. Wadarta qiimaha:",
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569)
                    )
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Wadarta: \$${String.format("%.2f", cartTotal + 1.50)} (Alaab + $1.50 Gaarsiin)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF16A34A),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Gaarsiiye Mooto SHIRI ayaa kuu keenaya 20-30 daqiiqo gudahood. Lacagta ku bixi EVC Plus.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckoutDialog = false
                        cartCount = 0
                        cartTotal = 0.0
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Waayahay (OK)", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                color = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Dib u noqo",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == "food") "SHIRI FOOD" else "SHIRI MARKET",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (selectedTab == "food") "Maqaayadaha Muqdisho • Cunto Diirran" else "Suuqa Bakaaro & Dukaamada Muqdisho",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    // Cart indicator
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable {
                            if (cartCount > 0) showCheckoutDialog = true
                            else Toast.makeText(context, "Kalluunka/Gaadhigu waa madhan yahay. Ku dar alaab!", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "$cartCount (\$$cartTotal)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Tabs: SHIRI FOOD vs SHIRI MARKET
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Tab 1: Food
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == "food") Color(0xFFEA580C) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = "food" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🍔", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SHIRI FOOD",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedTab == "food") Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    // Tab 2: Market
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selectedTab == "market") Color(0xFF16A34A) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedTab = "market" }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛍️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SHIRI MARKET",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedTab == "market") Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            // Search Bar matching Poster
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (selectedTab == "food") "Search restaurants or meals..."
                        else "Search products (Biyo, Bariis, Saliid)..."
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // Hero Banner matching Poster (Phone 3 / Phone 5)
            if (selectedTab == "food") {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFFF7ED), Color(0xFFFFEDD5), Color(0xFFFFFFFF))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color(0xFFEA580C),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "CUNTO DIIRRAN",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Delicious Food",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Delivered to your Doorstep",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFC2410C)
                                )
                            }
                            Text(text = "🍔", fontSize = 48.sp)
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7), Color(0xFFFFFFFF))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = Color(0xFF16A34A),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "SUUQA BAKAARO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Shop Smart",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Live Better • Qiimo Jaban",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF15803D)
                                )
                            }
                            Text(text = "🛒", fontSize = 48.sp)
                        }
                    }
                }
            }

            // Category Chips matching Poster
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val categories = if (selectedTab == "food") {
                    listOf("All", "🍕 Pizza", "🍔 Burger", "🍛 Somali Food", "🍗 Digaag", "🥤 Cabitaan")
                } else {
                    listOf("All", "🥦 Raashin", "💧 Biyo", "💻 Electronics", "🏠 Guriga", "👗 Dharka")
                }

                categories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    val activeColor = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) activeColor else Color.White,
                        border = BorderStroke(1.dp, if (isSelected) activeColor else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { selectedCategory = cat }
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Items List
            if (selectedTab == "food") {
                Text(
                    text = "Popular Restaurants in Mogadishu",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                restaurants.forEach { rest ->
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
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFF7ED),
                                modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = rest.emoji, fontSize = 28.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = rest.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = rest.location,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "⭐ ${rest.rating} (${rest.reviewCount})", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                    Text(text = " • ⏱️ ${rest.time}", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                                Text(
                                    text = "${rest.popularItem} (\$$rest.price)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEA580C)
                                )
                            }
                            Button(
                                onClick = {
                                    cartCount++
                                    cartTotal += rest.price
                                    Toast.makeText(context, "${rest.name}: Ku dartay dalabka! (\$${rest.price})", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Order", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Top Products (Bakaaro Market)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                products.forEach { prod ->
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
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4),
                                modifier = Modifier.size(50.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = prod.emoji, fontSize = 26.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = prod.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "${prod.unit} • ${prod.category}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "\$${prod.price}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF16A34A)
                                )
                            }
                            Button(
                                onClick = {
                                    cartCount++
                                    cartTotal += prod.price
                                    Toast.makeText(context, "${prod.name}: Waa lagu daray Kalluunka! (\$${prod.price})", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("+ Add", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Checkout Bar if items in cart
            if (cartCount > 0) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCheckoutDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "$cartCount Alaab Doortay",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Wadarta: \$$cartTotal",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Gudbi Dalabka (Checkout) →",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

