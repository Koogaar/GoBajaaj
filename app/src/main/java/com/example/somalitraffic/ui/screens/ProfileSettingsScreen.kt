package com.example.somalitraffic.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.updateUserAvatar(uri.toString())
        }
    }

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Profile"
            )
        },
        bottomBar = {
            SomaliBottomNavigation(
                currentRoute = "profile_settings",
                onNavigate = onNavigate,
                isDriver = currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER
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
            // Profile Avatar (Clickable with Camera icon for both Drivers and Passengers)
            Box(
                modifier = Modifier.size(92.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(BrandLightGray)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (!currentUser?.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentUser!!.avatarUrl,
                            contentDescription = "Profile Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = BrandBlack,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Camera Badge Button
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(BrandRedPrimary)
                        .clickable { photoPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Geli Sawir",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Taabo si aad sawir u geliso",
                fontSize = 11.5.sp,
                color = GoBajaajNavy,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { photoPickerLauncher.launch("image/*") }
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
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
                color = Color.Gray
            )

            val isDriver = currentUser?.role == com.example.somalitraffic.data.model.UserRole.DRIVER
            if (isDriver) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_bajaaj_icon),
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Bajaaj: ${currentUser?.vehicleModel ?: "Bajaj RE"}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Taargo: ${currentUser?.plateNumber ?: "TR-4820"}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "Diiwaangashan",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Menu Items
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileMenuItem(icon = Icons.Outlined.WorkOutline, title = "Taariikhda Safarada") { onNavigate("traffic_history") }
                HorizontalDivider(color = BrandBorder)
                if (isDriver) {
                    ProfileMenuItem(icon = Icons.Outlined.CreditCard, title = "Adeegga Maalinlaha ah ($0.25)") { onNavigate("driver_pass") }
                    HorizontalDivider(color = BrandBorder)
                }
                ProfileMenuItem(icon = Icons.Default.FavoriteBorder, title = "Taageero (Support)") { onNavigate("support_center") }
                HorizontalDivider(color = BrandBorder)
                ProfileMenuItem(icon = Icons.Outlined.HelpOutline, title = "Faahfaahin & Caawinaad") { onNavigate("support_center") }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contact Phone & WhatsApp Card
            com.example.somalitraffic.ui.components.GoBajaajContactHotlineCard(
                title = "Xafiiska Taageerada SHIRI",
                subtitle = "Wicitaan & WhatsApp: 061 809 2060"
            )

            Spacer(modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_logout"),
                colors = ButtonDefaults.buttonColors(containerColor = GoBajaajNavy),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Ka Bax",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.shiri_logo),
                    contentDescription = "SHIRI Logo",
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SHIRI Somalia • v4.0.0",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
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
