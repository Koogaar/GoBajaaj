package com.example.somalitraffic.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import kotlinx.coroutines.*

@Composable
fun SplashScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigateNext: (String) -> Unit
) {
    val progress = remember { Animatable(0.0f) }

    // Pulse animation for logo
    val pulse = rememberInfiniteTransition(label = "pulse")
    val logoScale by pulse.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

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
            // Marka app-ka la furo toos u gee Onboarding Welcome Screen (Screen 1)
            onNavigateNext("onboarding")
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // SHIRI RED SPLASH SCREEN — Logo casaan ah oo cad ku ah (Screen 7 Mockup)
    // Gadaalka: Gradiyaan cas ah oo SHIRI-ka u gaar ah
    // ══════════════════════════════════════════════════════════════════
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE50914), // SHIRI Red top
                        Color(0xFFDC2626), // Mid red
                        Color(0xFFB91C1C)  // Darker red bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ══════════════════════════════
            // SHIRI LOGO — Cad oo wareegsan
            // ══════════════════════════════
            Surface(
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier
                    .size(110.dp)
                    .scale(logoScale)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // S + pin icon (matches master logo design)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "S",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFDC2626)
                        )
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = (-8).dp, y = (-2).dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Name
            Text(
                text = "SHIRI",
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 4.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Dalbo • Hel • Ku Raaxayso",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // ══════════════════════════════
            // Loading Bar — cad oo qurux badan
            // ══════════════════════════════
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.value)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White)
                )
            }
        }

        // Bottom tagline
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "Safarka Bajaajta & Gaarsiinta Muqdisho",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.70f)
            )
        }
    }
}
