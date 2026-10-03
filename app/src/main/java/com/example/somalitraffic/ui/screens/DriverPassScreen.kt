package com.example.somalitraffic.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.*

enum class PaymentStep {
    SELECT_METHOD,
    INSTRUCTIONS,
    INPUT_DETAILS,
    PENDING_STATUS,
    APPROVED_STATUS,
    REJECTED_STATUS
}

data class PaymentMethodInfo(
    val name: String,
    val ussdCode: String,
    val badgeColor: Color,
    val accountNo: String
)

private fun dialUssdCode(context: Context, ussdCode: String) {
    try {
        val encodedHash = Uri.encode("#")
        val ussdUri = Uri.parse("tel:" + ussdCode.replace("#", encodedHash))
        val dialIntent = Intent(Intent.ACTION_DIAL, ussdUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialIntent)
    } catch (e: Exception) {
        android.util.Log.e("DriverPassScreen", "Error dialing USSD: $ussdCode", e)
    }
}

@Composable
fun DriverPassScreen(
    viewModel: SomaliTrafficViewModel,
    onPaymentSuccess: () -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentSub by viewModel.subscription.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    val paymentMethods = listOf(
        PaymentMethodInfo("EVC Plus", "*712*618092060*0.25#", Color(0xFF10B981), "618092060"),
        PaymentMethodInfo("eDahab", "*712*628512131*0.25#", Color(0xFFF59E0B), "628512131")
    )

    var selectedMethod by remember { mutableStateOf(paymentMethods[0]) }
    var currentStep by remember { mutableStateOf(PaymentStep.SELECT_METHOD) }

    var senderPhone by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submittedDate by remember { mutableStateOf("") }

    // Sync state if user already has an active, pending, or rejected subscription
    LaunchedEffect(currentSub) {
        val sub = currentSub
        if (sub != null) {
            when (sub.status.lowercase()) {
                "active", "completed", "approved" -> {
                    currentStep = PaymentStep.APPROVED_STATUS
                }
                "pending" -> {
                    currentStep = PaymentStep.PENDING_STATUS
                    if (!sub.senderPhone.isNullOrBlank()) {
                        senderPhone = sub.senderPhone
                    }
                }
                "rejected", "failed" -> {
                    currentStep = PaymentStep.REJECTED_STATUS
                }
            }
        }
    }

    // Continuous Live Sync: Polls Supabase every 2 seconds so when Admin approves on Web, the app unlocks immediately!
    LaunchedEffect(senderPhone) {
        val phoneToSync = senderPhone.ifBlank { currentSub?.senderPhone }
        viewModel.syncCurrentSubscription(phoneToSync) { isApproved, _ ->
            if (isApproved) {
                currentStep = PaymentStep.APPROVED_STATUS
            }
        }
        while (isActive) {
            delay(2000)
            val currentPhone = senderPhone.ifBlank { currentSub?.senderPhone }
            viewModel.syncCurrentSubscription(currentPhone) { isApproved, _ ->
                if (isApproved) {
                    currentStep = PaymentStep.APPROVED_STATUS
                }
            }
        }
    }

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Adeegga Maalinlaha ah ($0.25)",
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
            // Header Plan Banner
            Surface(
                color = GoBajaajNavy,
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(2.dp, GoBajaajYellow),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(GoBajaajYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = null,
                                tint = GoBajaajNavy,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Adeegga Maalinlaha ah ($0.25)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Kharashka: $0.25 USD (05:00 AM ilaa 05:00 AM)",
                                fontSize = 12.sp,
                                color = GoBajaajYellow,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2 Key Exclusive Driver Features List
                    Text(
                        text = "Faa'iidooyinka Gaarka ah ee Darwalka:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• ⏰ Shift A/B: Lacagta aad shubato waxay shaqaynaysaa ilaa 05:00 AM ee subaxda berri.\n" +
                               "• 🚦 Xogta Waddooyinka: Ka fiirso waddooyinka saxmadda leh inta aadan bixin si aad u doorato jidka ugu habboon.\n" +
                               "• 🚖 Dalabaadka Rakaabka: Hel dhammaan dalabaadka tooska ah ee GoBajaaj.",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (currentStep) {
                PaymentStep.SELECT_METHOD -> {
                    // STEP 1: SELECT PAYMENT METHOD
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "1. Dooro Habka Lacag-bixinta",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        Text(
                            text = "Waa kaa dhammaaday adeegga maalinlaha ah ee GoBajaaj, fadlan dib ugu shubo ($0.25) si aad shaqada u wadato.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        paymentMethods.forEach { method ->
                            val isSelected = selectedMethod.name == method.name
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp)
                                    .clickable { selectedMethod = method },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFFF3F4F6) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) BrandRedPrimary else BrandBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(method.badgeColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = method.name.take(2).uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = method.name,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextBlack
                                            )
                                            Text(
                                                text = method.ussdCode,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BrandRedPrimary
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                selectedMethod = method
                                                dialUssdCode(context, method.ussdCode)
                                            },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFDCFCE7))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Toos u wac ${method.ussdCode}",
                                                tint = Color(0xFF16A34A),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedMethod = method },
                                            colors = RadioButtonDefaults.colors(selectedColor = BrandRedPrimary)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { currentStep = PaymentStep.INSTRUCTIONS },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Sii Wad (Continue)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                PaymentStep.INSTRUCTIONS -> {
                    // STEP 2: PAYMENT INSTRUCTIONS & DIRECT CALL
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "2. Bixi Adeegga Maalinlaha ah ($0.25)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextBlack
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = selectedMethod.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(selectedMethod.badgeColor)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            color = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Habka: ${selectedMethod.name}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Lambarka Akoonka: ${selectedMethod.accountNo}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Qiimaha: $0.25 USD (Adeegga Maalinlaha ah)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Big Direct Call Button
                        Button(
                            onClick = { dialUssdCode(context, selectedMethod.ussdCode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_direct_dial_ussd"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Toos U Wac (${selectedMethod.ussdCode})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { currentStep = PaymentStep.INPUT_DETAILS },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_waan_bixiyay"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Waan Bixiyay (Geli Faahfaahinta)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        TextButton(
                            onClick = { currentStep = PaymentStep.SELECT_METHOD },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text("Dib u dooro habka", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }

                PaymentStep.INPUT_DETAILS -> {
                    // STEP 3: SUBMIT DETAILS (Phone + Reference)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "3. Geli Lambarka Soo Diray",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack
                        )
                        Text(
                            text = "App-ku wuxuu u diri doonaa faahfaahinta Admin-ka si loo xaqiijiyo.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        Text("Lambarka aad lacagta ka soo dirtay *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextBlack)
                        Text(
                            text = "Geli kaliya lambarka aad lacagta ka soo wareejisay si Admin-ku kuugu xaqiijiyo.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 2.dp, bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = senderPhone,
                            onValueChange = { senderPhone = it; errorMessage = null },
                            placeholder = { Text("Tusaale: 61XXXXXXX ama 061XXXXXXX") },
                            modifier = Modifier.fillMaxWidth().testTag("input_sender_phone"),
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
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val cleanedSenderPhone = senderPhone.trim().replace(" ", "")
                                val senderDigits = cleanedSenderPhone.filter { it.isDigit() }
                                if (senderDigits.length < 9) {
                                    errorMessage = "Fadlan geli lambarka saxda ah ee aad lacagta ka soo dirtay (ugu yaraan 9 lambar)."
                                    return@Button
                                }
                                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
                                submittedDate = dateStr
                                errorMessage = null
                                viewModel.payDriverPass(
                                    senderPhone = senderPhone.trim(),
                                    destinationPhone = selectedMethod.accountNo,
                                    paymentMethod = selectedMethod.name,
                                    onSuccess = {
                                        currentStep = PaymentStep.PENDING_STATUS
                                    },
                                    onError = { message ->
                                        errorMessage = message
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_xaqiiji_lacagta"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                            shape = RoundedCornerShape(14.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                            } else {
                                Text("Xaqiiji oo U Dir Admin-ka", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        TextButton(
                            onClick = { currentStep = PaymentStep.INSTRUCTIONS },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Text("Ka noqo", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                }

                PaymentStep.PENDING_STATUS -> {
                    // STEP 4: PENDING SCREEN (Sugaya Ansixinta Admin-ka)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Lacag-bixintaada waa la helay",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Waxaan xaqiijinaynaa lambarka aad ka soo dirtay. Fadlan sug inta Admin-ku ka ansixinayo.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                        )

                        Surface(
                            color = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                DetailRow("Lambarka laga diray:", senderPhone.ifBlank { currentSub?.senderPhone ?: "61XXXXXXX" })
                                DetailRow("Lacagta:", "$0.25 USD")
                                DetailRow("Habka:", selectedMethod.name)
                                DetailRow("Xaaladda:", "SUGAYSA (Awaiting Admin)", color = Color(0xFFD97706))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Automatic listener indicator
                        Surface(
                            color = Color(0xFFEFF6FF),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF2563EB),
                                    strokeWidth = 2.5.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Shaashaddu si toos ah (automatically) ayay isu beddelaysaa marka Admin-ku ansixiyo...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF1E40AF),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        val isRefreshing by viewModel.isRefreshingSubscription.collectAsState()
                        var refreshMsg by remember { mutableStateOf<String?>(null) }

                        OutlinedButton(
                            onClick = {
                                refreshMsg = null
                                val phoneToSync = senderPhone.ifBlank { currentSub?.senderPhone }
                                viewModel.syncCurrentSubscription(phoneToSync) { isApproved, msg ->
                                    refreshMsg = msg
                                    if (isApproved) {
                                        currentStep = PaymentStep.APPROVED_STATUS
                                    }
                                }
                            },
                            enabled = !isRefreshing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoBajaajNavy)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Waa la hubinayaa...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cusbooneysii Status-ka (Refresh)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (refreshMsg != null) {
                            Text(
                                text = refreshMsg!!,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentStep == PaymentStep.APPROVED_STATUS) Color(0xFF10B981) else Color(0xFFD97706),
                                modifier = Modifier.padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        com.example.somalitraffic.ui.components.GoBajaajContactMiniRow(
                            label = "Caawinaad degdeg ah ama ansixin:"
                        )
                    }
                }

                PaymentStep.APPROVED_STATUS -> {
                    // STEP 5: APPROVED / ACTIVE STATUS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1FAE5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Waad ku guuleysatay!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Adeegga maalinlaha ah waa la ansixiyay ($0.25).",
                            fontSize = 13.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        Surface(
                            color = Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                DetailRow("Xaaladda Adeegga:", "ACTIVE (Shaqeynaya)", color = Color(0xFF10B981))
                                DetailRow("Lambarka:", senderPhone.ifBlank { currentSub?.senderPhone ?: "61XXXXXXX" })
                                DetailRow("Habka:", selectedMethod.name)
                                DetailRow("Qiimaha:", "$0.25 USD")
                                DetailRow("Shift-ka:", "05:00 AM ilaa 05:00 AM ($0.25)")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { onPaymentSuccess() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_complete_pass_success"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Guda-gal Shaqada (Home)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                PaymentStep.REJECTED_STATUS -> {
                    // STEP 6: REJECTED STATUS
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = null,
                                tint = BrandRedPrimary,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Lacag-bixintaada waa la diiday",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextBlack,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = currentSub?.rejectionReason ?: "Fadlan la xiriir taageerada.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )

                        Button(
                            onClick = { currentStep = PaymentStep.SELECT_METHOD },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Dib U Codso Lacag-bixinta", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, color: Color = TextBlack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
