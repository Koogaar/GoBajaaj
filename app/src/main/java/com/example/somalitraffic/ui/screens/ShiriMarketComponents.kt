package com.example.somalitraffic.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.somalitraffic.data.model.*

// ══════════════════════════════════════════════════════════════════════════════
// SHIRI BRAND COLOR SYSTEM (MATCHING MASTER MOCKUP)
// ══════════════════════════════════════════════════════════════════════════════
val BrandGreenPrimary = Color(0xFF00A859)        // Signature Vibrant Green
val BrandGreenDark = Color(0xFF008746)           // Forest Green for depth
val BrandGreenLight = Color(0xFFE8F8F0)          // Mint background for chips
val BrandGreenGradientStart = Color(0xFF00B05F)  // Banner gradient start
val BrandGreenGradientEnd = Color(0xFF008746)    // Banner gradient end
val BrandBgLight = Color(0xFFF8FAFC)            // Crisp light-gray background
val BrandCardWhite = Color(0xFFFFFFFF)          // Card surface
val BrandTextDark = Color(0xFF0F172A)           // High-contrast slate dark text
val BrandTextMuted = Color(0xFF64748B)          // Secondary muted text
val BrandBorder = Color(0xFFE2E8F0)             // Subtle divider border
val BrandStarYellow = Color(0xFFF59E0B)         // Star rating yellow
val BrandRejectRed = Color(0xFFEF4444)          // Rejection red
val BrandRejectBg = Color(0xFFFEE2E2)           // Rejection background

// ══════════════════════════════════════════════════════════════════════════════
// 1. SHIRI SPEED-BAG LOGO COMPOSABLE
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriBrandLogo(
    modifier: Modifier = Modifier,
    iconSize: Int = 26,
    textSize: Int = 22,
    showTagline: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Speed lines + shopping bag icon
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = BrandGreenPrimary,
            modifier = Modifier.size(iconSize.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "SHIRI",
                    tint = Color.White,
                    modifier = Modifier.size((iconSize * 0.65).dp)
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = "SHIRI",
                fontSize = textSize.sp,
                fontWeight = FontWeight.Black,
                color = BrandGreenPrimary,
                letterSpacing = 1.sp
            )
            if (showTagline) {
                Text(
                    text = "Fast • Reliable • For Everyone",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = BrandGreenDark
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 2. TOP MARKETPLACE HEADER (Location, Change, Notifications, Brand)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriMarketplaceHeader(
    currentLocation: String,
    notificationCount: Int = 2,
    onLocationClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Row: Location & Notification Bell
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Location: Pin + Mogadishu ▾ + Change
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLocationClick() }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = BrandGreenPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = currentLocation.ifBlank { "Mogadishu" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextDark
                )
                Spacer(Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = BrandTextDark,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BrandGreenLight,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Change",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGreenDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Notification Bell
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .clickable { onNotificationClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Notifications",
                    tint = BrandTextDark,
                    modifier = Modifier.size(18.dp)
                )
                if (notificationCount > 0) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(15.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$notificationCount",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Brand Logo Row: SHIRI with Speed Bag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShiriBrandLogo(iconSize = 28, textSize = 24, showTagline = false)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 3. CAPSULE SEARCH BAR ("Maxaad maanta rabtaa?")
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriMarketSearchBar(
    query: String,
    placeholder: String = "Maxaad maanta rabtaa?",
    onQueryChange: (String) -> Unit,
    onVoiceClick: () -> Unit,
    onSearchClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BrandBorder),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onSearchClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                if (query.isBlank()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.5.sp,
                        color = BrandTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = query,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrandTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onVoiceClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Voice Search",
                    tint = BrandGreenPrimary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 4. SPECIAL OFFER BANNER (GREEN GRADIENT - MASTER MOCKUP)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriSpecialOfferBanner(
    title: String = "Special Offer",
    headline: String = "Get 20% OFF",
    subtext: String = "on Food Orders",
    actionText: String = "Order Now →",
    imageUrl: String = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400&auto=format&fit=crop&q=80",
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrandGreenPrimary,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(BrandGreenGradientStart, BrandGreenGradientEnd)
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
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = headline,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = subtext,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = actionText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Food Pizza Image in rounded container
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    modifier = Modifier.size(90.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Offer",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 5. CIRCULAR CATEGORY ITEM (MASTER MOCKUP STYLE)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriCircularCategoryItem(
    title: String,
    iconEmoji: String,
    bgColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = if (isSelected) BrandGreenPrimary else bgColor,
            modifier = Modifier.size(54.dp),
            shadowElevation = if (isSelected) 2.dp else 0.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = iconEmoji,
                    fontSize = 22.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
            color = if (isSelected) BrandGreenPrimary else BrandTextDark,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 6. POPULAR STORE HORIZONTAL CARD (PIZZA PALACE, FRESH MARKET, ETC.)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriPopularStoreCard(
    store: ShiriMerchantStore,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandCardWhite,
        border = BorderStroke(1.dp, BrandBorder),
        shadowElevation = 1.dp,
        modifier = Modifier
            .width(220.dp)
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
                // Delivery time badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🕒 ${store.deliveryTimeMin} min",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = store.name,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = BrandStarYellow,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "${store.rating} (1.2k)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextDark
                    )
                    Text("•", fontSize = 11.sp, color = BrandTextMuted)
                    Text(
                        text = "25 min • $${String.format("%.2f", store.deliveryFee)}",
                        fontSize = 11.sp,
                        color = BrandTextMuted
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 7. PRODUCT ITEM CARD WITH GREEN (+) BUTTON (MASTER MOCKUP)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriProductRowItem(
    product: ShiriProductItem,
    quantityInCart: Int = 0,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = BrandCardWhite,
        border = BorderStroke(1.dp, BrandBorder),
        shadowElevation = 0.5.dp,
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
                // Product Thumbnail
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.size(64.dp)
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (product.description.isNotBlank()) {
                        Text(
                            text = product.description,
                            fontSize = 11.sp,
                            color = BrandTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$${String.format("%.2f", product.price)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandGreenPrimary
                    )
                }
            }

            // Green (+) button or quantity selector
            if (quantityInCart == 0) {
                Surface(
                    shape = CircleShape,
                    color = BrandGreenPrimary,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { onAddClick() }
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
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onRemoveClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandTextDark)
                        }
                    }
                    Text(
                        text = "$quantityInCart",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandTextDark
                    )
                    Surface(
                        shape = CircleShape,
                        color = BrandGreenPrimary,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { onAddClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 8. SALES OVERVIEW AREA CHART (SMOOTH GREEN CURVE - MASTER MOCKUP)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriSalesOverviewChart(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
) {
    val points = listOf(20f, 45f, 30f, 60f, 50f, 85f, 95f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)
        val maxVal = 100f

        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - (value / maxVal) * (height - 20f) - 10f

            if (index == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (index - 1) * stepX
                val prevY = height - (points[index - 1] / maxVal) * (height - 20f) - 10f
                val controlX1 = prevX + (x - prevX) / 2
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2
                val controlY2 = y
                strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }

            if (index == points.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw gradient area fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    BrandGreenPrimary.copy(alpha = 0.35f),
                    BrandGreenPrimary.copy(alpha = 0.02f)
                )
            )
        )

        // Draw smooth green line
        drawPath(
            path = strokePath,
            color = BrandGreenPrimary,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 9. WEEKLY EARNINGS BAR CHART (DRIVER APP - MASTER MOCKUP)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriWeeklyEarningsBarChart(
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(130.dp)
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val heights = listOf(0.35f, 0.50f, 0.40f, 0.65f, 0.55f, 0.85f, 0.95f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEachIndexed { i, day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .fillMaxHeight(heights[i])
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(BrandGreenPrimary)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = day,
                    fontSize = 10.sp,
                    color = BrandTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 10. SAVED BUSINESSES & FAVORITE ITEMS SCREEN (DESTINATION 4)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriSavedScreen(
    stores: List<ShiriMerchantStore>,
    onStoreClick: (ShiriMerchantStore) -> Unit
) {
    var savedTab by remember { mutableStateOf("stores") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Saved",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = BrandTextDark
        )

        // Tabs: Businesses | Products
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (savedTab == "stores") BrandGreenPrimary else Color.White,
                border = BorderStroke(1.dp, if (savedTab == "stores") BrandGreenPrimary else BrandBorder),
                modifier = Modifier.clickable { savedTab = "stores" }
            ) {
                Text(
                    text = "Businesses (3)",
                    fontSize = 12.sp,
                    fontWeight = if (savedTab == "stores") FontWeight.Bold else FontWeight.Medium,
                    color = if (savedTab == "stores") Color.White else BrandTextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (savedTab == "products") BrandGreenPrimary else Color.White,
                border = BorderStroke(1.dp, if (savedTab == "products") BrandGreenPrimary else BrandBorder),
                modifier = Modifier.clickable { savedTab = "products" }
            ) {
                Text(
                    text = "Favorite Items (5)",
                    fontSize = 12.sp,
                    fontWeight = if (savedTab == "products") FontWeight.Bold else FontWeight.Medium,
                    color = if (savedTab == "products") Color.White else BrandTextDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stores.take(4)) { store ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier
                        .fillMaxWidth()
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
                            Column {
                                Text(
                                    text = store.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextDark
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = BrandStarYellow, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${store.rating}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandTextDark)
                                    Text(" • ${store.deliveryTimeMin} min • $${String.format("%.2f", store.deliveryFee)}", fontSize = 11.5.sp, color = BrandTextMuted)
                                }
                            }
                        }

                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Saved",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// 11. CUSTOMER PROFILE VIEW (DESTINATION 5)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun ShiriCustomerProfileView(
    currentUser: com.example.somalitraffic.data.model.UserProfile?,
    selectedDistrict: String,
    isSomaliLanguage: Boolean,
    onLanguageToggle: () -> Unit,
    onSwitchToMerchant: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (isSomaliLanguage) "Koontada" else "Profile",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = BrandTextDark
        )

        // User Avatar & Info Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = BrandGreenLight,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandGreenDark,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = currentUser?.fullName ?: if (isSomaliLanguage) "Macmiilka SHIRI" else "SHIRI Customer",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = BrandTextDark
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = currentUser?.phoneNumber ?: "0612345678",
                        fontSize = 12.5.sp,
                        color = BrandTextMuted
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandGreenLight
                    ) {
                        Text(
                            text = "Macmiil (Customer)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // SWITCH TO BUSINESS / MERCHANT MODE (Highlighted Card)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.5.dp, BrandGreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSwitchToMerchant() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BrandGreenLight,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = BrandGreenDark, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isSomaliLanguage) "U Gudub Meheraddaada" else "Switch to Merchant Mode",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextDark
                        )
                        Text(
                            text = if (isSomaliLanguage) "Maamul dukaanka, alaabta & dalabyada" else "Manage store, products & orders",
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                    }
                }
                Icon(Icons.Default.ArrowForward, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
            }
        }

        // Settings Section
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Address
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isSomaliLanguage) "Ciwaanka Gaarsiinta" else "Delivery Address",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandTextDark
                        )
                    }
                    Text(
                        text = selectedDistrict,
                        fontSize = 12.sp,
                        color = BrandGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = BrandBorder)

                // Language
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLanguageToggle() },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isSomaliLanguage) "Luuqadda (Language)" else "Language",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandTextDark
                        )
                    }
                    Text(
                        text = if (isSomaliLanguage) "Af-Soomaali" else "English",
                        fontSize = 12.sp,
                        color = BrandGreenPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                HorizontalDivider(color = BrandBorder)

                // Support
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (isSomaliLanguage) "Caawinaad & Taageero" else "Help & Support",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandTextDark
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Logout Button
        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color(0xFFEF4444)),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isSomaliLanguage) "Ka Bax Koontada (Logout)" else "Log Out",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444)
            )
        }
    }
}

