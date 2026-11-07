package com.example.somalitraffic.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.UserProfile
import com.example.somalitraffic.data.model.UserRole
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliSkylineBottom
import com.example.somalitraffic.ui.components.launchGoBajaajCall
import com.example.somalitraffic.ui.components.launchGoBajaajWhatsApp

@Composable
fun ForgotPasswordScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit,
    onResetSuccess: (UserRole) -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()

    var step by remember { mutableStateOf(1) } // 1: verify account, 2: set new password
    var phoneNumber by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var verifiedUser by remember { mutableStateOf<UserProfile?>(null) }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successDialogVisible by remember { mutableStateOf(false) }
    var finalRole by remember { mutableStateOf(UserRole.PASSENGER) }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        focusedBorderColor = Color(0xFFE50914),
        unfocusedBorderColor = Color(0xFFCBD5E1),
        focusedTextColor = Color(0xFF0F172A),
        unfocusedTextColor = Color(0xFF0F172A),
        cursorColor = Color(0xFFE50914)
    )

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
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Dib u noqo",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    Text(
                        text = "Dib u Soo Celinta Furaha",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brand Logo on Pure White Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.size(76.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "SHIRI",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = com.example.ui.theme.ShiriCrimson,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (step == 1) "Xaqiiji Akoonkaaga" else "Dooro Fure Sir Cusub",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (step == 1)
                        "Geli nambarkaaga iyo magacaaga si aan u xaqiijino akoonkaaga adigoon u baahnayn SMS."
                    else
                        "Geli furahaaga sirta ah ee cusub si aad dib ugu furato akoonkaaga.",
                    fontSize = 12.5.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Step Indicator Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepPill(
                        stepNumber = "1",
                        title = "Xaqiijinta",
                        isActive = step == 1,
                        isCompleted = step > 1
                    )
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(2.dp)
                            .background(if (step > 1) Color(0xFF16A34A) else Color(0xFFCBD5E1))
                    )
                    StepPill(
                        stepNumber = "2",
                        title = "Furaha Cusub",
                        isActive = step == 2,
                        isCompleted = false
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==================== STEP 1: VERIFY ACCOUNT ====================
                AnimatedVisibility(visible = step == 1, enter = fadeIn(), exit = fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Phone input
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it; errorMessage = null },
                            placeholder = { Text("61XXXXXXX", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
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
                                .testTag("input_reset_phone"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Full name input
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; errorMessage = null },
                            placeholder = { Text("Magacaaga buuxa (Full Name)", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF475569))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reset_name"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            singleLine = true
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Verify Button
                        Button(
                            onClick = {
                                val cleanPhone = phoneNumber.trim().replace(" ", "")
                                if (cleanPhone.isBlank() || cleanPhone.length < 7) {
                                    errorMessage = "Fadlan geli nambar taleefan oo sax ah."
                                    return@Button
                                }
                                if (fullName.trim().isBlank()) {
                                    errorMessage = "Fadlan geli magacaaga buuxa si akoonka loo xaqiijiyo."
                                    return@Button
                                }
                                viewModel.verifyAccountForReset(
                                    phone = cleanPhone,
                                    fullName = fullName.trim(),
                                    onSuccess = { user ->
                                        verifiedUser = user
                                        errorMessage = null
                                        step = 2
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_verify_account"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE50914)),
                            shape = RoundedCornerShape(26.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text("Xaqiiji Akoonka (Continue)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // ==================== STEP 2: SET NEW PASSWORD ====================
                AnimatedVisibility(visible = step == 2, enter = fadeIn(), exit = fadeOut()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Verified badge card
                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = verifiedUser?.fullName ?: fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Akoonka: +252 ${phoneNumber.trim()} (La xaqiijiyay)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF16A34A),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // New password
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; errorMessage = null },
                            placeholder = { Text("Furaha sirta cusub", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF475569)) },
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reset_new_password"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confirm password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            placeholder = { Text("Ku celi furaha sirta cusub", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF475569)) },
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color(0xFF94A3B8)
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_reset_confirm_password"),
                            shape = RoundedCornerShape(14.dp),
                            colors = fieldColors,
                            singleLine = true
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFFEF2F2),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFFB91C1C),
                                    fontSize = 12.5.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (newPassword.isBlank() || confirmPassword.isBlank()) {
                                    errorMessage = "Fadlan geli furaha sirta cusub."
                                    return@Button
                                }
                                if (newPassword != confirmPassword) {
                                    errorMessage = "Furayaasha sirta ah isma le'eka."
                                    return@Button
                                }
                                if (newPassword.length < 4) {
                                    errorMessage = "Furaha sirta waa inuu ugu yaraan 4 xaraf yahay."
                                    return@Button
                                }
                                viewModel.resetPasswordAndLogin(
                                    phone = phoneNumber,
                                    fullName = fullName,
                                    newPass = newPassword,
                                    onSuccess = { user ->
                                        finalRole = user.role
                                        successDialogVisible = true
                                    },
                                    onError = { err ->
                                        errorMessage = err
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_submit_new_password"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(26.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text("Beddel Furaha Sirta & Gal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        TextButton(
                            onClick = { step = 1; errorMessage = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dib ugu noqo xaqiijinta nambarka", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Help & Customer Support Card
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ma u baahan tahay caawinaad toos ah?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Haddii aad xogta akoonkaaga xasuusan weydo, la xiriir xafiiska SHIRI:",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { launchGoBajaajCall(context) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Wac Taageerada", fontSize = 11.5.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    val clean = phoneNumber.trim().filter { it.isDigit() }
                                    val msg = if (clean.isNotBlank())
                                        "Asc SHIRI, waxaan u baahanahay caawinaad ku saabsan furaha sirta akoonkayga: $clean"
                                    else
                                        "Asc SHIRI, waxaan u baahanahay caawinaad ku saabsan furaha sirta akoonkayga."
                                    launchGoBajaajWhatsApp(context, defaultMessage = msg)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            SomaliSkylineBottom(modifier = Modifier.fillMaxWidth())
        }

        // Success Dialog
        if (successDialogVisible) {
            AlertDialog(
                onDismissRequest = {
                    successDialogVisible = false
                    onResetSuccess(finalRole)
                },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Hambalyo!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "Furahaaga sirta ah si guul leh ayaa loo beddelay, akoonkaagana waa laguu furay!",
                        fontSize = 13.5.sp,
                        color = Color(0xFF475569),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            successDialogVisible = false
                            onResetSuccess(finalRole)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gal App-ka Hadda", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            )
        }
    }
}

@Composable
private fun StepPill(
    stepNumber: String,
    title: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    val bg = when {
        isCompleted -> Color(0xFF16A34A)
        isActive -> Color(0xFFE50914)
        else -> Color(0xFFE2E8F0)
    }
    val fg = when {
        isCompleted || isActive -> Color.White
        else -> Color(0xFF64748B)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) Color(0xFFFEE2E2) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = if (isActive) Color(0xFFE50914) else Color(0xFF64748B)
        )
    }
}
