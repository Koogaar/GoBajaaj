#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/ProfileSettingsScreen.kt
package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkOutline
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
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliBottomNavigation
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.ui.theme.*

@Composable
fun ProfileSettingsScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Profile"
            )
        },
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "profile_settings",
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
                .padding(vertical = 32.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Card (Centered)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BrandLightGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = BrandBlack, modifier = Modifier.size(48.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = currentUser?.fullName ?: "Darawal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            
            Text(
                text = currentUser?.phoneNumber ?: "",
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Text(
                text = currentUser?.role?.titleSomali ?: "Darawal",
                fontSize = 14.sp,
                color = Color.Gray // Using Gray as requested to avoid blue/purple
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Menu Items
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileMenuItem(icon = Icons.Outlined.WorkOutline, title = "Taariikhda Safarada") { onNavigate("traffic_history") }
                HorizontalDivider(color = BrandBorder)
                ProfileMenuItem(icon = Icons.Outlined.CreditCard, title = "Subscription & Payments") { onNavigate("driver_pass") }
                HorizontalDivider(color = BrandBorder)
                ProfileMenuItem(icon = Icons.Outlined.Settings, title = "Settings") { /* TODO */ }
                HorizontalDivider(color = BrandBorder)
                ProfileMenuItem(icon = Icons.Default.FavoriteBorder, title = "Taageero") { onNavigate("support_center") }
                HorizontalDivider(color = BrandBorder)
                ProfileMenuItem(icon = Icons.Outlined.HelpOutline, title = "Ku Bixinta") { /* TODO */ }
            }

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_logout"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "Ka Bax",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = TextBlack, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextBlack)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
    }
}
INNER_EOF
