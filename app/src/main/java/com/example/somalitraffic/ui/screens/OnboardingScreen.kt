package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.somalitraffic.ui.SomaliTrafficViewModel

@Composable
fun OnboardingScreen(
    viewModel: SomaliTrafficViewModel,
    onFinishOnboarding: () -> Unit,
    onNavigateToLogin: () -> Unit = {}
) {
    fun startAsGuest() {
        viewModel.repository.prefs.setOnboardingCompleted(true)
        viewModel.continueAsGuestCustomer()
        onFinishOnboarding()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE50914),
                        Color(0xFFDC2626),
                        Color(0xFFB91C1C)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BRAND SECTION: SHIRI Logo + Title + Tagline
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                // White circular logo badge with pin & letter S
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(76.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "S",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFDC2626)
                            )
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(x = (-6).dp, y = (-2).dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SHIRI",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Dalbo • Hel • Ku Raaxayso",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.92f),
                    letterSpacing = 0.5.sp
                )
            }

            // CENTER HERO ILLUSTRATION: Delicious Food (Burger, Fries, Drink)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=800&auto=format&fit=crop&q=80",
                    contentDescription = "SHIRI Cunto & Gaarsiin",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                )
            }

            // BOTTOM ACTION BUTTONS: "Bilow ->" and "Gal Account"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                // White Pill Button: Bilow ->
                Button(
                    onClick = { startAsGuest() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Bilow",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Text Link: Gal Account
                Text(
                    text = "Gal Account",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigateToLogin() }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}
