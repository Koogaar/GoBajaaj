package com.example.somalitraffic.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
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
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliSkylineBottom

@Composable
fun SignUpScreen(
    viewModel: SomaliTrafficViewModel,
    initialIsDriver: Boolean = true,
    initialRole: String = if (initialIsDriver) "driver" else "passenger",
    onBackClick: () -> Unit,
    onSignUpSuccess: () -> Unit
) {
    val isCourier = initialRole == "courier"
    val isMerchant = initialRole == "merchant"
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isDriverMode by remember { mutableStateOf(initialRole == "driver" || (!isCourier && !isMerchant && initialIsDriver)) }
    var vehicleType by remember { mutableStateOf(if (isCourier) "Mooto (Courier)" else "Bajaaj") }
    
    // Courier Specific Fields
    var licenseNumber by remember { mutableStateOf("") }
    var guarantorName by remember { mutableStateOf("") }
    var guarantorPhone by remember { mutableStateOf("") }
    var cidLetterNumber by remember { mutableStateOf("") }
    
    // Merchant Specific Fields
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("Maqaayad / Cunto") }
    var merchantPaymentNumber by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("Hodan") }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isLoading by viewModel.isLoading.collectAsState()

    BackHandler {
        onBackClick()
    }

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
                    .padding(top = 16.dp, bottom = 16.dp),
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
                            contentDescription = "Dib u noqo",
                            tint = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // App Logo Badge on Pure White Card
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

                Spacer(modifier = Modifier.height(10.dp))

                // Brand Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SHIRI",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDC2626),
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title & Subtitle
                Text(
                    text = when {
                        isMerchant -> "Diiwaangelinta Ganacsiga / Maqaayadda"
                        isCourier -> "Diiwaangelinta Gaarsiiye Mooto"
                        isDriverMode -> "Diiwaangelinta Darawalka"
                        else -> "Diiwaangelinta Dalbo Bajaaj (Rakaab)"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isMerchant -> "Qoro magaca ganacsiga, goobta iyo nambarkaaga Till-ka"
                        isCourier -> "Geli xogtaada, shatiga wadista, damiinka iyo warqadda CID-da"
                        else -> "Geli magacaaga, nambarkaaga iyo furaha sirta"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Official Pricing Notice Pill
                Surface(
                    color = when {
                        isMerchant || isCourier -> Color(0xFFFEF2F2)
                        isDriverMode -> Color(0xFFFEF3C7)
                        else -> Color(0xFFECFDF5)
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, when {
                        isMerchant || isCourier -> Color(0xFFFCA5A5)
                        isDriverMode -> Color(0xFFFDE68A)
                        else -> Color(0xFFA7F3D0)
                    }),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when {
                                isMerchant -> "🏪 Rukhumadda Ganacsiga: $5.00 / Bishii • Xaqiijin Admin"
                                isCourier -> "🏍️ Rukhumadda Gaarsiiyaha: $5.00 / Bishii • Xaqiijin Admin"
                                isDriverMode -> "🛺 Driver Pass-ka Bajaajta: $0.25 / Maalintii (24 Saac)"
                                else -> "👤 Akoonka Rakaabka: Bilaash ($0.00)"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isMerchant || isCourier -> Color(0xFF991B1B)
                                isDriverMode -> Color(0xFF92400E)
                                else -> Color(0xFF065F46)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE50914),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    cursorColor = Color(0xFFE50914)
                )

                // Vehicle Type selector for Drivers
                if (isDriverMode) {
                    Text(
                        text = "Nooca Gaadiidka:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isBajaaj = vehicleType == "Bajaaj"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isBajaaj) Color(0xFFE50914) else Color.Transparent)
                                .clickable { vehicleType = "Bajaaj" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🚖 Bajaaj",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBajaaj) Color.White else Color(0xFF475569)
                            )
                        }

                        val isGaari = vehicleType == "Gaari"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isGaari) Color(0xFFE50914) else Color.Transparent)
                                .clickable { vehicleType = "Gaari" }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🚗 Gaari (Car)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGaari) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (isMerchant) {
                    // Magaca Ganacsiga
                    OutlinedTextField(
                        value = businessName,
                        onValueChange = { businessName = it; errorMessage = null },
                        placeholder = { Text("Magaca Maqaayadda ama Dukaanka", fontSize = 13.5.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("🏪", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Nooca Ganacsiga (Business Category)
                    Text(
                        text = "Nooca Ganacsiga:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF334155),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Maqaayad", "Fast Food", "Dukaan", "Farmashiye").forEach { type ->
                            val isSel = businessType.contains(type)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) Color(0xFFDC2626) else Color.Transparent)
                                    .clickable { businessType = type }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Degmada
                    OutlinedTextField(
                        value = district,
                        onValueChange = { district = it; errorMessage = null },
                        placeholder = { Text("Degmada Ganacsiga (Hodan, Waaberi...)", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("📍", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // 1. Full Name Input
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it; errorMessage = null },
                    placeholder = { 
                        Text(
                            if (isMerchant) "Magaca Mulkiilaha ama Maamulaha" else "Magaca buuxa (Full Name)", 
                            fontSize = 14.sp, 
                            color = Color(0xFF94A3B8)
                        ) 
                    },
                    leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF475569)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_signup_name"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Phone Number with Somalia +252 Indicator
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
                        .testTag("input_signup_phone"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                if (isMerchant) {
                    Spacer(modifier = Modifier.height(10.dp))

                    // Nambarka Lacag-qabashada Ganacsiga (Customer Payment Till / Number)
                    OutlinedTextField(
                        value = merchantPaymentNumber,
                        onValueChange = { merchantPaymentNumber = it; errorMessage = null },
                        placeholder = { Text("Nambarka Till-ka / EVC ee Macmiilka", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("💳", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "ℹ️ Macaamiishu lacagta cuntada/alaabta toos ayay nambarkan ugu soo dirayaan.",
                        fontSize = 11.sp,
                        color = Color(0xFF0284C7),
                        lineHeight = 15.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                    )
                }

                if (isCourier) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Shatiga Wadista (Driving License)
                    OutlinedTextField(
                        value = licenseNumber,
                        onValueChange = { licenseNumber = it; errorMessage = null },
                        placeholder = { Text("Nambarka Shatiga Wadista (License No.)", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("🪪", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Magaca Damiinka (Guarantor Name)
                    OutlinedTextField(
                        value = guarantorName,
                        onValueChange = { guarantorName = it; errorMessage = null },
                        placeholder = { Text("Magaca Damiinka (Guarantor Full Name)", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("🤝", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Telefoonka Damiinka (Guarantor Phone)
                    OutlinedTextField(
                        value = guarantorPhone,
                        onValueChange = { guarantorPhone = it; errorMessage = null },
                        placeholder = { Text("Telefoonka Damiinka (61XXXXXXX)", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("📞", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Nambarka Warqadda CID (Police Criminal Clearance)
                    OutlinedTextField(
                        value = cidLetterNumber,
                        onValueChange = { cidLetterNumber = it; errorMessage = null },
                        placeholder = { Text("Nambarka Warqadda CID (CID Clearance)", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { Text("📜", fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp, end = 4.dp)) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Password
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
                        .testTag("input_signup_password"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Confirm Password
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; errorMessage = null },
                    placeholder = { Text("Ku celi furaha sirta", fontSize = 14.sp, color = Color(0xFF94A3B8)) },
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
                        .testTag("input_signup_confirm_password"),
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors,
                    singleLine = true
                )

                // 5. Koodhka Suuq-geynta / Referral Code (Keliya Darawalka - Naqshad yar oo fit ah)
                if (isDriverMode) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it.uppercase() },
                        placeholder = { Text("Koodhka Suuq-geynta (Ikhtiyaari)", fontSize = 12.5.sp, color = Color(0xFF94A3B8)) },
                        leadingIcon = { 
                            Text(
                                text = "🎁", 
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            ) 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("input_signup_referral_code"),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        singleLine = true
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFE50914),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Primary Register Button
                Button(
                    onClick = {
                        val cleanName = fullName.trim()
                        val cleanPhone = phoneNumber.trim().replace(" ", "")
                        if (cleanName.isBlank() || cleanPhone.isBlank() || password.isBlank()) {
                            errorMessage = "Fadlan buuxi dhammaan xogta."
                            return@Button
                        }
                        if (isMerchant) {
                            if (businessName.isBlank() || district.isBlank() || merchantPaymentNumber.isBlank()) {
                                errorMessage = "Fadlan buuxi magaca ganacsiga, degmada iyo nambarkaaga Till-ka."
                                return@Button
                            }
                        }
                        if (isCourier) {
                            if (licenseNumber.isBlank() || guarantorName.isBlank() || guarantorPhone.isBlank() || cidLetterNumber.isBlank()) {
                                errorMessage = "Gaarsiiyaha waxaa khasab ku ah: Shatiga, Damiinka & CID-da."
                                return@Button
                            }
                        }
                        if (password != confirmPassword) {
                            errorMessage = "Furaha sirta isma le'eka."
                            return@Button
                        }
                        if (password.length < 6) {
                            errorMessage = "Furaha sirta waa inuu ugu yaraan 6 xaraf yahay."
                            return@Button
                        }
                        viewModel.register(
                            name = cleanName,
                            phone = cleanPhone,
                            pass = password,
                            isDriver = isDriverMode,
                            vehicleModel = when {
                                isCourier -> "Mooto (Courier)"
                                isMerchant -> "Ganacsi ($businessType)"
                                else -> vehicleType
                            },
                            referralCode = if (isDriverMode) referralCode.trim().ifBlank { null } else null,
                            roleStr = initialRole,
                            licenseNumber = if (isCourier) licenseNumber.trim() else null,
                            guarantorName = if (isCourier) guarantorName.trim() else null,
                            guarantorPhone = if (isCourier) guarantorPhone.trim() else null,
                            cidLetterNumber = if (isCourier) cidLetterNumber.trim() else null,
                            businessName = if (isMerchant) businessName.trim() else null,
                            businessType = if (isMerchant) businessType else null,
                            merchantPaymentNumber = if (isMerchant) merchantPaymentNumber.trim() else null,
                            district = if (isMerchant) district.trim() else null
                        ) { user ->
                            onSignUpSuccess()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_register_submit"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(26.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text(
                            text = "Is-diiwaangeli (Register)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer Navigation Link to Login
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
                                .clickable { onBackClick() }
                                .padding(4.dp)
                        ) {
                            Text(
                                text = "Hore ma u leedahay akoon? ",
                                fontSize = 13.5.sp,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = "Gal (Login)",
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
