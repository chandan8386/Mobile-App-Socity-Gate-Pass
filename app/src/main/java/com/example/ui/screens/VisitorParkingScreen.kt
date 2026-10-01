package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.VisitorEntry
import com.example.data.model.VisitorParkingSlot
import com.example.ui.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorParkingScreen(
    slots: List<VisitorParkingSlot>,
    insideVisitors: List<VisitorEntry>,
    currentRole: UserRole,
    onAssignSlot: (visitorId: Long, slotNumber: String) -> Unit,
    onReleaseSlot: (visitorId: Long, slotNumber: String) -> Unit,
    onNavigateToCheckInWithSlot: (slotNumber: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedZone by remember { mutableStateOf("All") }
    var selectedSlotForAction by remember { mutableStateOf<VisitorParkingSlot?>(null) }
    var showAssignDialogForSlot by remember { mutableStateOf<VisitorParkingSlot?>(null) }
    var showSlotDetailsDialog by remember { mutableStateOf<VisitorParkingSlot?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val zones = listOf("All", "Surface Clubhouse", "Basement B1", "EV Station", "Two-Wheeler")

    val totalCount = slots.size
    val occupiedCount = slots.count { it.isOccupied }
    val availableCount = totalCount - occupiedCount
    val occupancyRate = if (totalCount > 0) occupiedCount.toFloat() / totalCount else 0f

    val filteredSlots = remember(slots, selectedZone, searchQuery) {
        slots.filter { slot ->
            val matchesZone = selectedZone == "All" || slot.zone.equals(selectedZone, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                slot.slotNumber.contains(searchQuery, ignoreCase = true) ||
                (slot.visitorName?.contains(searchQuery, ignoreCase = true) == true) ||
                (slot.vehicleNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                (slot.flatNumber?.contains(searchQuery, ignoreCase = true) == true)
            matchesZone && matchesSearch
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Sticky Header with Occupancy Metrics
        Card(
            shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Visitor Parking Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Live bay assignment & vacancy monitoring",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (occupancyRate > 0.85f) Color(0xFFFEE2E2)
                        else if (occupancyRate > 0.6f) Color(0xFFFEF3C7)
                        else Color(0xFFDCFCE7),
                        border = BorderStroke(
                            1.dp,
                            if (occupancyRate > 0.85f) Color(0xFFEF4444)
                            else if (occupancyRate > 0.6f) Color(0xFFF59E0B)
                            else Color(0xFF22C55E)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (occupancyRate > 0.85f) Color(0xFFDC2626)
                                        else if (occupancyRate > 0.6f) Color(0xFFD97706)
                                        else Color(0xFF16A34A)
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (occupancyRate > 0.85f) "PARKING CRITICAL"
                                else if (occupancyRate > 0.6f) "MODERATE"
                                else "BAY CAPACITY OK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (occupancyRate > 0.85f) Color(0xFF991B1B)
                                else if (occupancyRate > 0.6f) Color(0xFF92400E)
                                else Color(0xFF166534)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OccupancyStatBox(
                        title = "Available",
                        count = "$availableCount",
                        sub = "Free Bays",
                        color = Color(0xFF16A34A),
                        bgColor = Color(0xFFF0FDF4),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_available_parking")
                    )
                    OccupancyStatBox(
                        title = "Occupied",
                        count = "$occupiedCount",
                        sub = "Vehicles Parked",
                        color = Color(0xFFDC2626),
                        bgColor = Color(0xFFFEF2F2),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_occupied_parking")
                    )
                    OccupancyStatBox(
                        title = "Capacity",
                        count = "$totalCount",
                        sub = "Total Guest Bays",
                        color = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stat_total_parking")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Live Occupancy Rate", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "${(occupancyRate * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (occupancyRate > 0.85f) Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { occupancyRate },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (occupancyRate > 0.85f) Color(0xFFEF4444)
                        else if (occupancyRate > 0.6f) Color(0xFFF59E0B)
                        else Color(0xFF22C55E),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Zone Filters & Search Bar
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by slot (e.g. V-01), visitor, flat, plate...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parking_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(zones) { zone ->
                    val isSelected = selectedZone == zone
                    val countForZone = if (zone == "All") slots.size else slots.count { it.zone.equals(zone, ignoreCase = true) }
                    val freeInZone = if (zone == "All") availableCount else slots.count { it.zone.equals(zone, ignoreCase = true) && !it.isOccupied }

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedZone = zone },
                        label = {
                            Text(
                                text = "$zone ($freeInZone free)",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Visual Parking Bay Floor Plan Grid
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Visitor Bays • Visual Indicator",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem("Available", Color(0xFF16A34A))
                        LegendItem("Occupied", Color(0xFFDC2626))
                    }
                }
            }

            // Grid of Parking Slots (2 columns for high readability)
            items(filteredSlots.chunked(2)) { rowSlots ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (slot in rowSlots) {
                        Box(modifier = Modifier.weight(1f)) {
                            VisitorParkingBayCard(
                                slot = slot,
                                onClick = {
                                    if (slot.isOccupied) {
                                        showSlotDetailsDialog = slot
                                    } else {
                                        showAssignDialogForSlot = slot
                                    }
                                }
                            )
                        }
                    }
                    if (rowSlots.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Modal: Quick Assign Bay to Visitor
    showAssignDialogForSlot?.let { slot ->
        AssignParkingSlotDialog(
            slot = slot,
            insideVisitors = insideVisitors,
            onDismiss = { showAssignDialogForSlot = null },
            onAssignToVisitor = { visitorId ->
                onAssignSlot(visitorId, slot.slotNumber)
                showAssignDialogForSlot = null
            },
            onRegisterNewWithSlot = {
                showAssignDialogForSlot = null
                onNavigateToCheckInWithSlot(slot.slotNumber)
            }
        )
    }

    // Modal: Occupied Slot Details & Release Action
    showSlotDetailsDialog?.let { slot ->
        OccupiedSlotDetailsDialog(
            slot = slot,
            currentRole = currentRole,
            onDismiss = { showSlotDetailsDialog = null },
            onRelease = {
                slot.visitorId?.let { vId ->
                    onReleaseSlot(vId, slot.slotNumber)
                }
                showSlotDetailsDialog = null
            }
        )
    }
}

/**
 * Visual representation of an individual parking bay.
 * Mimics real parking bay striping with high-contrast indicator for occupied vs available.
 */
@Composable
fun VisitorParkingBayCard(
    slot: VisitorParkingSlot,
    onClick: () -> Unit
) {
    val isOccupied = slot.isOccupied
    val borderColor = if (isOccupied) Color(0xFFEF4444) else Color(0xFF22C55E)
    val containerBg = if (isOccupied) Color(0xFFFEF2F2) else Color(0xFFF0FDF4)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerBg),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("parking_bay_${slot.slotNumber}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Bay Number & Indicator Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = slot.slotNumber,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (isOccupied) Color(0xFFB91C1C) else Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isOccupied) Color(0xFFDC2626) else Color(0xFF16A34A)
                ) {
                    Text(
                        text = if (isOccupied) "OCCUPIED" else "FREE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vehicle & Zone Icon
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = when {
                        slot.slotType.contains("EV", ignoreCase = true) -> Icons.Default.ElectricCar
                        slot.slotType.contains("2-Wheeler", ignoreCase = true) -> Icons.Default.TwoWheeler
                        else -> Icons.Default.DirectionsCar
                    },
                    contentDescription = null,
                    tint = if (isOccupied) Color(0xFFDC2626) else Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = slot.zone,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (isOccupied) {
                // Occupied details
                Text(
                    text = slot.vehicleNumber?.ifBlank { "Vehicle Attached" } ?: "Vehicle Attached",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF991B1B)
                )
                Text(
                    text = "${slot.visitorName ?: "Visitor"} • ${slot.flatNumber ?: "Flat"}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    text = "Tap to Assign",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF15803D)
                )
                Text(
                    text = slot.slotType,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OccupancyStatBox(
    title: String,
    count: String,
    sub: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = color, fontWeight = FontWeight.SemiBold)
            Text(count, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(sub, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * Dialog to assign a free parking slot to an active inside visitor or link to new registration.
 */
@Composable
fun AssignParkingSlotDialog(
    slot: VisitorParkingSlot,
    insideVisitors: List<VisitorEntry>,
    onDismiss: () -> Unit,
    onAssignToVisitor: (visitorId: Long) -> Unit,
    onRegisterNewWithSlot: () -> Unit
) {
    val unallocatedVisitors = remember(insideVisitors) {
        insideVisitors.filter { it.status == "INSIDE" && it.parkingSlot.isBlank() }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assign_parking_dialog")
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
                    Column {
                        Text(
                            text = "Assign Parking Bay ${slot.slotNumber}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${slot.zone} • ${slot.slotType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                Text(
                    text = "Select active visitor currently inside gate:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (unallocatedVisitors.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "All active inside visitors currently have assigned bays.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = onRegisterNewWithSlot,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_register_with_slot")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check-In New Visitor for ${slot.slotNumber}")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(unallocatedVisitors) { visitor ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAssignToVisitor(visitor.id) }
                                    .testTag("assign_visitor_item_${visitor.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = visitor.visitorName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${visitor.visitorType} • Flat ${visitor.flatNumber}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (visitor.vehicleNumber.isNotBlank() && visitor.vehicleNumber != "None") {
                                            Text(
                                                text = "Vehicle: ${visitor.vehicleNumber}",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = { onAssignToVisitor(visitor.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Assign", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * Dialog to inspect an occupied parking slot and release/free it.
 */
@Composable
fun OccupiedSlotDetailsDialog(
    slot: VisitorParkingSlot,
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onRelease: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("occupied_slot_details_dialog")
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text(
                                text = slot.slotNumber,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Occupied Visitor Bay",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${slot.zone} • ${slot.slotType}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Parking details list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParkingDetailItem("Parked Vehicle Plate", slot.vehicleNumber ?: "N/A", isMonospace = true)
                    ParkingDetailItem("Visitor Name", slot.visitorName ?: "Unknown Visitor")
                    ParkingDetailItem("Visiting Apartment", "${slot.flatNumber ?: "N/A"} (${slot.tower ?: "Tower"})")
                    slot.occupiedSince?.let { timestamp ->
                        ParkingDetailItem(
                            "Entry Logged At",
                            SimpleDateFormat("hh:mm a, dd MMM", Locale.getDefault()).format(Date(timestamp))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = onRelease,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_release_parking_slot")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Release Bay")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParkingDetailItem(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
