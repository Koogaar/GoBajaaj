#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/DriverPassScreen.kt
package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.ui.theme.*

@Composable
fun DriverPassScreen(
    viewModel: SomaliTrafficViewModel,
    onPaymentSuccess: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    var transactionId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "", // Top bar in image has no title just back arrow
                onBackClick = onBackClick
            )
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Plan Card
            Surface(
                color = BrandBlack,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, BrandRedPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(BrandRedPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Driver Pass",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "$1 / Bishii",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    FeatureItem("Hel digniino saxmad ah")
                    FeatureItem("Raac safarkaaga")
                    FeatureItem("Adeegyada mudnaanta")
                    FeatureItem("Taariikhda traffic-ga")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Dooro Number-ka Lacagta Lagu Dirayo",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                PaymentNumberRow("618092060") { clipboardManager.setText(AnnotatedString("618092060")) }
                Spacer(modifier = Modifier.height(8.dp))
                PaymentNumberRow("628512131") { clipboardManager.setText(AnnotatedString("628512131")) }

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Lacagta $1 ku dir mid ka mid ah labada number.",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Transaction ID / Reference",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = transactionId,
                    onValueChange = { transactionId = it; errorMessage = null },
                    placeholder = { Text("Geli Transaction ID / Reference") },
                    modifier = Modifier.fillMaxWidth().testTag("input_transaction_id"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandRedPrimary,
                        unfocusedBorderColor = BrandBorder
                    ),
                    singleLine = true
                )
                
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = BrandRedPrimary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (successMessage != null) {
                    Text(
                        text = successMessage!!,
                        color = TrafficOpenGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (transactionId.isBlank()) {
                            errorMessage = "Fadlan geli Transaction ID-ga"
                            return@Button
                        }
                        viewModel.submitManualPayment(
                            method = "EVC/ZAAD/SAHAL",
                            account = "Manual",
                            amount = 1.0,
                            transactionId = transactionId,
                            onSuccess = {
                                successMessage = "Lacagtaada waa la diray. Waa la xaqiijin doonaa dhowaan."
                                transactionId = ""
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("btn_xaqiiji_lacagta"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                    shape = RoundedCornerShape(28.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Xaqiiji Lacagta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(Icons.Default.Check, contentDescription = null, tint = BrandRedPrimary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color.White, fontSize = 14.sp)
    }
}

@Composable
private fun PaymentNumberRow(number: String, onCopyClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BrandBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = BrandBlack, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = number, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextBlack)
        }
        Text(
            text = "Nuqul",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = BrandRedPrimary,
            modifier = Modifier.clickable { onCopyClick() }.padding(4.dp)
        )
    }
}
INNER_EOF
