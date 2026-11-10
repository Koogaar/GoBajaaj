package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.UserRole
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliSkylineBottom

@Composable
fun LoginScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    onLoginSuccess: (UserRole) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { msg ->
            errorMessage = msg
        }
    }

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
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 40.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Logo Badge on Pure White Card
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(84.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Title & Subtitle
                Text(
                    text = "Gal Akoonkaaga",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Geli nambarkaaga iyo furaha sirta",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE50914),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    cursorColor = Color(0xFFE50914)
                )

                // Phone Number or Email Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; errorMessage = null },
                    placeholder = {
                        Text(
                            text = if (phoneNumber.contains("@")) "wariye@gmail.com" else "61XXXXXXX ama Gmail",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    leadingIcon = {
                        if (phoneNumber.contains("@")) {
                            Icon(
                                Icons.Outlined.Email,
                                contentDescription = null,
                                tint = Color(0xFFE50914),
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                            ) {
                                Text(
                                    text = "🇸🇴 +252",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(Color(0xFFCBD5E1))
                                )
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_phone"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Input
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; errorMessage = null },
                    placeholder = { Text("Furaha sirta (Password)", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF475569)) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_login_password"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFE50914),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Forgot Password Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Illoobay Furaha Sirta? (Forgot Password)",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.clickable {
                            onNavigateToForgotPassword()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Login Button
                Button(
                    onClick = {
                        val cleanPhone = phoneNumber.trim().replace(" ", "")
                        if (cleanPhone.isBlank() || password.isBlank()) {
                            errorMessage = "Fadlan geli nambarka/Gmail-ka iyo furaha sirta."
                            return@Button
                        }
                        viewModel.login(cleanPhone, password) { user ->
                            onLoginSuccess(user.role)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_login_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                    shape = RoundedCornerShape(26.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = "Gal (Login)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Container
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .clickable { onNavigateToSignUp() }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Weli ma haysatid akoon? ",
                                fontSize = 13.5.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Is-diiwaangeli",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)
                        Spacer(modifier = Modifier.height(12.dp))

                        com.example.somalitraffic.ui.components.GoBajaajContactMiniRow(
                            label = "Caawinaad:"
                        )
                    }
                }
            }

            // Bottom Skyline Vector Illustration
            SomaliSkylineBottom(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
