#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/HomeDashboardScreen.kt
package com.example.somalitraffic.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.somalitraffic.R
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.data.model.TrafficStatus
import com.example.somalitraffic.data.repository.SearchResultItem
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.*
import com.example.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@Composable
fun HomeDashboardScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val intersections by viewModel.intersections.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    
    val openCount = intersections.count { it.currentStatus == "Furan" }
    val moderateCount = intersections.count { it.currentStatus == "Dhexdhexaad" }
    val busyCount = intersections.count { it.currentStatus == "Mashquul" }
    val heavyCount = intersections.count { it.currentStatus == "Saxmad badan" }

    Scaffold(
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "home",
                onNavigate = onNavigate
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // RED HEADER SECTION
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = BrandRedPrimary,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    // Avatar & Name Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Salaan, ${currentUser?.fullName?.split(" ")?.firstOrNull() ?: "Darawal"}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Darawal",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                        IconButton(onClick = { onNavigate("traffic_alerts") }) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Digniino", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Driver Pass Card inside Header
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("driver_pass") },
                        colors = CardDefaults.cardColors(containerColor = BrandRedDeep),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Driver Pass", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("$1 / Bishii", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f))
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
                        }
                        Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.1f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text("Ku hel faa'iidooyinka badan oo darawalnimo", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // WHITE BODY SECTION
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                
                Text(
                    text = "Xaaladda Waddooyinka",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusDot(TrafficOpenGreen, "Furan", openCount)
                    StatusDot(TrafficModerateYellow, "Dhexdhexaad", moderateCount)
                    StatusDot(TrafficBusyOrange, "Mashquul", busyCount)
                    StatusDot(TrafficHeavyRed, "Saxmad badan", heavyCount)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big Red Search Button
                Button(
                    onClick = { onNavigate("live_traffic") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_raadi_isgoys"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Raadi Isgoys ama Waddo",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Quick Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickAction(Icons.Outlined.Map, "Kharidadda") { onNavigate("live_traffic") }
                    QuickAction(Icons.Outlined.Notifications, "Digniino") { onNavigate("traffic_alerts") }
                    QuickAction(Icons.Outlined.Route, "Waddooyinka") { onNavigate("live_traffic") }
                    QuickAction(Icons.Outlined.History, "Taariikho") { onNavigate("traffic_history") }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Traffic-ka Maanta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Today's Traffic List (Top intersections or random sample)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    intersections.take(3).forEach { intersection ->
                        TrafficMaantaItem(intersection = intersection, onClick = {
                            viewModel.selectIntersection(intersection)
                            onNavigate("traffic_details/${intersection.id}")
                        })
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatusDot(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = TextBlack)
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = count.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextBlack)
    }
}

@Composable
fun QuickAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(BrandLightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = TextBlack)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = TextBlack)
    }
}

@Composable
fun TrafficMaantaItem(intersection: Intersection, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandLightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandRedPrimary, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = intersection.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextBlack)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val color = when (intersection.currentStatus) {
                "Furan" -> TrafficOpenGreen
                "Dhexdhexaad" -> TrafficModerateYellow
                "Mashquul" -> TrafficBusyOrange
                "Saxmad badan" -> TrafficHeavyRed
                else -> Color.Gray
            }
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = intersection.currentStatus, fontSize = 12.sp, color = color)
        }
    }
}
INNER_EOF
