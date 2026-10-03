#!/bin/bash
cat << 'INNER_EOF' > app/src/main/java/com/example/somalitraffic/ui/screens/LoginScreen.kt
package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.R
import com.example.somalitraffic.data.model.UserRole
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.ui.theme.*

@Composable
fun LoginScreen(
    viewModel: SomaliTrafficViewModel,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (UserRole) -> Unit
) {
    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.uiMessage.collect { msg ->
            errorMessage = msg
        }
    }

    Scaffold(
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Emblem
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = BrandRedPrimary,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SOMALI",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = BrandRedPrimary
            )
            Text(
                text = "TRAFFIC",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = BrandBlack
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Ku soo dhawoow",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
            Text(
                text = "Gal si aad u isticmaasho akoonkaaga",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = BrandWhite,
                unfocusedContainerColor = BrandWhite,
                focusedBorderColor = BrandRedPrimary,
                unfocusedBorderColor = BrandBorder,
                focusedTextColor = TextBlack,
                unfocusedTextColor = TextBlack,
                cursorColor = BrandRedPrimary
            )

            // Identifier Input
            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    errorMessage = null
                },
                placeholder = {
                    Column {
                        Text("Phone number", fontSize = 12.sp, color = TextBlack, fontWeight = FontWeight.Bold)
                        Text("Taleefanka ama Emailka", fontSize = 14.sp, color = Color.Gray)
                    }
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Person, contentDescription = null, tint = TextBlack)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_login_identifier"),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                placeholder = { Text("Furaha sirta", color = Color.Gray) },
                leadingIcon = {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = TextBlack)
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                            tint = Color.Gray
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_login_password"),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { rememberMe = !rememberMe }) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(if (rememberMe) BrandRedPrimary else BrandWhite, RoundedCornerShape(4.dp))
                            .border(1.dp, if (rememberMe) BrandRedPrimary else BrandBorder, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rememberMe) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Xasuusnow", fontSize = 14.sp, color = TextBlack)
                }
                Text("Furaha sirta ilaaway?", fontSize = 14.sp, color = BrandRedPrimary, fontWeight = FontWeight.Bold)
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = BrandRedPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (identifier.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(identifier, password) { user ->
                            onLoginSuccess(user.role)
                        }
                    } else {
                        errorMessage = "Fadlan buuxi dhammaan xogta"
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("btn_submit_login"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                shape = RoundedCornerShape(28.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Gal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.clickable { onNavigateToSignUp() }, verticalAlignment = Alignment.CenterVertically) {
                Text("Ma lihid akoon? ", fontSize = 14.sp, color = TextBlack)
                Text("Isdiiwaangeli", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandRedPrimary)
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Ama", fontSize = 14.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { /* TODO: Google Auth */ },
                    modifier = Modifier.weight(1f).height(48.dp).border(1.dp, BrandBorder, RoundedCornerShape(24.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Google", color = TextBlack, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { /* TODO: Apple Auth */ },
                    modifier = Modifier.weight(1f).height(48.dp).border(1.dp, BrandBorder, RoundedCornerShape(24.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandWhite),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Apple", color = TextBlack, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
INNER_EOF
