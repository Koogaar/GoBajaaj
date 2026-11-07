package com.example.somalitraffic.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiriDeliveryScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()

    var selectedServiceType by remember { mutableStateOf("package") } // "package", "shopping", "food"
    var pickupLocation by remember { mutableStateOf("KM4, Hodan, Muqdisho") }
    var dropoffLocation by remember { mutableStateOf("") }
    var recipientPhone by remember { mutableStateOf("") }
    var packageNotes by remember { mutableStateOf("") }
    var deliverySpeed by remember { mutableStateOf("express") } // "express", "standard"
    var showSuccessDialog by remember { mutableStateOf(false) }
    var orderTrackingId by remember { mutableStateOf("") }

    val fareAmount = when {
        selectedServiceType == "shopping" -> if (deliverySpeed == "express") 3.50 else 2.50
        deliverySpeed == "express" -> 2.50
        else -> 1.50
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
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
                        text = "Dalabka Gaarsiinta Waa La Diray!",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Mooto Gaarsiiye ayaa loo qoondeeyay dalabkaaga. Waxaad kula socon kartaa lambarka:",
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569)
                    )
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tracking ID: $orderTrackingId",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0284C7),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Text(
                        text = "Qiimaha: \$$fareAmount • Qaabka: EVC Plus / Kaash",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
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
                color = Color(0xFF0284C7),
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
                            text = "SHIRI DELIVERY",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Mooto Express & Gaarsiinta Muqdisho",
                            fontSize = 11.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF86EFAC))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Mooto Live",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner matching Phone 4 in Poster
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
                                listOf(Color(0xFFE0F2FE), Color(0xFFF0F9FF), Color(0xFFFFFFFF))
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
                                color = Color(0xFF0284C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "MOOTO EXPRESS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Fast Delivery",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Anytime, Anywhere Muqdisho",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF0369A1)
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.TwoWheeler,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3 Delivery Service Options (matching Poster)
            Text(
                text = "Dooro Nooca Gaarsiinta",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF0F172A)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeliveryServiceCard(
                    title = "Send Package",
                    subtitle = "Documents, Goods",
                    emoji = "📦",
                    isSelected = selectedServiceType == "package",
                    onClick = { selectedServiceType = "package" },
                    modifier = Modifier.weight(1f)
                )
                DeliveryServiceCard(
                    title = "Send Shopping",
                    subtitle = "Market Items",
                    emoji = "🛍️",
                    isSelected = selectedServiceType == "shopping",
                    onClick = { selectedServiceType = "shopping" },
                    modifier = Modifier.weight(1f)
                )
                DeliveryServiceCard(
                    title = "Send Food",
                    subtitle = "Restaurants",
                    emoji = "🍔",
                    isSelected = selectedServiceType == "food",
                    onClick = { selectedServiceType = "food" },
                    modifier = Modifier.weight(1f)
                )
            }

            // Delivery Form Details
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Faahfaahinta Gaarsiinta",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )

                    // Pickup
                    OutlinedTextField(
                        value = pickupLocation,
                        onValueChange = { pickupLocation = it },
                        label = { Text("Goobta laga qaadayo (Pickup)") },
                        leadingIcon = {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF0284C7))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Dropoff
                    OutlinedTextField(
                        value = dropoffLocation,
                        onValueChange = { dropoffLocation = it },
                        label = { Text("Goobta la geynayo (Dropoff Destination)") },
                        placeholder = { Text("Tusaale: Taleex, Bakaaro, Sanaa...") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Recipient Phone
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = { recipientPhone = it },
                        label = { Text("Telefoonka Qaataha (Recipient Phone)") },
                        placeholder = { Text("61XXXXXXX ama 68XXXXXXX") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color(0xFF16A34A))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Notes / Description
                    OutlinedTextField(
                        value = packageNotes,
                        onValueChange = { packageNotes = it },
                        label = { Text("Tilmaam / Nooca Xirmada (Ikhtiyaari)") },
                        placeholder = { Text("Waraaqo, Baakad yar, Dukaanka Taleex...") },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFF64748B))
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Speed & Price Selection
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Qiimaha Gaarsiinta (Estimated Fare)",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "\$$fareAmount",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0284C7)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = deliverySpeed == "express",
                            onClick = { deliverySpeed = "express" },
                            label = { Text("Express (20-30m)") }
                        )
                        FilterChip(
                            selected = deliverySpeed == "standard",
                            onClick = { deliverySpeed = "standard" },
                            label = { Text("Standard (1h)") }
                        )
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (dropoffLocation.isBlank()) {
                        Toast.makeText(context, "Fadlan geli goobta la geynayo (Dropoff)!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (recipientPhone.isBlank()) {
                        Toast.makeText(context, "Fadlan geli telefoonka qaataha!", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    orderTrackingId = "SH-DEL-" + (1000..9999).random()
                    showSuccessDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dalbo Mooto Gaarsiiye Hadda →",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun DeliveryServiceCard(
    title: String,
    subtitle: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0)
        ),
        color = if (isSelected) Color(0xFFEFF6FF) else Color.White,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color(0xFF64748B),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

