package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.Intersection
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.somalitraffic.ui.components.TrafficStatusBadge
import com.example.ui.theme.*

@Composable
fun TrafficHistoryScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val intersections by viewModel.intersections.collectAsState()
    var selectedPeriod by remember { mutableStateOf("Maanta") }
    val periods = listOf("Maanta", "Shalay", "7 Maalmood", "30 Maalmood")

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Taariikhda Saxmadda",
                onBackClick = onBackClick
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                periods.forEach { period ->
                    val isSelected = selectedPeriod == period
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPeriod = period },
                        label = { Text(period, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRedPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(intersections) { item ->
                    HistoryItemCard(item = item, period = selectedPeriod)
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: Intersection, period: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = "$period • ${item.lastUpdated ?: "10:30 AM"}",
                    fontSize = 12.sp,
                    color = TextBlack
                )
            }
            TrafficStatusBadge(status = item.currentStatus)
        }
    }
}
