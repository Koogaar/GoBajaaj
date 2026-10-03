package com.example.somalitraffic.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.somalitraffic.data.model.SupportTicket
import com.example.somalitraffic.ui.SomaliTrafficViewModel
import com.example.somalitraffic.ui.components.SomaliTopBar
import com.example.ui.theme.*

@Composable
fun SupportCenterScreen(
    viewModel: SomaliTrafficViewModel,
    onBackClick: () -> Unit
) {
    val tickets by viewModel.supportTickets.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Saxmadda") }

    Scaffold(
        topBar = {
            SomaliTopBar(
                title = "Xarunta Caawinta",
                onBackClick = onBackClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = BrandRedPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.AddComment, contentDescription = "Fariin dir")
            }
        },
        containerColor = BrandWhite
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Support Hotline Card with Direct Call & WhatsApp
            com.example.somalitraffic.ui.components.GoBajaajContactHotlineCard(
                title = "Khadka Tooska ah ee GoBajaaj (24/7)",
                subtitle = "Wicitaan toos ah & WhatsApp Heegan ah"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fariimaha & Cabashooyinkaada (${tickets.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = TextBlack
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (tickets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Wali ma jiro wax fariin ama cabasho ah oo aad dirtay.\nGuji badhanka hoose si aad noola soo xiriirto.",
                        fontSize = 13.sp,
                        color = TextBlack,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(tickets) { ticket ->
                        TicketItemCard(ticket)
                    }
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Fariin u dir Taageerada", fontWeight = FontWeight.Bold, color = TextBlack) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Ciwaanka") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BrandWhite,
                                unfocusedContainerColor = BrandWhite,
                                focusedBorderColor = BrandRedPrimary,
                                unfocusedBorderColor = BrandBorderDark,
                                focusedTextColor = TextBlack,
                                unfocusedTextColor = TextBlack
                            )
                        )
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Faahfaahinta fariintaada") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = BrandWhite,
                                unfocusedContainerColor = BrandWhite,
                                focusedBorderColor = BrandRedPrimary,
                                unfocusedBorderColor = BrandBorderDark,
                                focusedTextColor = TextBlack,
                                unfocusedTextColor = TextBlack
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (subject.isNotBlank() && message.isNotBlank()) {
                                viewModel.createTicket(category, subject, message) {
                                    showCreateDialog = false
                                    subject = ""
                                    message = ""
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRedPrimary)
                    ) {
                        Text("Dir Fariinta", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Ka noqo", color = TextBlack, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
private fun TicketItemCard(ticket: SupportTicket) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BrandWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${ticket.ticketNumber} - ${ticket.subject}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextBlack
                )
                Surface(
                    color = BrandLightGray,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = ticket.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRedPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = ticket.message, fontSize = 12.sp, color = TextBlack)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "La diray: ${ticket.createdAt}", fontSize = 10.sp, color = TextBlack)
        }
    }
}
