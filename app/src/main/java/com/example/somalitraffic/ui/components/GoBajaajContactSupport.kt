package com.example.somalitraffic.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.HeadsetMic
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandBorderDark
import com.example.ui.theme.BrandRedPrimary
import java.net.URLEncoder

const val GOBAJAAJ_CONTACT_PHONE = "0618092060"
const val GOBAJAAJ_WHATSAPP_PHONE = "252618092060"

fun launchGoBajaajCall(context: Context, phone: String = GOBAJAAJ_CONTACT_PHONE) {
    try {
        val cleanNumber = phone.replace(" ", "").trim()
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("GoBajaajContact", "Error launching dialer: ${e.message}")
    }
}

fun launchGoBajaajWhatsApp(
    context: Context,
    phone: String = GOBAJAAJ_WHATSAPP_PHONE,
    defaultMessage: String = "Asc SHIRI, waxaan u baahanahay caawinaad."
) {
    try {
        val cleanPhone = phone.filter { it.isDigit() }
        val encoded = URLEncoder.encode(defaultMessage, "UTF-8")
        val uri = Uri.parse("https://wa.me/$cleanPhone?text=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("GoBajaajContact", "Error launching WhatsApp: ${e.message}")
    }
}

@Composable
fun GoBajaajContactHotlineCard(
    modifier: Modifier = Modifier,
    title: String = "Khadka Tooska ah ee Caawinta (24/7)",
    subtitle: String = "Wicitaan toos ah & WhatsApp Heegan ah",
    containerColor: Color = Color(0xFF111827)
) {
    val context = LocalContext.current
    val whatsappGreen = Color(0xFF25D366)

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BrandBorderDark),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(BrandRedPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = null,
                        tint = BrandRedPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "Tel: 061 809 2060",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF60A5FA),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Call & WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Call Button
                Button(
                    onClick = { launchGoBajaajCall(context, GOBAJAAJ_CONTACT_PHONE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Wac",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Wac (Call)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                // WhatsApp Button
                Button(
                    onClick = { launchGoBajaajWhatsApp(context, GOBAJAAJ_WHATSAPP_PHONE) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun GoBajaajContactMiniRow(
    modifier: Modifier = Modifier,
    label: String = "Caawinaad degdeg ah:"
) {
    val context = LocalContext.current
    val whatsappGreen = Color(0xFF25D366)

    Surface(
        color = Color(0xFFF8FAFC),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = modifier
            .wrapContentWidth()
            .padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.HeadsetMic,
                contentDescription = null,
                tint = BrandRedPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color(0xFF475569),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "061 809 2060",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Call Button Pill
            Surface(
                onClick = { launchGoBajaajCall(context, GOBAJAAJ_CONTACT_PHONE) },
                shape = CircleShape,
                color = BrandRedPrimary,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Wac",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // WhatsApp Button Pill
            Surface(
                onClick = { launchGoBajaajWhatsApp(context, GOBAJAAJ_WHATSAPP_PHONE) },
                shape = CircleShape,
                color = whatsappGreen,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}
