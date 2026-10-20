package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.components.SomaliSkylineBottom

@Composable
fun RoleSelectionScreen(
    onBackClick: () -> Unit,
    onRoleSelected: (Boolean) -> Unit // true for driver, false for passenger
) {
    var selectedRole by remember { mutableStateOf<Boolean?>(true) }
    var showErrorMessage by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF0F7FF),
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Navigation Back Arrow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tilmaan dib u noqod",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // App Logo Badge
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_gobajaaj_logo),
                    contentDescription = "GoBajaaj Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Dooro nooca akoonka",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Fadlan dooro nooca adeeg ee aad u baahan tahay",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Darawal / Isticmaale Card
                RoleCard(
                    title = "DARAWAL / ISTICMAALE",
                    subtitle = "Xogta Saxmadda Waddooyinka Muqdisho • Isgoysyada & Khariiradda Tooska ah",
                    iconEmoji = "🚖",
                    isSelected = true,
                    onClick = {
                        selectedRole = true
                        showErrorMessage = false
                    }
                )

                if (showErrorMessage) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Fadlan dooro nooca akoonka",
                        color = Color(0xFFE50914),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

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
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = "Sii wad",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Bottom Skyline
            SomaliSkylineBottom(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun RoleCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFFE50914) else Color(0xFFE2E8F0)
    val backgroundColor = if (isSelected) Color(0xFFFEF2F2) else Color.White

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        color = backgroundColor,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = iconEmoji,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
