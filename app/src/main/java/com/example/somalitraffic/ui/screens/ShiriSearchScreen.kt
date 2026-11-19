package com.example.somalitraffic.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.somalitraffic.data.model.ShiriMerchantStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriSearchScreen(
    stores: List<ShiriMerchantStore>,
    onStoreClick: (ShiriMerchantStore) -> Unit,
    onCategoryClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val recentSearches = remember { mutableStateListOf("Pizza", "Grocery", "Pharmacy") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredStores = remember(searchQuery, selectedFilter) {
        stores.filter { store ->
            val matchesQuery = searchQuery.isBlank() ||
                store.name.contains(searchQuery, ignoreCase = true) ||
                store.products.any { it.name.contains(searchQuery, ignoreCase = true) } ||
                store.categoryId.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "Under 20 min" -> store.deliveryTimeMin <= 20
                "Free Delivery" -> store.deliveryFee <= 0.50
                "Top Rated" -> store.rating >= 4.7
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ─── Header: Search Title ───
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Search",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = BrandTextDark
            )
        }

        // ─── Search Input Bar ───
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search businesses, products...",
                    fontSize = 13.5.sp,
                    color = BrandTextMuted
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = BrandTextMuted, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = BrandGreenPrimary,
                unfocusedBorderColor = BrandBorder
            )
        )

        // ─── Filter Chips Row ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Top Rated", "Under 20 min", "Free Delivery").forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) BrandGreenPrimary else Color.White,
                    border = BorderStroke(1.dp, if (isSelected) BrandGreenPrimary else BrandBorder),
                    modifier = Modifier
                        .clickable { selectedFilter = filter }
                ) {
                    Text(
                        text = filter,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else BrandTextDark,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (searchQuery.isBlank()) {
            // ─── Recent Searches Section ───
            if (recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Searches",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextDark
                    )
                    Text(
                        text = "Clear all",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandGreenPrimary,
                        modifier = Modifier.clickable { recentSearches.clear() }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentSearches.forEach { term ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BrandBorder),
                            modifier = Modifier.clickable { searchQuery = term }
                        ) {
                            Text(
                                text = term,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = BrandTextDark,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // ─── Categories Shortcut Grid (Restaurants, Groceries, Pharmacy, Clothing) ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextDark
                )
                Text(
                    text = "See all >",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandGreenPrimary,
                    modifier = Modifier.clickable { onCategoryClick("all") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Triple("Restaurants", "🍔", "cunto"),
                    Triple("Groceries", "🛒", "supermarket"),
                    Triple("Pharmacy", "💊", "daawo"),
                    Triple("Clothing", "👗", "fashion")
                ).forEach { (name, emoji, catId) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(72.dp)
                            .clickable { onCategoryClick(catId) }
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, BrandBorder),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 22.sp)
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandTextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // ─── Top Businesses Section ───
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Businesses",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextDark
                )
                Text(
                    text = "See all >",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandGreenPrimary,
                    modifier = Modifier.clickable { }
                )
            }
        }

        // ─── Businesses List ───
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredStores) { store ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onStoreClick(store) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.size(60.dp)
                        ) {
                            AsyncImage(
                                model = store.imageUrl,
                                contentDescription = store.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = store.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextDark
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (store.categoryId == "cunto") "Food" else if (store.categoryId == "supermarket") "Grocery" else "Pharmacy",
                                    fontSize = 11.5.sp,
                                    color = BrandTextMuted
                                )
                                Text(" • ", fontSize = 11.5.sp, color = BrandTextMuted)
                                Icon(Icons.Default.Star, contentDescription = null, tint = BrandStarYellow, modifier = Modifier.size(13.dp))
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    text = "${store.rating}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextDark
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${store.deliveryTimeMin} min • $${String.format("%.2f", store.deliveryFee)}",
                                fontSize = 11.sp,
                                color = BrandTextMuted
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = BrandTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

