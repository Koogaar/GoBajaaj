package com.example.somalitraffic.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
                "driver" -> onNavigateNext("home")
                "passenger" -> onNavigateNext("ride")
                else -> onNavigateNext("ride")
            }
        } else {
            onNavigateNext("login")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoBajaajNavy)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .scale(scale.value)
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_gobajaaj_logo),
                contentDescription = "GoBajaaj Logo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "GoBajaaj",
                fontSize = 38.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gaadiid • Waddooyin • Xiritaanka Waddooyinka • Taleefannada Lumay",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = GoBajaajYellow
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Ku soco si fudud",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "Garbahaaga, Kooxdaada, iyo Bulshadaada",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
