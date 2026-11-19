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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.somalitraffic.data.model.ShiriBusinessCategory
import com.example.somalitraffic.data.model.ShiriMerchantStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriCategoryDetailScreen(
    category: ShiriBusinessCategory,
    stores: List<ShiriMerchantStore>,
    onBackClick: () -> Unit,
    onStoreClick: (ShiriMerchantStore) -> Unit,
    onSearchClick: () -> Unit
) {
    var selectedSubcat by remember { mutableStateOf("All") }

    val filterChips = remember(category) {
        listOf("All") + category.subcategories.map { it.nameEn.split(" ").first() }.distinct().take(6)
    }

    val categoryStores = remember(category, stores, selectedSubcat) {
        stores.filter { store ->
            val matchCat = store.categoryId == category.id || store.extraCategories.contains(category.id)
            val matchSub = selectedSubcat == "All" || store.subcategoryId.contains(selectedSubcat, ignoreCase = true)
            matchCat && matchSub
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
    ) {
        // ─── Top Bar: Back Arrow + Category Title + Search Icon ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BrandTextDark,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = category.nameEn,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = BrandTextDark
            )

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = BrandTextDark,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ─── Hero Food Banner ("Delicious food, delivered fast") ───
            item {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = category.bannerImageUrl,
                            contentDescription = category.nameEn,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x33000000),
                                            Color(0xCC000000)
                                        )
                                    )
                                )
                        )
                        Text(
                            text = "Delicious food, delivered fast",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        )
                    }
                }
            }

            // ─── Subcategories Filter Pills (All, Pizza, Burger, African, etc.) ───
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterChips.forEach { chip ->
                        val isSelected = selectedSubcat.equals(chip, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) BrandGreenPrimary else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) BrandGreenPrimary else BrandBorder),
                            modifier = Modifier.clickable { selectedSubcat = chip }
                        ) {
                            Text(
                                text = chip,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BrandTextDark,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // ─── Store List with (+) Add/View Action ───
            items(if (categoryStores.isNotEmpty()) categoryStores else stores.take(5)) { store ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onStoreClick(store) }
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
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(64.dp)
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
                                    color = BrandTextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = BrandStarYellow, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        text = "${store.rating}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextDark
                                    )
                                    Text(" • ", fontSize = 12.sp, color = BrandTextMuted)
                                    Text(
                                        text = "${store.deliveryTimeMin} min • $${String.format("%.2f", store.deliveryFee)}",
                                        fontSize = 12.sp,
                                        color = BrandTextMuted
                                    )
                                }
                            }
                        }

                        // Green (+) Action Button (Master Mockup style)
                        Surface(
                            shape = CircleShape,
                            color = BrandGreenPrimary,
                            modifier = Modifier
                                .size(32.dp)
                                .clickable { onStoreClick(store) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "View Store",
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
}

