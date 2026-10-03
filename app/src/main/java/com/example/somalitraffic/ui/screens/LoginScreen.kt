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
import com.example.somalitraffic.ui.components.SmsOtpSection
import com.example.somalitraffic.ui.components.SomaliSkylineBottom

@Composable
fun LoginScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (UserRole) -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Forgot Password dialog state
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPhone by remember { mutableStateOf("") }
    var forgotOtpCode by remember { mutableStateOf("") }
    var forgotStep by remember { mutableStateOf(1) } // 1: enter phone, 2: enter otp
    var forgotError by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()
    val otpCooldown by viewModel.otpCooldown.collectAsState()
    val pendingPhone by viewModel.pendingPhone.collectAsState()
    val otpNotice by viewModel.otpNotice.collectAsState()

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
                // App Logo Badge
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_gobajaaj_logo),
                    contentDescription = "GoBajaaj Logo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(18.dp))
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Brand Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Go",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF44349A),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Bajaaj",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFE50914),
                        letterSpacing = 0.5.sp
                    )
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

                // Phone Number Input
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; errorMessage = null },
                    placeholder = { Text("061XXXXXXX ama 61XXXXXXX", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                    leadingIcon = {
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
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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
                            showForgotPasswordDialog = true
                            forgotPhone = phoneNumber
                            forgotStep = 1
                            forgotError = null
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Action: Login Button
                Button(
                    onClick = {
                        val cleanPhone = phoneNumber.trim().replace(" ", "")
                        if (cleanPhone.isBlank() || password.isBlank()) {
                            errorMessage = "Fadlan geli nambarka iyo furaha sirta."
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

        // Forgot Password Dialog (SMS OTP Verification)
        if (showForgotPasswordDialog) {
            AlertDialog(
                onDismissRequest = {
                    showForgotPasswordDialog = false
                    viewModel.resetOtpState()
                },
                title = {
                    Text(
                        text = "Dib u Soo Celinta Furaha Sirta",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (forgotStep == 1) {
                            Text(
                                text = "Geli nambarkaaga taleefanka si aan kuugu soo dirno koodhka OTP.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedTextField(
                                value = forgotPhone,
                                onValueChange = { forgotPhone = it; forgotError = null },
                                placeholder = { Text("061XXXXXXX ama 61XXXXXXX", fontSize = 13.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            SmsOtpSection(
                                phoneE164 = pendingPhone.ifBlank { viewModel.repository.prefs.toE164(forgotPhone) },
                                otpCode = forgotOtpCode,
                                onOtpChange = { forgotOtpCode = it; forgotError = null },
                                cooldownSeconds = otpCooldown,
                                isLoading = isLoading,
                                errorMessage = forgotError,
                                noticeMessage = otpNotice,
                                onVerifyClick = {
                                    viewModel.verifyOtpAndLogin(forgotPhone, forgotOtpCode) { user ->
                                        showForgotPasswordDialog = false
                                        viewModel.resetOtpState()
                                        onLoginSuccess(user.role)
                                    }
                                },
                                onResendClick = { viewModel.resendOtp(forgotPhone) },
                                onChangeNumberClick = { forgotStep = 1 },
                                actionButtonText = "Xaqiiji Koodhka & Gal"
                            )
                        }

                        if (forgotError != null && forgotStep == 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = forgotError!!,
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    if (forgotStep == 1) {
                        Button(
                            onClick = {
                                val clean = forgotPhone.trim().replace(" ", "")
                                if (clean.isBlank() || clean.length < 7) {
                                    forgotError = "Fadlan geli nambar taleefan oo sax ah"
                                    return@Button
                                }
                                viewModel.sendOtpForLogin(clean) {
                                    forgotStep = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                            } else {
                                Text("Dir Koodhka SMS (OTP)", color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showForgotPasswordDialog = false
                        viewModel.resetOtpState()
                    }) {
                        Text("Xir", color = Color(0xFF64748B))
                    }
                }
            )
        }
    }
}
