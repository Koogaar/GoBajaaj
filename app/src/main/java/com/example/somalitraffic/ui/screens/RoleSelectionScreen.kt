package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.components.SomaliSkylineBottom

@Composable
fun RoleSelectionScreen(
    onBackClick: () -> Unit,
    onRoleSelected: (String) -> Unit
) {
    var selectedRole by remember { mutableStateOf<String?>("passenger") }
    var showErrorMessage by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F5F9)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Back Arrow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White, CircleShape)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tilmaan dib u noqod",
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // App Logo with Pure White Background (Poster Style)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.shiri_logo),
                            contentDescription = "SHIRI Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Dooro Adeegga Aad Rabto",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "SHIRI • Everything, One Journey.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Dalbo Bajaaj (Rakaab / Passenger)
                RoleCard(
                    title = "DALBO BAJAAJ",
                    subtitle = "Rakaab • Dalbo Bajaaj & Safar degdeg ah • Bilaash ($0)",
                    tagText = "SHIRI RIDE",
                    tagColor = Color(0xFFE50914),
                    tagBg = Color(0xFFFEF2F2),
                    iconEmoji = "🛺",
                    isSelected = selectedRole == "passenger",
                    onClick = {
                        selectedRole = "passenger"
                        showErrorMessage = false
                    }
                )

                Spacer(modifier = Modifier.height(11.dp))

                // 2. Darawal Bajaaj (Driver Pass $0.25 - 18 isgoys removed)
                RoleCard(
                    title = "DARAWAL BAJAAJ",
                    subtitle = "Qaadista Rakaabka Bajaajta • Driver Pass $0.25 Maalintii",
                    tagText = "BAJAAJLE",
                    tagColor = Color(0xFFB45309),
                    tagBg = Color(0xFFFEF3C7),
                    iconEmoji = "🛺",
                    isSelected = selectedRole == "driver",
                    onClick = {
                        selectedRole = "driver"
                        showErrorMessage = false
                    }
                )

                Spacer(modifier = Modifier.height(11.dp))

                // 3. SHIRI Delivery (Gaarsiiye Mooto / Courier)
                RoleCard(
                    title = "SHIRI DELIVERY",
                    subtitle = "Dirso Alaab • Baakado, Cunto & Gaarsiin Mooto • $5/Bishii",
                    tagText = "MOOTO COURIER",
                    tagColor = Color(0xFF0284C7),
                    tagBg = Color(0xFFF0F9FF),
                    iconEmoji = "🏍️",
                    isSelected = selectedRole == "courier",
                    onClick = {
                        selectedRole = "courier"
                        showErrorMessage = false
                    }
                )

                Spacer(modifier = Modifier.height(11.dp))

                // 4. SHIRI Eats & Market (Ganacsade / Maqaayad)
                RoleCard(
                    title = "SHIRI EATS & MARKET",
                    subtitle = "Dalbo Cunto, Dukaamada & Ganacsiga • Qoro Till-kaaga • $5/Bishii",
                    tagText = "GANACSADE",
                    tagColor = Color(0xFF16A34A),
                    tagBg = Color(0xFFF0FDF4),
                    iconEmoji = "🍔",
                    isSelected = selectedRole == "merchant",
                    onClick = {
                        selectedRole = "merchant"
                        showErrorMessage = false
                    }
                )

                if (showErrorMessage) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Fadlan dooro mid ka mid ah adeegyada",
                        color = Color(0xFFDC2626),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (selectedRole != null) {
                            onRoleSelected(selectedRole!!)
                        } else {
                            showErrorMessage = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(26.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Sii wad →",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Skyline for Somali Mogadishu Touch
                SomaliSkylineBottom(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                )
            }
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    tagText: String,
    tagColor: Color,
    tagBg: Color,
    iconEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFE50914) else Color(0xFFE2E8F0)
    val backgroundColor = if (isSelected) Color(0xFFFFF5F5) else Color.White

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        color = backgroundColor,
        shadowElevation = if (isSelected) 3.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Container
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = tagBg,
                modifier = Modifier.size(46.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = iconEmoji,
                        fontSize = 24.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Surface(
                        color = tagBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = tagColor,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Check circle
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Doortay",
                    tint = Color(0xFFE50914),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                )
            }
        }
    }
}
