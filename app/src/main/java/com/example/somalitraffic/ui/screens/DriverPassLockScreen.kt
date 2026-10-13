package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun DriverPassLockScreen(
    viewModel: SomaliTrafficViewModel,
    onPayNow: () -> Unit,
    onCheckAgain: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null
) {
    // Continuously check in the background: when Admin approves, unlock automatically
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(4_000)
            viewModel.syncCurrentSubscription()
            if (viewModel.isSubscriptionActive()) {
                onCheckAgain?.invoke()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GoBajaajNavy)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_gobajaaj_logo),
                contentDescription = "GoBajaaj",
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lock Icon Badge
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GoBajaajYellow,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Headline 1 (White)
            Text(
                text = "Lacagta Waa ay ka Dhamaatay",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Headline 2 (Yellow)
            Text(
                text = "Fadlan ku Shubo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = GoBajaajYellow,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Big Rounded Pill Payment Button (Matching Image 2)
            Button(
                onClick = onPayNow,
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GoBajaajYellow),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_wallet_add),
                        contentDescription = "Wallet",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(34.dp)
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    // Vertical Divider inside button
                    Box(
                        modifier = Modifier
                            .width(1.5.dp)
                            .height(38.dp)
                            .background(Color(0xFF0F172A).copy(alpha = 0.65f))
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Ku shubo hadda",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            lineHeight = 20.sp
                        )
                        Text(
                            text = "0.25 Maalinle",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            if (onLogout != null) {
                Spacer(modifier = Modifier.height(28.dp))
                TextButton(
                    onClick = onLogout
                ) {
                    Text(
                        text = "Ka bax (Log out)",
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}