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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.somalitraffic.ui.components.TrafficStatusBadge
import com.example.ui.theme.*

@Composable
fun AlternativeRouteScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val selectedIntersection by viewModel.selectedIntersection.collectAsState()
    val searchError by viewModel.searchError.collectAsState()
    var selectedRouteIndex by remember { mutableIntStateOf(1) }

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Jidka Kale",
                onBackClick = onBackClick
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedIntersection == null) {
                Text(
                    text = "Xogta isgoyska lama helin. Fadlan marka hore dooro isgoys.",
                    fontSize = 14.sp,
                    color = BrandRedPrimary
                )
                return@Scaffold
            }

            val target = selectedIntersection!!

            if (searchError != null) {
                Text(
                    text = searchError ?: "Xogta lama soo dejin karin.",
                    fontSize = 14.sp,
                    color = BrandRedPrimary
                )
            } else if (target.alternativeRoute.isNullOrBlank()) {
                 Text(
                    text = "Xog cusub oo saxmad ah lama hayo.",
                    fontSize = 14.sp,
                    color = TextBlack
                )
            } else {
                RouteOptionCard(
                    title = "Jidka Cad",
                    subtitle = target.alternativeRoute!!, // Use the DB data here
                    status = "Furan",
                    isSelected = selectedRouteIndex == 1,
                    iconTint = BrandRedPrimary,
                    onClick = { selectedRouteIndex = 1 }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onBackClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_dooro_jidka"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Dooro Jidka",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun RouteOptionCard(
    title: String,
    subtitle: String,
    status: String,
    isSelected: Boolean,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) BrandRedPrimary else BrandBorder,
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFFEF2F2) else BrandWhite
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val color = when (status.trim().lowercase()) {
                        "furan" -> TrafficOpenGreen
                        "normal" -> TrafficModerateYellow
                        "jaam" -> TrafficBusyOrange
                        "xeran" -> TrafficHeavyRed
                        else -> Color.Gray
                    }
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = status, fontSize = 12.sp, color = color, fontWeight = FontWeight.Bold)
                }
            }
            
            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                tint = if (isSelected) BrandRedPrimary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
