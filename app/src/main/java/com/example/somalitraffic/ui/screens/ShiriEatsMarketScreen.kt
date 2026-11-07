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
import java.util.UUID

data class RegisteredBusiness(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val category: String, // "food" or "market"
    val district: String,
    val tillNumber: String,
    val phone: String,
    val description: String = "",
    val isVerified: Boolean = true
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
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddBusinessDialog by remember { mutableStateOf(false) }

    // Real dynamic list of businesses — NO hardcoded fake demo mockups
    val businessList = remember { mutableStateListOf<RegisteredBusiness>() }

    // Dialog state for adding a new business
    var newBizName by remember { mutableStateOf("") }
    var newBizType by remember { mutableStateOf("Maqaayad (Restaurant)") }
    var newBizDistrict by remember { mutableStateOf("Hodan, Muqdisho") }
    var newBizTill by remember { mutableStateOf("") }
    var newBizPhone by remember { mutableStateOf("") }
    var newBizDesc by remember { mutableStateOf("") }

    val businessTypeOptions = listOf(
        "Maqaayad (Restaurant)" to "food",
        "Dukaan / Supermarket" to "market",
        "Hilib & Khudaar" to "market",
        "Cabitaan & Macmacaan" to "food",
        "Farmashi (Pharmacy)" to "market",
        "Qalabka Elektaroonigga" to "market",
        "Dharka & Fashion" to "market"
    )

    // Filtered businesses
    val filteredBusinesses = remember(businessList.toList(), selectedTab, searchQuery, selectedCategoryFilter) {
        businessList.filter { biz ->
            biz.category == selectedTab &&
            (searchQuery.isBlank() || biz.name.contains(searchQuery, ignoreCase = true) ||
             biz.district.contains(searchQuery, ignoreCase = true) ||
             biz.type.contains(searchQuery, ignoreCase = true)) &&
            (selectedCategoryFilter == "All" || biz.type.contains(selectedCategoryFilter, ignoreCase = true))
        }
    }

    // Dialog for Admin / Merchant to register real business
    if (showAddBusinessDialog) {
        AlertDialog(
            onDismissRequest = { showAddBusinessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = null,
                        tint = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Diiwaangeli Meherad Cusub",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Geli xogta saxda ah ee ganacsigaaga ama maqaayaddaada.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    // Business Name
                    OutlinedTextField(
                        value = newBizName,
                        onValueChange = { newBizName = it },
                        label = { Text("Magaca Meheradda (Business Name)") },
                        placeholder = { Text("Tusaale: Dukaanka Taleex") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Business Type dropdown / chip selector
                    Text(
                        text = "Nooca Meheradda:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        businessTypeOptions.forEach { (typeLabel, _) ->
                            val isSel = newBizType == typeLabel
                            FilterChip(
                                selected = isSel,
                                onClick = { newBizType = typeLabel },
                                label = { Text(typeLabel, fontSize = 11.sp, maxLines = 1) }
                            )
                        }
                    }

                    // District
                    OutlinedTextField(
                        value = newBizDistrict,
                        onValueChange = { newBizDistrict = it },
                        label = { Text("Degmada / Goobta (District/Area)") },
                        placeholder = { Text("Taleex, Hodan, Bakaaro, KM4...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Phone Number
                    OutlinedTextField(
                        value = newBizPhone,
                        onValueChange = { newBizPhone = it },
                        label = { Text("Telefoonka Ganacsiga (Phone)") },
                        placeholder = { Text("61XXXXXXX") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Merchant Till Number
                    OutlinedTextField(
                        value = newBizTill,
                        onValueChange = { newBizTill = it },
                        label = { Text("Merchant Till / EVC Number (Ikhtiyaari)") },
                        placeholder = { Text("Tusaale: Till 39401") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Description
                    OutlinedTextField(
                        value = newBizDesc,
                        onValueChange = { newBizDesc = it },
                        label = { Text("Faahfaahin kooban (Description)") },
                        placeholder = { Text("Cunto, Raashin, Qalab...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBizName.isBlank()) {
                            Toast.makeText(context, "Fadlan geli magaca meheradda!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (newBizPhone.isBlank()) {
                            Toast.makeText(context, "Fadlan geli telefoonka ganacsiga!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val matchedCategory = businessTypeOptions.find { it.first == newBizType }?.second ?: "market"
                        businessList.add(
                            RegisteredBusiness(
                                name = newBizName.trim(),
                                type = newBizType.substringBefore(" ("),
                                category = matchedCategory,
                                district = newBizDistrict.trim(),
                                tillNumber = newBizTill.trim(),
                                phone = newBizPhone.trim(),
                                description = newBizDesc.trim()
                            )
                        )
                        Toast.makeText(context, "Meheradda '${newBizName.trim()}' waa la diiwaangeliyay!", Toast.LENGTH_SHORT).show()
                        newBizName = ""
                        newBizTill = ""
                        newBizPhone = ""
                        newBizDesc = ""
                        showAddBusinessDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Diiwaangeli Ganacsiga", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBusinessDialog = false }) {
                    Text("Ka noqo", color = Color.Gray)
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
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Dib u noqo",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (selectedTab == "food") "SHIRI FOOD" else "SHIRI MARKET",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (selectedTab == "food") "Maqaayadaha & Cuntada Muqdisho" else "Suuqa & Ganacsiyada Muqdisho",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Button to Add Business
                    Surface(
                        color = Color.White.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { showAddBusinessDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AddBusiness,
                                contentDescription = "Ku dar Ganacsi",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ku dar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🍔", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CUNTO & MAQAAYADO",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedTab == "food") Color.White else Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                            modifier = Modifier.padding(vertical = 9.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🛍️", fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SUUQA & DUKAAMADA",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                color = if (selectedTab == "market") Color.White else Color(0xFF475569),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        if (selectedTab == "food") "Raadi maqaayad, cunto, ama meel..."
                        else "Raadi dukaan, alaab, ama goob...",
                        fontSize = 12.5.sp
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            // Category Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filters = if (selectedTab == "food") {
                    listOf("All", "Maqaayad", "Cabitaan", "Fast Food")
                } else {
                    listOf("All", "Dukaan", "Hilib & Khudaar", "Farmashi", "Qalab")
                }

                filters.forEach { filter ->
                    val isSel = selectedCategoryFilter == filter
                    val activeColor = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSel) activeColor else Color.White,
                        border = BorderStroke(1.dp, if (isSel) activeColor else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { selectedCategoryFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Medium,
                            color = if (isSel) Color.White else Color(0xFF0F172A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedTab == "food") "Meheradaha Cuntada ee Diiwaangashan" else "Meheradaha Suuqa ee Diiwaangashan",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )

                Text(
                    text = "${filteredBusinesses.size} Meheradood",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Real Businesses List OR Empty State (Zero Demo Data)
            if (filteredBusinesses.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = (if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A)).copy(alpha = 0.1f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (selectedTab == "food") "Weli maqaayado ma diiwaangashana" else "Weli dukaamo ma diiwaangashana",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Admin ahaan ama Ganacsade ahaan, riix badhanka hoose si aad u diiwaangeliso meherad dhab ah.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showAddBusinessDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == "food") Color(0xFFEA580C) else Color(0xFF16A34A)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Diiwaangeli Meherad Cusub",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Display Real Registered Businesses
                filteredBusinesses.forEach { biz ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
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
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = (if (biz.category == "food") Color(0xFFEA580C) else Color(0xFF16A34A)).copy(alpha = 0.12f),
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (biz.category == "food") Icons.Default.Restaurant else Icons.Default.ShoppingBag,
                                        contentDescription = null,
                                        tint = if (biz.category == "food") Color(0xFFEA580C) else Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = biz.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Surface(
                                        color = Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = biz.type,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Text(
                                    text = "📍 ${biz.district}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (biz.tillNumber.isNotBlank()) {
                                    Text(
                                        text = "💳 Till: ${biz.tillNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Call Button
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${biz.phone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0FDF4))
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = "Wac",
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Informational Notice at the bottom
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Meherad kasta waxay leedahay Till Number toos ah oo lagu bixiyo EVC Plus / Hormuud.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
