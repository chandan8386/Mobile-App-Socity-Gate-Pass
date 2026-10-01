package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.VisitorEntry
import com.example.data.model.IntercomMessage
import com.example.data.model.IntercomResident
import com.example.ui.UserRole
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun IntercomMessagingDialog(
    targetResident: IntercomResident,
    visitor: VisitorEntry? = null,
    messages: List<IntercomMessage>,
    currentRole: UserRole,
    onSendMessage: (text: String) -> Unit,
    onDecisionReply: (messageId: String, decision: String) -> Unit,
    onDismiss: () -> Unit
) {
    var inputMessage by remember {
        mutableStateOf(
            if (visitor != null) {
                if (visitor.visitorType == "Delivery") "Delivery from ${visitor.visitorCompany.ifBlank { "Courier" }} is at the main gate. Please confirm entry."
                else if (visitor.visitorType == "Cab / Ride") "Cab (${visitor.vehicleNumber.ifBlank { "Ride" }}) has arrived at the gate for pickup. Confirm boarding?"
                else "Visitor ${visitor.visitorName} (${visitor.visitorType}) is at the gate. Allow access?"
            } else ""
        )
    }

    val quickTemplates = listOf(
        "Delivery arrived at Main Gate. Allow inside?",
        "Cab is at the gate. Confirm boarding?",
        "Guest is waiting at security booth. Allow entry?",
        "Please collect package from Guard Desk.",
        "Visitor request: Please call back on Intercom."
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 16.dp)
                .testTag("intercom_messaging_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Intercom • Flat ${targetResident.flatNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${targetResident.residentName} • EXT: ${targetResident.intercomExtension}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Visitor Context Banner
                if (visitor != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Verifying: ${visitor.visitorName} (${visitor.visitorType})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Vehicle: ${visitor.vehicleNumber.ifBlank { "Walking / None" }} • Flat ${visitor.flatNumber}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Chat Message Stream
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    reverseLayout = true,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val targetMessages = messages.filter { it.targetFlat == targetResident.flatNumber }
                    if (targetMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Forum, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "No previous messages for Flat ${targetResident.flatNumber}.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Send a verification inquiry below.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(targetMessages.reversed()) { msg ->
                            val isGuard = msg.senderRole == "GUARD"
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isGuard) Alignment.Start else Alignment.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isGuard) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFDCFCE7),
                                    border = BorderStroke(1.dp, if (isGuard) MaterialTheme.colorScheme.outlineVariant else Color(0xFF86EFAC)),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (isGuard) "Gate Security" else "Resident (${targetResident.flatNumber})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 10.sp,
                                                color = if (isGuard) MaterialTheme.colorScheme.primary else Color(0xFF15803D)
                                            )
                                            Text(
                                                text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(msg.timestamp)),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = msg.messageText, fontSize = 12.sp, lineHeight = 16.sp)

                                        // Decision status badge if resident approved/denied
                                        if (!msg.decision.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (msg.decision) {
                                                    "APPROVED" -> Color(0xFF16A34A)
                                                    "PARCEL_AT_GATE" -> Color(0xFFD97706)
                                                    else -> Color(0xFFDC2626)
                                                }
                                            ) {
                                                Text(
                                                    text = when (msg.decision) {
                                                        "APPROVED" -> "✓ ENTRY APPROVED BY RESIDENT"
                                                        "PARCEL_AT_GATE" -> "📦 KEEP PARCEL AT GATE"
                                                        else -> "✗ VISITOR ACCESS DENIED"
                                                    },
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        // Action buttons for Resident to respond
                                        if (isGuard && msg.decision == null && currentRole == UserRole.RESIDENT) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                FilledTonalButton(
                                                    onClick = { onDecisionReply(msg.id, "APPROVED") },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Allow", fontSize = 10.sp)
                                                }
                                                FilledTonalButton(
                                                    onClick = { onDecisionReply(msg.id, "PARCEL_AT_GATE") },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Keep at Gate", fontSize = 10.sp)
                                                }
                                                FilledTonalButton(
                                                    onClick = { onDecisionReply(msg.id, "DENIED") },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Deny", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Canned Templates Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickTemplates) { tmpl ->
                        SuggestionChip(
                            onClick = { inputMessage = tmpl },
                            label = { Text(tmpl, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Message Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Type message to Flat ${targetResident.flatNumber}...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("intercom_text_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = false,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                onSendMessage(inputMessage.trim())
                                inputMessage = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("btn_send_intercom_msg")
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
