package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.VisitorEntry
import com.example.ui.UserRole
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatTimestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorsScreen(
    visitors: List<VisitorEntry>,
    currentRole: UserRole,
    currentFlat: String,
    onApprove: (Long) -> Unit,
    onDeny: (Long) -> Unit,
    onCheckIn: (Long) -> Unit,
    onCheckOut: (Long) -> Unit,
    onOpenRegistrationForm: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onRegisterVisitor: (
        name: String, phone: String, purpose: String, company: String,
        vehicleNo: String, tower: String, flatNo: String, hostName: String,
        isPreApproved: Boolean, remarks: String
    ) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, INSIDE, PENDING, EXITED
    var showAddDialog by remember { mutableStateOf(false) }
    var showPreInviteDialog by remember { mutableStateOf(false) }

    val insideCount = visitors.count { it.status == "INSIDE" }
    val waitingApprovalCount = visitors.count { it.status == "WAITING_APPROVAL" }
    val exitedCount = visitors.count { it.status == "EXITED" }

    // Filter visitors based on query and filter tab
    val filteredVisitors = remember(visitors, searchQuery, selectedFilter, currentRole, currentFlat) {
        visitors.filter { visitor ->
            val matchesRole = if (currentRole == UserRole.RESIDENT) {
                visitor.flatNumber == currentFlat || visitor.tower == currentFlat.split("-").firstOrNull()?.let { "Tower $it" }
            } else true

            val matchesFilter = when (selectedFilter) {
                "INSIDE" -> visitor.status == "INSIDE"
                "PENDING" -> visitor.status == "WAITING_APPROVAL"
                "EXITED" -> visitor.status == "EXITED"
                else -> true
            }

            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isBlank() ||
                    visitor.visitorName.lowercase().contains(q) ||
                    visitor.visitorType.lowercase().contains(q) ||
                    visitor.purpose.lowercase().contains(q) ||
                    visitor.visitorCompany.lowercase().contains(q) ||
                    visitor.flatNumber.lowercase().contains(q) ||
                    visitor.vehicleNumber.lowercase().contains(q)

            matchesRole && matchesFilter && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentRole == UserRole.RESIDENT) {
                        showPreInviteDialog = true
                    } else {
                        onOpenRegistrationForm()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_visitor_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (currentRole == UserRole.RESIDENT) Icons.Default.QrCode else Icons.Default.PersonAdd,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentRole == UserRole.RESIDENT) "Invite Guest" else "Log Entry",
                        fontWeight = FontWeight.SemiBold
                    )
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
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "Inside Gate",
                    value = "$insideCount",
                    icon = Icons.Default.MeetingRoom,
                    iconColor = Color(0xFF16A34A),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Pending Approval",
                    value = "$waitingApprovalCount",
                    icon = Icons.Default.PendingActions,
                    iconColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Exited Today",
                    value = "$exitedCount",
                    icon = Icons.Default.DoneAll,
                    iconColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Register Visitor Form Action Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visitor Gate Logs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onExportCsv,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("export_csv_btn_visitors_screen")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onOpenRegistrationForm,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("open_registration_form_button")
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Entry Form", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by visitor name or type (e.g. Delivery, Rahul)...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("visitor_search_field")
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(
                    label = "All (${visitors.size})",
                    selected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" }
                )
                FilterChipItem(
                    label = "Inside ($insideCount)",
                    selected = selectedFilter == "INSIDE",
                    onClick = { selectedFilter = "INSIDE" }
                )
                FilterChipItem(
                    label = "Awaiting ($waitingApprovalCount)",
                    selected = selectedFilter == "PENDING",
                    onClick = { selectedFilter = "PENDING" }
                )
                FilterChipItem(
                    label = "Exited ($exitedCount)",
                    selected = selectedFilter == "EXITED",
                    onClick = { selectedFilter = "EXITED" }
                )
            }

            // Visitors List
            if (filteredVisitors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DirectionsWalk,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No visitor entries found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Use the button below to register a new visitor or invite guest",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredVisitors, key = { it.id }) { visitor ->
                        VisitorCard(
                            visitor = visitor,
                            currentRole = currentRole,
                            currentFlat = currentFlat,
                            onApprove = { onApprove(visitor.id) },
                            onDeny = { onDeny(visitor.id) },
                            onCheckIn = { onCheckIn(visitor.id) },
                            onCheckOut = { onCheckOut(visitor.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddVisitorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, purpose, company, vehicleNo, tower, flatNo, hostName, preApproved, remarks ->
                onRegisterVisitor(name, phone, purpose, company, vehicleNo, tower, flatNo, hostName, preApproved, remarks)
                showAddDialog = false
            }
        )
    }

    if (showPreInviteDialog) {
        PreInviteGuestDialog(
            hostFlat = currentFlat,
            onDismiss = { showPreInviteDialog = false },
            onConfirm = { name, phone, purpose, vehicleNo, remarks ->
                val tower = "Tower ${currentFlat.split("-").firstOrNull() ?: "A"}"
                onRegisterVisitor(name, phone, purpose, "Personal", vehicleNo, tower, currentFlat, "Host Resident", true, remarks)
                showPreInviteDialog = false
            }
        )
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun VisitorCard(
    visitor: VisitorEntry,
    currentRole: UserRole,
    currentFlat: String,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
    onCheckIn: () -> Unit,
    onCheckOut: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visitor_card_${visitor.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name, purpose, status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val purposeIcon = when (visitor.purpose) {
                        "Delivery" -> Icons.Default.LocalShipping
                        "Cab / Ride" -> Icons.Default.DirectionsCar
                        "Service / Repair" -> Icons.Default.Handyman
                        "Daily Help" -> Icons.Default.CleaningServices
                        else -> Icons.Default.Person
                    }
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(purposeIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = visitor.visitorName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (visitor.visitorCompany.isNotBlank() && visitor.visitorCompany != "Personal") {
                                "${visitor.purpose} (${visitor.visitorCompany})"
                            } else {
                                visitor.purpose
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(visitor.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Destination Flat",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${visitor.tower} • ${visitor.flatNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = "Vehicle / Pass",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (visitor.vehicleNumber.isNotBlank() && visitor.vehicleNumber != "None") visitor.vehicleNumber else "Passcode: ${visitor.passCode}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Entry Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatTimestamp(visitor.entryTime).split(",").lastOrNull()?.trim() ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (visitor.remarks.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Remarks: ${visitor.remarks}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Action Buttons based on status & role
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If waiting for approval and resident or guard
                if (visitor.status == "WAITING_APPROVAL") {
                    OutlinedButton(
                        onClick = onDeny,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("deny_visitor_${visitor.id}")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Deny")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("approve_visitor_${visitor.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve Entry")
                    }
                } else if (visitor.status == "APPROVED" && (currentRole == UserRole.GUARD || currentRole == UserRole.ADMIN)) {
                    Button(
                        onClick = onCheckIn,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("checkin_visitor_${visitor.id}")
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gate Check-In")
                    }
                } else if (visitor.status == "INSIDE" && (currentRole == UserRole.GUARD || currentRole == UserRole.ADMIN)) {
                    Button(
                        onClick = onCheckOut,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("checkout_visitor_${visitor.id}")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Exit")
                    }
                } else if (visitor.status == "EXITED") {
                    val exit = visitor.exitTimestamp
                    Text(
                        text = "Exited at ${if (exit != null) formatTimestamp(exit) else "Earlier"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Add Visitor Dialog (for Security Personnel)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitorDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String, phone: String, purpose: String, company: String,
        vehicleNo: String, tower: String, flatNo: String, hostName: String,
        isPreApproved: Boolean, remarks: String
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Guest") }
    var company by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var tower by remember { mutableStateOf("Tower A") }
    var flatNo by remember { mutableStateOf("A-101") }
    var hostName by remember { mutableStateOf("Chandan Kumar") }
    var remarks by remember { mutableStateOf("") }

    val purposeList = listOf("Guest", "Delivery", "Cab / Ride", "Service / Repair", "Daily Help")
    val towerList = listOf("Tower A", "Tower B", "Tower C", "Tower D")
    val flatMap = mapOf(
        "Tower A" to listOf("A-101", "A-102", "A-201"),
        "Tower B" to listOf("B-103", "B-204"),
        "Tower C" to listOf("C-105", "C-302"),
        "Tower D" to listOf("D-202", "D-401")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("log_visitor_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gate Entry Registration",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Visitor Full Name *") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Number *") },
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        Text("Visit Purpose:", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            purposeList.take(3).forEach { p ->
                                FilterChip(
                                    selected = purpose == p,
                                    onClick = { purpose = p },
                                    label = { Text(p, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            purposeList.drop(3).forEach { p ->
                                FilterChip(
                                    selected = purpose == p,
                                    onClick = { purpose = p },
                                    label = { Text(p, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = { Text("Company / Service (e.g. Amazon, Uber)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = vehicleNo,
                            onValueChange = { vehicleNo = it },
                            label = { Text("Vehicle Plate Number (Optional)") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                    item {
                        Text("Select Destination Flat:", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            towerList.forEach { t ->
                                FilterChip(
                                    selected = tower == t,
                                    onClick = {
                                        tower = t
                                        flatNo = flatMap[t]?.firstOrNull() ?: "A-101"
                                    },
                                    label = { Text(t, fontSize = 11.sp) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            flatMap[tower]?.forEach { f ->
                                FilterChip(
                                    selected = flatNo == f,
                                    onClick = { flatNo = f },
                                    label = { Text(f, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Gate Security Remarks") },
                            placeholder = { Text("Package count / ID checked") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onConfirm(name, phone, purpose, company, vehicleNo, tower, flatNo, hostName, false, remarks)
                        }
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_visitor_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register & Alert Flat Resident")
                }
            }
        }
    }
}

// Pre-Invite Guest Dialog (for Residents)
@Composable
fun PreInviteGuestDialog(
    hostFlat: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, phone: String, purpose: String, vehicleNo: String, remarks: String) -> Unit
) {
    var guestName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("Guest") }
    var remarks by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pre_invite_dialog")
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
                        text = "Generate Guest Invite Pass",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Creating pre-approved entry pass for Flat $hostFlat",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                OutlinedTextField(
                    value = guestName,
                    onValueChange = { guestName = it },
                    label = { Text("Guest Name *") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it },
                    label = { Text("Vehicle Number (Optional)") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Special Instructions for Guard") },
                    placeholder = { Text("e.g. Allow basement visitor parking bay #4") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (guestName.isNotBlank()) {
                            onConfirm(guestName, phone, purpose, vehicleNo, remarks)
                        }
                    },
                    enabled = guestName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("generate_invite_pass_button")
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate & Share Gate Pass")
                }
            }
        }
    }
}
