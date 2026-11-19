package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.somalitraffic.data.model.ShiriMerchantStore
import com.example.somalitraffic.data.model.ShiriProductItem

@Composable
fun ShiriCartScreen(
    store: ShiriMerchantStore?,
    cartItems: Map<String, Int>,
    onBackClick: () -> Unit,
    onClearCart: () -> Unit,
    onQuantityChange: (String, Int) -> Unit,
    onCheckoutClick: () -> Unit
) {
    val storeProducts = store?.products ?: emptyList()
    val cartProductList = remember(cartItems, storeProducts) {
        storeProducts.filter { (cartItems[it.id] ?: 0) > 0 }
    }

    val subtotal = cartProductList.sumOf { (cartItems[it.id] ?: 0) * it.price }
    val deliveryFee = store?.deliveryFee ?: 1.00
    val serviceFee = 0.50
    val total = if (subtotal > 0) subtotal + deliveryFee + serviceFee else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ─── Top Bar: Back, "Cart", Trash ───
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
                text = "Cart",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = BrandTextDark
            )

            IconButton(
                onClick = onClearCart,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Clear",
                    tint = BrandTextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (cartProductList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = BrandTextMuted,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Gaadhigaagu waa maran yahay",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextDark
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Ku dar cunto ama alaab dukaanka",
                        fontSize = 12.5.sp,
                        color = BrandTextMuted
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ─── Store Header Banner ───
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            AsyncImage(
                                model = store?.imageUrl ?: "",
                                contentDescription = store?.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = store?.name ?: "Store",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextDark
                            )
                            Text(
                                text = "🕒 ${store?.deliveryTimeMin ?: 25} min",
                                fontSize = 11.5.sp,
                                color = BrandTextMuted
                            )
                        }
                    }
                }

                // ─── Cart Items List ───
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cartProductList) { product ->
                        val qty = cartItems[product.id] ?: 0
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BrandBorder),
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
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = product.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandTextDark
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "$${String.format("%.2f", product.price)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = BrandGreenPrimary
                                        )
                                    }
                                }

                                // [-] [ qty ] [+] Counter (Master Mockup style)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clickable { onQuantityChange(product.id, qty - 1) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandTextDark)
                                        }
                                    }
                                    Text(
                                        text = "$qty",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextDark,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = BrandGreenLight,
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clickable { onQuantityChange(product.id, qty + 1) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandGreenDark)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ─── Price Breakdown & Calculation ───
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 13.sp, color = BrandTextMuted)
                            Text("$${String.format("%.2f", subtotal)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandTextDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Delivery Fee", fontSize = 13.sp, color = BrandTextMuted)
                            Text("$${String.format("%.2f", deliveryFee)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandTextDark)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Service Fee", fontSize = 13.sp, color = BrandTextMuted)
                            Text("$${String.format("%.2f", serviceFee)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandTextDark)
                        }
                        HorizontalDivider(color = BrandBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total", fontSize = 16.sp, fontWeight = FontWeight.Black, color = BrandTextDark)
                            Text("$${String.format("%.2f", total)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = BrandGreenPrimary)
                        }
                    }
                }

                // ─── Big Green Checkout Button ───
                Button(
                    onClick = onCheckoutClick,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Checkout →",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

