package com.example.somalitraffic.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import kotlinx.coroutines.*

@Composable
fun SplashScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigateNext: (String) -> Unit
) {
    val progress = remember { Animatable(0.12f) }

    LaunchedEffect(Unit) {
        // 1) Validate the restored session FIRST. Android Auto Backup can restore
        //    an old "gobajaaj_prefs" onto a fresh install; trusting it would send
        //    a logged-out user straight to the Driver Pass screen. If the stored
        //    token is not real/refreshable it is cleared here.
        val validSession = try {
            viewModel.validateStoredSession()
        } catch (_: Exception) {
            viewModel.currentUser.value != null
        }

        val syncJob = if (validSession) {
            launch(Dispatchers.IO) {
                try {
                    viewModel.syncCurrentSubscription()
                } catch (_: Exception) {}
            }
        } else null

        progress.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 1800, easing = FastOutSlowInEasing)
        )

        if (syncJob != null) {
            try {
                withTimeoutOrNull(800) {
                    syncJob.join()
                }
            } catch (_: Exception) {}
        }
        delay(150)

        val currentUser = viewModel.currentUser.value
        if (currentUser != null) {
            when (currentUser.role.code) {
                "reporter" -> onNavigateNext("reporter_dashboard")
                "super_admin", "user_manager", "subscription_manager", "traffic_manager", "reporter_manager", "support_agent" -> onNavigateNext("admin_dashboard")
                "driver" -> {
                    if (viewModel.isSubscriptionActive()) {
                        onNavigateNext("home")
                    } else {
                        onNavigateNext("driver_pass")
                    }
                }
                "merchant" -> onNavigateNext("merchant_app")
                "courier" -> onNavigateNext("delivery")
                "passenger" -> onNavigateNext("home")
                else -> onNavigateNext("home")
            }
        } else {
            // Marka app-ka la furo toos u gee Role Selection (Dooro Doorkaaga: Macmiil, Ganacsade, Darawal, Delivery)
            onNavigateNext("role_selection")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF031433))
    ) {
        // High-resolution full splash graphic matching SHIRI design
        Image(
            painter = painterResource(id = com.example.R.drawable.ic_gobajaaj_splash_bg),
            contentDescription = "SHIRI Splash",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Live animated glowing gradient progress bar overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(top = 115.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(148.dp)
                    .height(6.5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF082252).copy(alpha = 0.9f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00E676), // Bright Green
                                    Color(0xFF00B0FF)  // Bright Cyan
                                )
                            )
                        )
                )
            }
        }
    }
}

