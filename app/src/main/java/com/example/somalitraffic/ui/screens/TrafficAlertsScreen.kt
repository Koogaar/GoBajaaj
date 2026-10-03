package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.TrafficIncident
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.*
import com.example.ui.theme.*

@Composable
fun TrafficAlertsScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit
) {
    val incidents by viewModel.incidents.collectAsState()
    var selectedFilter by remember { mutableStateOf("Dhammaan") }
    val filterOptions = listOf("Dhammaan", "Shilal", "Caqabad", "Jid Xiran")

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Digniino"
            )
        },
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "traffic_alerts",
                onNavigate = onNavigate,
                isDriver = true
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) BrandRedPrimary else BrandWhite,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) BrandRedPrimary else BrandBorder),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextBlack,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            if (incidents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Waqtigan digniino cusub ma jiraan.",
                        fontSize = 14.sp,
                        color = TextBlack
                    )
                }
            } else {
                val filteredIncidents = incidents.filter {
                    when (selectedFilter) {
                        "Dhammaan" -> true
                        "Shilal" -> it.category.contains("Shil", ignoreCase = true)
                        "Caqabad" -> it.category.contains("Caqabad", ignoreCase = true)
                        "Jid Xiran" -> it.category.contains("Xiran", ignoreCase = true)
                        else -> true
                    }
                }

                if (filteredIncidents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Waqtigan digniino cusub ma jiraan.",
                            fontSize = 14.sp,
                            color = TextBlack
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredIncidents) { incident ->
                            IncidentAlertCard(incident = incident)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentAlertCard(incident: TrafficIncident) {
    val (iconBg, icon) = when (incident.category.lowercase()) {
        "shil" -> Pair(BrandRedPrimary, Icons.Default.CarCrash)
        "caqabad" -> Pair(TrafficModerateYellow, Icons.Default.Warning)
        "waddo xiran", "jid xiran" -> Pair(BrandRedPrimary, Icons.Default.Block)
        else -> Pair(BrandRedPrimary, Icons.Default.NotificationsActive)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = BrandWhite),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${incident.category} - ${incident.locationName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = incident.description ?: "",
                    fontSize = 14.sp,
                    color = TextBlack,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = incident.createdAt.split("T").firstOrNull() ?: incident.createdAt, // Or relative time
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
