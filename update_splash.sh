#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/SplashScreen.kt
package com.example.somalitraffic.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Traffic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigateNext: (String) -> Unit
) {
    val scale = remember { Animatable(0.7f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300)
        )
        delay(1000)

        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            when (currentUser.role.code) {
                "reporter" -> onNavigateNext("reporter_dashboard")
                "super_admin", "user_manager", "subscription_manager", "traffic_manager", "reporter_manager", "support_agent" -> onNavigateNext("admin_dashboard")
                else -> onNavigateNext("home")
            }
        } else {
            onNavigateNext("login")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandRedPrimary)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(scale.value)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    tint = BrandRedPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "SOMALI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = "TRAFFIC",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Waddooyinka Wanaagsan",
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = "Nolosha Kaaga Badbaadiyaan",
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}
INNER_EOF
