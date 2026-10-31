package com.example.somalitraffic.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.DriverExpenseEntry
import com.example.somalitraffic.data.model.FinanceTimeframe
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverFinancesScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val expenses by viewModel.driverExpenses.collectAsState()
    var selectedTimeframe by remember { mutableStateOf(FinanceTimeframe.DAILY) }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogIsIncome by remember { mutableStateOf(false) }
    var inputAmount by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("shidaal") }
    var inputNote by remember { mutableStateOf("") }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val currentMonthStr = remember {
        SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    }
    val currentYearStr = remember {
        SimpleDateFormat("yyyy", Locale.US).format(Date())
    }

    // Filter entries based on timeframe
    val filteredEntries = remember(expenses, selectedTimeframe) {
        val now = System.currentTimeMillis()
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

        expenses.filter { entry ->
            when (selectedTimeframe) {
                FinanceTimeframe.DAILY -> entry.date == todayStr || entry.timestampMs >= (now - 24 * 60 * 60 * 1000)
                FinanceTimeframe.WEEKLY -> entry.timestampMs >= sevenDaysAgo
                FinanceTimeframe.MONTHLY -> entry.date.startsWith(currentMonthStr)
                FinanceTimeframe.YEARLY -> entry.date.startsWith(currentYearStr)
            }
        }
    }

    val totalIncome = remember(filteredEntries) {
        filteredEntries.filter { it.isIncome }.sumOf { it.amount }
    }
    val totalExpense = remember(filteredEntries) {
        filteredEntries.filter { !it.isIncome }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Buugga Dhaqaalaha (Ledger)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dakhliga, Kharashaadka & Faa'iidada Saafiga ah",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GoBajaajNavy)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Timeframe Selector Tabs
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FinanceTimeframe.values().forEach { tf ->
                        val isSelected = selectedTimeframe == tf
                        Surface(
                            onClick = { selectedTimeframe = tf },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) GoBajaajBlue else Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tf.labelSomali,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isSelected) Color.White else GoBajaajNavy
                                )
                                Text(
                                    text = tf.subtitleSomali,
                                    fontSize = 9.5.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Summary Cards
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Warbixinta: ${selectedTimeframe.labelSomali} (${selectedTimeframe.subtitleSomali})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Income Box
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF16A34A))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Dakhli (Soo Galay)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF15803D))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", totalIncome)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }

                                // Expense Box
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFFEF2F2),
                                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFDC2626))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Kharash (Baxay)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB91C1C))
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$${String.format(Locale.US, "%.2f", totalExpense)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Net Balance (Hadhaa / Faa'iido)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (netBalance >= 0) GoBajaajNavy else Color(0xFF7F1D1D),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Hadhaaga / Faa'iidada Saafiga ah",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = if (netBalance >= 0) "Macaash fiican ayaad ku jirtaa" else "Kharashka ayaa dakhliga ka badan",
                                            fontSize = 10.sp,
                                            color = if (netBalance >= 0) GoBajaajYellow else Color(0xFFFCA5A5)
                                        )
                                    }
                                    Text(
                                        text = "$${String.format(Locale.US, "%.2f", netBalance)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (netBalance >= 0) GoBajaajYellow else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // Quick Action Buttons (+ Dakhli, - Kharash)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                dialogIsIncome = true
                                inputCategory = "safar_caadi"
                                inputAmount = ""
                                inputNote = ""
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+ Ku Dar Dakhli", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                dialogIsIncome = false
                                inputCategory = "shidaal"
                                inputAmount = ""
                                inputNote = ""
                                showAddDialog = true
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("- Ku Dar Kharash", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Transaction History Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Liiska Wax-is-beddelka (${filteredEntries.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoBajaajNavy
                        )
                    }
                }

                if (filteredEntries.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(42.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Wax xisaab ah looma helin xilligan",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Riiix badhamada kor ku yaal si aad dakhli ama kharash u darto",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredEntries, key = { it.id }) { entry ->
                        ExpenseItemCard(
                            entry = entry,
                            onDelete = { viewModel.deleteDriverExpense(entry.id) }
                        )
                    }
                }
            }
        }
    }

    // Add Income / Expense Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = if (dialogIsIncome) "🟢 Ku Dar Dakhli Cusub" else "🔴 Ku Dar Kharash Cusub",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoBajaajNavy
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Geli lacagta ($ USD):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = inputAmount,
                        onValueChange = { inputAmount = it },
                        placeholder = { Text("1.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = GoBajaajNavy) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (dialogIsIncome) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    )

                    Text(
                        text = "Nooca:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )

                    // Category chips
                    val categories = if (dialogIsIncome) {
                        listOf(
                            "safar_caadi" to "Safar Caadi ah",
                            "safar_gobajaaj" to "Safar GoBajaaj",
                            "xamaali" to "Xamaali / Xirmo",
                            "kale" to "Dakhli Kale"
                        )
                    } else {
                        listOf(
                            "shidaal" to "Batrool / Shidaal",
                            "pass" to "Driver Pass",
                            "dayactir" to "Dayactir / Taayir",
                            "kiro" to "Kirada Bajaajta",
                            "cunto" to "Cunto & Shaah",
                            "kale" to "Kharash Kale"
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                pair.forEach { (catKey, catLabel) ->
                                    val isSelected = inputCategory == catKey
                                    Surface(
                                        onClick = { inputCategory = catKey },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) (if (dialogIsIncome) Color(0xFF16A34A) else Color(0xFFDC2626)) else Color(0xFFF1F5F9),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = catLabel,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else GoBajaajNavy,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = "Faahfaahin kooban (Ikhtiyaari):",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = inputNote,
                        onValueChange = { inputNote = it },
                        placeholder = { Text("Tusaale: Batroolka KM4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = inputAmount.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            val entry = DriverExpenseEntry(
                                type = if (dialogIsIncome) "income" else "expense",
                                category = inputCategory,
                                amount = amount,
                                notes = inputNote.trim(),
                                date = todayStr
                            )
                            viewModel.saveDriverExpense(entry) {
                                showAddDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (dialogIsIncome) Color(0xFF16A34A) else Color(0xFFDC2626)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Kaydi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Ka Noqo", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun ExpenseItemCard(
    entry: DriverExpenseEntry,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (entry.isIncome) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (entry.isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = null,
                    tint = if (entry.isIncome) Color(0xFF16A34A) else Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.categoryLabelSomali,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = GoBajaajNavy
                )
                if (entry.notes.isNotBlank()) {
                    Text(
                        text = entry.notes,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Text(
                    text = entry.date,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
            }
            Text(
                text = (if (entry.isIncome) "+" else "-") + "$" + String.format(Locale.US, "%.2f", entry.amount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (entry.isIncome) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

