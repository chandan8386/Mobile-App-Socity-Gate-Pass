package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.entity.VisitorEntry
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun formatRelativeEntryTime(timestamp: Long): String {
    val diffMs = System.currentTimeMillis() - timestamp
    val minutes = diffMs / (60 * 1000)
    val hours = diffMs / (60 * 60 * 1000)
    val days = diffMs / (24 * 60 * 60 * 1000)

    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))

    return when {
        minutes < 1 -> "Just now ($timeStr)"
        minutes < 60 -> "$minutes mins ago ($timeStr)"
        hours < 24 -> "$hours hrs ago ($timeStr)"
        days == 1L -> "Yesterday, $timeStr"
        else -> "$dateStr, $timeStr"
    }
}

/**
 * Screen displaying the list of recent visitors directly from the Room database,
 * ordered by entry time (timestamp DESC), showing name, type, and timestamp.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentVisitorsScreen(
    visitors: List<VisitorEntry>,
    onCheckOut: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    onOpenRegistrationForm: () -> Unit,
    onShowQrPass: (VisitorEntry) -> Unit = {},
    onOpenQrExitScanner: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onStartIntercomCall: (VisitorEntry) -> Unit = {},
    onOpenIntercomMessage: (VisitorEntry) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("ALL") }
    var selectedVisitorForDetail by remember { mutableStateOf<VisitorEntry?>(null) }

    // Ensure list is strictly ordered by entry timestamp descending
    val orderedRecentVisitors = remember(visitors, searchQuery, selectedTypeFilter) {
        val query = searchQuery.trim().lowercase()
        visitors
            .sortedByDescending { it.timestamp }
            .filter { visitor ->
                val typeMatch = when (selectedTypeFilter) {
                    "ALL" -> true
                    "INSIDE" -> visitor.status == "INSIDE"
                    "DELIVERY" -> visitor.visitorType.equals("Delivery", ignoreCase = true)
                    "GUEST" -> visitor.visitorType.equals("Guest", ignoreCase = true)
                    "CAB" -> visitor.visitorType.contains("Cab", ignoreCase = true)
                    "SERVICE" -> visitor.visitorType.contains("Service", ignoreCase = true)
                    "HELP" -> visitor.visitorType.contains("Help", ignoreCase = true)
                    "EXITED" -> visitor.status == "EXITED"
                    else -> visitor.visitorType.equals(selectedTypeFilter, ignoreCase = true)
                }

                // Explicitly filter by visitor name or visitor type as requested
                val searchMatch = query.isBlank() ||
                        visitor.visitorName.lowercase().contains(query) ||
                        visitor.visitorType.lowercase().contains(query) ||
                        visitor.visitorCompany.lowercase().contains(query) ||
                        visitor.flatNumber.lowercase().contains(query)

                typeMatch && searchMatch
            }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenRegistrationForm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_register_recent_visitor")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Register Visitor", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 1. TOP SEARCH BAR (Filtered by visitor name or visitor type)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or visitor type (e.g. Delivery, Rahul)...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("clear_search_button")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .testTag("visitor_search_bar")
            )

            // 2. QUICK TYPE & STATUS FILTER CHIPS
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val filterChips = listOf(
                    "ALL" to "All Types",
                    "DELIVERY" to "📦 Delivery",
                    "GUEST" to "👥 Guests",
                    "CAB" to "🚖 Cab / Ride",
                    "SERVICE" to "🛠️ Service",
                    "HELP" to "🧹 Daily Help",
                    "INSIDE" to "🟢 Inside Gate",
                    "EXITED" to "🚪 Exited"
                )
                items(filterChips) { (key, label) ->
                    FilterChip(
                        selected = selectedTypeFilter == key,
                        onClick = {
                            selectedTypeFilter = if (selectedTypeFilter == key && key != "ALL") "ALL" else key
                        },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedTypeFilter == key) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("filter_chip_$key")
                    )
                }
            }

            // 3. SEARCH RESULTS SUMMARY HEADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedTypeFilter != "ALL") {
                            "Search Results (${orderedRecentVisitors.size})"
                        } else {
                            "Recent Visitors Log"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Filtering by \"$searchQuery\" • Ordered by Entry Time"
                        else "Real-time records ordered by entry time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (searchQuery.isNotBlank() || selectedTypeFilter != "ALL") {
                    TextButton(
                        onClick = {
                            searchQuery = ""
                            selectedTypeFilter = "ALL"
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("reset_search_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 11.sp)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportCsv,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("recent_export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onOpenQrExitScanner,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("scan_qr_exit_button")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Scan QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Visitor Cards List
            if (orderedRecentVisitors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No visitors found matching \"$searchQuery\""
                            else "No visitors found in this filter",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Try searching by visitor name (e.g. Rahul) or type (e.g. Delivery, Guest)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {
                                searchQuery = ""
                                selectedTypeFilter = "ALL"
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Filters")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("recent_visitors_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(orderedRecentVisitors, key = { it.id }) { visitor ->
                        RecentVisitorItemCard(
                            visitor = visitor,
                            onClick = { selectedVisitorForDetail = visitor },
                            onCheckOut = { onCheckOut(visitor.id) },
                            onCheckIn = { onCheckIn(visitor.id) },
                            onShowQrPass = { onShowQrPass(visitor) },
                            onStartIntercomCall = { onStartIntercomCall(visitor) },
                            onOpenIntercomMessage = { onOpenIntercomMessage(visitor) }
                        )
                    }
                }
            }
        }
    }

    // Detail dialog when tapping any card
    selectedVisitorForDetail?.let { visitor ->
        VisitorDetailDialog(
            visitor = visitor,
            onDismiss = { selectedVisitorForDetail = null },
            onCheckOut = {
                onCheckOut(visitor.id)
                selectedVisitorForDetail = null
            },
            onShowQrPass = {
                onShowQrPass(visitor)
                selectedVisitorForDetail = null
            },
            onStartIntercomCall = {
                onStartIntercomCall(visitor)
                selectedVisitorForDetail = null
            },
            onOpenIntercomMessage = {
                onOpenIntercomMessage(visitor)
                selectedVisitorForDetail = null
            }
        )
    }
}

/**
 * Individual visitor card highlighting: Name, Type, and Entry Timestamp.
 */
@Composable
fun RecentVisitorItemCard(
    visitor: VisitorEntry,
    onClick: () -> Unit,
    onCheckOut: () -> Unit,
    onCheckIn: () -> Unit,
    onShowQrPass: () -> Unit = {},
    onStartIntercomCall: () -> Unit = {},
    onOpenIntercomMessage: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_visitor_item_${visitor.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Type Icon + Name + Visitor Type Badge + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (typeIcon, typeBgColor, typeFgColor) = when (visitor.visitorType) {
                        "Delivery" -> Triple(Icons.Default.LocalShipping, Color(0xFFEFF6FF), Color(0xFF2563EB))
                        "Cab / Ride" -> Triple(Icons.Default.DirectionsCar, Color(0xFFFEF3C7), Color(0xFFD97706))
                        "Service / Repair" -> Triple(Icons.Default.Handyman, Color(0xFFF3E8FF), Color(0xFF7E22CE))
                        "Daily Help" -> Triple(Icons.Default.CleaningServices, Color(0xFFECFDF5), Color(0xFF059669))
                        else -> Triple(Icons.Default.Person, Color(0xFFF1F5F9), Color(0xFF334155))
                    }

                    val hasPhoto = !visitor.photoUri.isNullOrBlank() && File(visitor.photoUri).exists()
                    if (hasPhoto) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        ) {
                            AsyncImage(
                                model = File(visitor.photoUri!!),
                                contentDescription = "Visitor Photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(typeBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                typeIcon,
                                contentDescription = visitor.visitorType,
                                tint = typeFgColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        // 1. VISITOR NAME
                        Text(
                            text = visitor.visitorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // 2. VISITOR TYPE
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = typeBgColor
                            ) {
                                Text(
                                    text = visitor.visitorType,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = typeFgColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (visitor.visitorCompany.isNotBlank() && visitor.visitorCompany != "Personal") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${visitor.visitorCompany})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (visitor.parkingSlot.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocalParking,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = visitor.parkingSlot,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                StatusBadge(status = visitor.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Middle Box: Entry Timestamp and Destination Flat
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 3. TIMESTAMP (Entry Time)
                    Column {
                        Text(
                            text = "ENTRY TIMESTAMP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatRelativeEntryTime(visitor.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Destination Flat
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DESTINATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${visitor.tower} • ${visitor.flatNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Exit timestamp or Quick Action Buttons
            val exitMs = visitor.exitTimestamp
            if (exitMs != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Exited at: ${formatTimestamp(exitMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Vehicle: ${visitor.vehicleNumber.ifBlank { "Walking" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Action Buttons & QR Pass
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onShowQrPass,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("visitor_qr_pass_btn_${visitor.id}")
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Pass", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onStartIntercomCall,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7))
                            .testTag("intercom_call_btn_${visitor.id}")
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Call Flat Intercom",
                            tint = Color(0xFF15803D),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onOpenIntercomMessage,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("intercom_msg_btn_${visitor.id}")
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Message Flat Intercom",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (visitor.status == "INSIDE") {
                        Button(
                            onClick = onCheckOut,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Exit", fontSize = 11.sp)
                        }
                    } else if (visitor.status == "APPROVED") {
                        Button(
                            onClick = onCheckIn,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark Inside", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed modal showing full visitor entry record, phone, OTP, timestamps.
 */
@Composable
fun VisitorDetailDialog(
    visitor: VisitorEntry,
    onDismiss: () -> Unit,
    onCheckOut: () -> Unit,
    onShowQrPass: () -> Unit = {},
    onStartIntercomCall: () -> Unit = {},
    onOpenIntercomMessage: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("visitor_detail_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visitor Entry Record",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                DetailItemRow("Visitor Name", visitor.visitorName)
                DetailItemRow("Visitor Type", visitor.visitorType)
                DetailItemRow("Phone Number", visitor.visitorPhoneNumber)
                DetailItemRow("Entry Time", formatTimestamp(visitor.timestamp))
                if (visitor.exitTimestamp != null) {
                    DetailItemRow("Exit Time", formatTimestamp(visitor.exitTimestamp))
                }
                DetailItemRow("Destination", "${visitor.tower} - ${visitor.flatNumber}")
                DetailItemRow("Host Resident", visitor.hostResidentName)
                DetailItemRow("Vehicle Number", visitor.vehicleNumber.ifBlank { "Walking / None" })
                if (visitor.parkingSlot.isNotBlank()) {
                    DetailItemRow("Assigned Parking Bay", visitor.parkingSlot)
                }
                DetailItemRow("QR Pass Token", visitor.effectiveQrToken)
                DetailItemRow("Gate Passcode", if (visitor.passCode.isNotBlank()) visitor.passCode else "N/A")
                DetailItemRow("Current Status", visitor.status)
                if (visitor.remarks.isNotBlank()) {
                    DetailItemRow("Remarks", visitor.remarks)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Intercom verification buttons inside Detail Dialog
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Resident Verification Intercom:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onStartIntercomCall,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_intercom_call_btn")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call Flat", fontSize = 11.sp)
                            }

                            FilledTonalButton(
                                onClick = onOpenIntercomMessage,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("detail_intercom_msg_btn")
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Message", fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onShowQrPass,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Digital QR Entry Pass")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                    if (visitor.status == "INSIDE") {
                        Button(
                            onClick = onCheckOut,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Mark Exit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}
