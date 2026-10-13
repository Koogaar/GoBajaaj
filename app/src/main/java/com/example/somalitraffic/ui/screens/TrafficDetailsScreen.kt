package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliInteractiveMap
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.ui.theme.*

@Composable
fun TrafficDetailsScreen(
    intersectionId: String,
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit,
    onNavigateToAlternativeRoutes: () -> Unit
) {
    val intersections by viewModel.intersections.collectAsState()
    val intersection = intersections.find { it.id == intersectionId } ?: return

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = intersection.name,
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(Icons.Default.Share, contentDescription = "La wadaag", tint = Color.White)
                    }
                }
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
            // Interactive Map showing intersection
            SomaliInteractiveMap(
                intersections = listOf(intersection),
                selectedIntersectionId = intersection.id,
                blinkingIntersectionId = intersection.id,
                userLatitude = intersection.latitude,
                userLongitude = intersection.longitude,
                customUserPinLabel = intersection.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )

            Column(modifier = Modifier.padding(20.dp)) {
                // Status Row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = when (intersection.currentStatus.trim().lowercase()) {
                        "furan" -> TrafficOpenGreen
                        "normal" -> TrafficModerateYellow
                        "jaam" -> TrafficBusyOrange
                        "xeran" -> TrafficHeavyRed
                        else -> Color.Gray
                    }
                    Surface(
                        color = color,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = intersection.currentStatus, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = intersection.lastUpdated ?: "Hadda", fontSize = 14.sp, color = Color.Gray)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = intersection.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = intersection.roadNames.ifBlank { intersection.connectingRoads }.ifBlank { "Waddada weyn" },
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Hazard Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HazardBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CarCrash,
                        label = "Shilal",
                        count = if (intersection.hasAccident) "1" else "0"
                    )
                    HazardBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "Caqabad",
                        count = if (intersection.hasHazard) "1" else "0"
                    )
                    HazardBox(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Block,
                        label = "Jid Xiran",
                        count = if (intersection.hasClosure) "1" else "0"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Warbixin Ugu Dambeeyay",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                if (intersection.latestVerifiedReport.isNullOrBlank()) {
                    Text(
                        text = "Xog cusub oo saxmad ah lama hayo.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandLightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = intersection.latestVerifiedReport!!,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextBlack
                                )
                                Text(
                                    text = intersection.lastUpdated ?: "Dhowaan",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { /* TODO View Details */ },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Arag Faahfaahinta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onNavigateToAlternativeRoutes,
                    modifier = Modifier.fillMaxWidth().height(56.dp).border(1.dp, BrandBorder, RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Text("Jidka Kale", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                }
            }
        }
    }
}

@Composable
fun HazardBox(modifier: Modifier = Modifier, icon: ImageVector, label: String, count: String) {
    Column(
        modifier = modifier
            .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextBlack)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, color = TextBlack)
        Text(text = count, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextBlack)
    }
}
