package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.ui.theme.*

@Composable
fun OnboardingScreen(
    viewModel: SomaliTrafficViewModel,
    onFinishOnboarding: () -> Unit
) {
    fun complete() {
        viewModel.repository.prefs.setOnboardingCompleted(true)
        onFinishOnboarding()
    }

    Scaffold(
        containerColor = SomaliBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { complete() },
                    modifier = Modifier.testTag("btn_skip_onboarding")
                ) {
                    Text(
                        text = "Ka bood",
                        color = TextBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Central Illustration & Feature List
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(BrandBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = BrandRedPrimary,
                        modifier = Modifier.size(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Ku Soo Dhawoow\nGoBajaaj",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = TextBlack,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hal nidaam oo isku xira saxmadda tooska ah iyo ilaalinta taleefankaaga gacanta.",
                    fontSize = 14.sp,
                    color = TextBlack,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Feature items
                FeatureItem(
                    icon = Icons.Default.Traffic,
                    title = "La soco saxmadda waddooyinka",
                    description = "Arag xaaladda isgoysyada Muqdisho si toos ah (Furan, Mashquul, Saxmad)."
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureItem(
                    icon = Icons.Default.NotificationsActive,
                    title = "Hel digniino degdeg ah",
                    description = "Ogow shilalka, waddooyinka xiran, iyo dariiqyada kale ee furan."
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Ilaali taleefankaaga",
                    description = "Diiwaangeli taleefankaaga si aad u hesho ammaan buuxa iyo raadraac toos ah."
                )
                Spacer(modifier = Modifier.height(14.dp))
                FeatureItem(
                    icon = Icons.Default.GpsFixed,
                    title = "Raac taleefanka lumay",
                    description = "Haddii taleefanku kaa lumo, si degdeg ah u hel goobtii ugu dambeysay."
                )
            }

            // Bottom CTA
            Button(
                onClick = { complete() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_start_onboarding"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Bilow",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFFFEE2E2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandRedPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextBlack
            )
        }
    }
}
