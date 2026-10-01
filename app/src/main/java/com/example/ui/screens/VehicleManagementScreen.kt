package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.ResidentVehicle
import com.example.data.entity.VisitorEntry
import com.example.data.model.VisitorParkingSlot
import com.example.ui.PlateCheckResult
import com.example.ui.UserRole
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleManagementScreen(
    vehicles: List<ResidentVehicle>,
    currentFlatVehicles: List<ResidentVehicle>,
    currentRole: UserRole,
    currentFlat: String,
    residentName: String,
    livePlateCheckResult: PlateCheckResult?,
    visitorParkingSlots: List<VisitorParkingSlot> = emptyList(),
    insideVisitors: List<VisitorEntry> = emptyList(),
    onCheckPlate: (String) -> Unit,
    onClearPlateCheck: () -> Unit,
    onAssignVisitorParking: (visitorId: Long, slotNumber: String) -> Unit = { _, _ -> },
    onReleaseVisitorParking: (visitorId: Long, slotNumber: String) -> Unit = { _, _ -> },
    onNavigateToCheckInWithSlot: (slotNumber: String) -> Unit = {},
    onRegisterVehicle: (
        plate: String,
        type: String,
        makeModel: String,
        color: String,
        tower: String,
        flat: String,
        owner: String,
        phone: String,
        slot: String,
        notes: String
    ) -> Unit,
    onDeleteVehicle: (Long) -> Unit,
    onNavigateToVisitorRegistration: (prefillVehiclePlate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember {
        mutableStateOf(if (currentRole == UserRole.GUARD) "CROSS_REF" else "REGISTRY")
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") }
    var showAddVehicleDialog by remember { mutableStateOf(false) }
    var prefillPlateForAdd by remember { mutableStateOf("") }
    var vehicleToViewDetail by remember { mutableStateOf<ResidentVehicle?>(null) }

    val vehicleTypes = listOf("All", "4-Wheeler", "2-Wheeler", "Electric EV", "Other")

    // Filter vehicles
    val filteredVehicles = remember(vehicles, searchQuery, selectedTypeFilter) {
        vehicles.filter { v ->
            val matchesQuery = searchQuery.isBlank() ||
                v.plateNumber.contains(searchQuery, ignoreCase = true) ||
                v.cleanPlate.contains(searchQuery.replace(Regex("[^A-Za-z0-9]"), ""), ignoreCase = true) ||
                v.ownerName.contains(searchQuery, ignoreCase = true) ||
                v.flatNumber.contains(searchQuery, ignoreCase = true) ||
                v.makeModel.contains(searchQuery, ignoreCase = true) ||
                v.parkingSlot.contains(searchQuery, ignoreCase = true)

            val matchesType = when (selectedTypeFilter) {
                "All" -> true
                "4-Wheeler" -> v.vehicleType.contains("4-Wheeler", ignoreCase = true) || v.vehicleType.contains("Car", ignoreCase = true)
                "2-Wheeler" -> v.vehicleType.contains("2-Wheeler", ignoreCase = true) || v.vehicleType.contains("Bike", ignoreCase = true)
                "Electric EV" -> v.vehicleType.contains("EV", ignoreCase = true) || v.vehicleType.contains("Electric", ignoreCase = true)
                else -> true
            }

            matchesQuery && matchesType
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val freeVisitorBays = visitorParkingSlots.count { !it.isOccupied }
        // Tab Selector Row
        TabRow(
            selectedTabIndex = when (selectedTab) {
                "CROSS_REF" -> 0
                "VISITOR_PARKING" -> 1
                "REGISTRY" -> 2
                else -> 3
            },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == "CROSS_REF",
                onClick = { selectedTab = "CROSS_REF" },
                text = { Text("Gate Cross-Ref", fontSize = 11.sp, fontWeight = if (selectedTab == "CROSS_REF") FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_vehicle_cross_ref")
            )
            Tab(
                selected = selectedTab == "VISITOR_PARKING",
                onClick = { selectedTab = "VISITOR_PARKING" },
                text = { Text("Visitor Parking ($freeVisitorBays)", fontSize = 11.sp, fontWeight = if (selectedTab == "VISITOR_PARKING") FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.LocalParking, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_visitor_parking")
            )
            Tab(
                selected = selectedTab == "REGISTRY",
                onClick = { selectedTab = "REGISTRY" },
                text = { Text("Society (${vehicles.size})", fontSize = 11.sp, fontWeight = if (selectedTab == "REGISTRY") FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_vehicle_registry")
            )
            Tab(
                selected = selectedTab == "MY_FLAT",
                onClick = { selectedTab = "MY_FLAT" },
                text = { Text("My Flat", fontSize = 11.sp, fontWeight = if (selectedTab == "MY_FLAT") FontWeight.Bold else FontWeight.Normal) },
                icon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("tab_vehicle_my_flat")
            )
        }

        when (selectedTab) {
            "CROSS_REF" -> {
                GateCrossReferenceView(
                    liveResult = livePlateCheckResult,
                    onCheckPlate = onCheckPlate,
                    onClearPlate = onClearPlateCheck,
                    onRegisterAsVisitor = onNavigateToVisitorRegistration,
                    onRegisterResidentVehicle = { plate ->
                        prefillPlateForAdd = plate
                        showAddVehicleDialog = true
                    }
                )
            }
            "VISITOR_PARKING" -> {
                VisitorParkingScreen(
                    slots = visitorParkingSlots,
                    insideVisitors = insideVisitors,
                    currentRole = currentRole,
                    onAssignSlot = onAssignVisitorParking,
                    onReleaseSlot = onReleaseVisitorParking,
                    onNavigateToCheckInWithSlot = onNavigateToCheckInWithSlot
                )
            }
            "REGISTRY" -> {
                SocietyVehicleRegistryView(
                    vehicles = filteredVehicles,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedType = selectedTypeFilter,
                    onSelectType = { selectedTypeFilter = it },
                    vehicleTypes = vehicleTypes,
                    onVehicleClick = { vehicleToViewDetail = it },
                    onAddNewVehicle = {
                        prefillPlateForAdd = ""
                        showAddVehicleDialog = true
                    }
                )
            }
            "MY_FLAT" -> {
                MyFlatVehiclesView(
                    currentFlat = currentFlat,
                    residentName = residentName,
                    vehicles = currentFlatVehicles,
                    onVehicleClick = { vehicleToViewDetail = it },
                    onAddNewVehicle = {
                        prefillPlateForAdd = ""
                        showAddVehicleDialog = true
                    },
                    onDeleteVehicle = onDeleteVehicle
                )
            }
        }
    }

    // Add Vehicle Dialog
    if (showAddVehicleDialog) {
        AddResidentVehicleDialog(
            initialPlate = prefillPlateForAdd,
            currentFlat = currentFlat,
            residentName = residentName,
            onDismiss = { showAddVehicleDialog = false },
            onSubmit = { plate, type, makeModel, color, tower, flat, owner, phone, slot, notes ->
                onRegisterVehicle(plate, type, makeModel, color, tower, flat, owner, phone, slot, notes)
                showAddVehicleDialog = false
            }
        )
    }

    // Vehicle Detail Dialog
    vehicleToViewDetail?.let { vehicle ->
        VehicleDetailDialog(
            vehicle = vehicle,
            currentRole = currentRole,
            onDismiss = { vehicleToViewDetail = null },
            onDelete = {
                onDeleteVehicle(vehicle.id)
                vehicleToViewDetail = null
            }
        )
    }
}

/**
 * Gate Personnel Live Plate Cross-Reference Lookup Tool.
 * Provides instant identification of approaching vehicles at the society security barrier.
 */
@Composable
fun GateCrossReferenceView(
    liveResult: PlateCheckResult?,
    onCheckPlate: (String) -> Unit,
    onClearPlate: () -> Unit,
    onRegisterAsVisitor: (String) -> Unit,
    onRegisterResidentVehicle: (String) -> Unit
) {
    var plateInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val sampleQuickTestPlates = listOf(
        "MH 02 CZ 4488" to "Resident (Nexon EV)",
        "MH 04 BK 9911" to "Resident (Creta)",
        "DL 01 AA 9999" to "Resident (MG ZS)",
        "MH 12 ZZ 9999" to "Unknown Visitor"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Gate Vehicle Cross-Reference Terminal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cross-references incoming vehicle plates against authorized resident registry",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Live Plate Input Field
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Enter Vehicle Plate Number",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = plateInput,
                        onValueChange = {
                            plateInput = it
                            if (it.isNotBlank()) {
                                onCheckPlate(it)
                            } else {
                                onClearPlate()
                            }
                        },
                        label = { Text("Vehicle Plate Number (e.g. MH 02 CZ 4488)") },
                        placeholder = { Text("Type plate or tap quick chip below") },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (plateInput.isNotBlank()) {
                                IconButton(onClick = {
                                    plateInput = ""
                                    onClearPlate()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                focusManager.clearFocus()
                                onCheckPlate(plateInput)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("plate_lookup_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick test plate chips
                    Text(
                        text = "Quick Test Plates (Security Sim):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sampleQuickTestPlates) { (plate, desc) ->
                            SuggestionChip(
                                onClick = {
                                    plateInput = plate
                                    onCheckPlate(plate)
                                },
                                label = { Text("$plate ($desc)", fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("test_plate_chip_${plate.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }
        }

        // Live Cross-Reference Result Card
        item {
            if (liveResult != null && liveResult.queryPlate.isNotBlank()) {
                if (liveResult.isMatch && liveResult.vehicle != null) {
                    val v = liveResult.vehicle
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                        border = BorderStroke(2.dp, Color(0xFF22C55E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verified_resident_vehicle_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF16A34A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "AUTHORIZED RESIDENT VEHICLE",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D),
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF16A34A)
                                ) {
                                    Text(
                                        text = "PASS GRANTED",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // License plate display box
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = BorderStroke(1.5.dp, Color(0xFF15803D)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF1E3A8A),
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("IND", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = v.plateNumber,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            letterSpacing = 2.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = v.vehicleType,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Resident Details Grid
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                DetailRow("Resident Owner", "${v.ownerName} (${v.flatNumber}, ${v.tower})")
                                DetailRow("Parking Bay Allotted", v.parkingSlot)
                                DetailRow("Vehicle Make & Color", "${v.makeModel} • ${v.color}")
                                DetailRow("Security Gate Sticker", v.stickerNumber.ifBlank { "Verified" })
                                if (v.ownerPhone.isNotBlank()) {
                                    DetailRow("Owner Contact", v.ownerPhone)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            FilledTonalButton(
                                onClick = {
                                    // Feedback
                                    onClearPlate()
                                    plateInput = ""
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gate_barrier_open_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Acknowledge & Open Boom Barrier")
                            }
                        }
                    }
                } else {
                    // UNREGISTERED / VISITOR VEHICLE WARNING
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(2.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("unregistered_vehicle_alert_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "UNREGISTERED VEHICLE / VISITOR",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFB45309),
                                        fontSize = 13.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = "AUDIT REQUIRED",
                                        color = Color(0xFFB45309),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "Plate \"${liveResult.queryPlate}\" is NOT registered in the society resident database.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Security Protocol: Cross-reference required before gate barrier clearance. Register as a visitor or add to resident registry if newly purchased.",
                                fontSize = 11.sp,
                                color = Color(0xFFB45309)
                            )

                            if (liveResult.matchedVisitorEntry != null) {
                                val vis = liveResult.matchedVisitorEntry
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Previous Visitor Log Found",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "${vis.visitorName} (${vis.visitorType}) to ${vis.tower} - ${vis.flatNumber}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Logged on ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(vis.timestamp))}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRegisterAsVisitor(liveResult.queryPlate) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("register_as_visitor_btn")
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log Visitor Entry", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onRegisterResidentVehicle(liveResult.queryPlate) },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("add_to_registry_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add to Registry", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Awaiting Vehicle Plate Input",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Type any plate number above or tap a test chip to verify instantly against society records.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Society-wide Vehicle Registry View.
 */
@Composable
fun SocietyVehicleRegistryView(
    vehicles: List<ResidentVehicle>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedType: String,
    onSelectType: (String) -> Unit,
    vehicleTypes: List<String>,
    onVehicleClick: (ResidentVehicle) -> Unit,
    onAddNewVehicle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search & Add Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search plate, owner, flat, slot...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("vehicle_registry_search_input")
            )

            FloatingActionButton(
                onClick = onAddNewVehicle,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("add_vehicle_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Register Vehicle")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Type filter chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(vehicleTypes) { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onSelectType(type) },
                    label = { Text(type, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (vehicles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "No vehicles found for \"$searchQuery\"" else "No vehicles registered",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    ResidentVehicleCard(
                        vehicle = vehicle,
                        onClick = { onVehicleClick(vehicle) }
                    )
                }
            }
        }
    }
}

/**
 * Resident's flat vehicles tab.
 */
@Composable
fun MyFlatVehiclesView(
    currentFlat: String,
    residentName: String,
    vehicles: List<ResidentVehicle>,
    onVehicleClick: (ResidentVehicle) -> Unit,
    onAddNewVehicle: () -> Unit,
    onDeleteVehicle: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Registered Flat: $currentFlat",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "$residentName • ${vehicles.size} vehicles active",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = onAddNewVehicle,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_flat_vehicle_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Vehicle", fontSize = 12.sp)
                    }
                }
            }
        }

        if (vehicles.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Vehicles Registered for $currentFlat",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Register your car or two-wheeler plate number for automated gate entry & parking slot access.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onAddNewVehicle) {
                            Text("Register Vehicle Now")
                        }
                    }
                }
            }
        } else {
            items(vehicles, key = { it.id }) { vehicle ->
                ResidentVehicleCard(
                    vehicle = vehicle,
                    onClick = { onVehicleClick(vehicle) }
                )
            }
        }
    }
}

@Composable
fun ResidentVehicleCard(
    vehicle: ResidentVehicle,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("vehicle_card_${vehicle.cleanPlate}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // License Plate Tag
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = vehicle.plateNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                }

                // Parking Slot Badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalParking,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = vehicle.parkingSlot,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = vehicle.makeModel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${vehicle.vehicleType} • ${vehicle.color}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${vehicle.flatNumber} (${vehicle.tower})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = vehicle.ownerName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddResidentVehicleDialog(
    initialPlate: String = "",
    currentFlat: String,
    residentName: String,
    onDismiss: () -> Unit,
    onSubmit: (
        plate: String,
        type: String,
        makeModel: String,
        color: String,
        tower: String,
        flat: String,
        owner: String,
        phone: String,
        slot: String,
        notes: String
    ) -> Unit
) {
    var plate by remember { mutableStateOf(initialPlate) }
    var vehicleType by remember { mutableStateOf("4-Wheeler Car") }
    var makeModel by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("White") }
    var tower by remember { mutableStateOf(if (currentFlat.startsWith("B")) "Tower B" else "Tower A") }
    var flatNumber by remember { mutableStateOf(currentFlat) }
    var ownerName by remember { mutableStateOf(residentName) }
    var ownerPhone by remember { mutableStateOf("+91 98201 12345") }
    var parkingSlot by remember { mutableStateOf("B1-P12") }
    var notes by remember { mutableStateOf("") }

    var plateError by remember { mutableStateOf<String?>(null) }
    var makeError by remember { mutableStateOf<String?>(null) }

    val vehicleTypes = listOf("4-Wheeler Car", "2-Wheeler Bike", "Electric EV Car", "Electric EV Scooter", "Commercial")
    val towers = listOf("Tower A", "Tower B", "Tower C", "Tower D")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("add_vehicle_dialog")
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
                            text = "Register Resident Vehicle",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Links plate to gate security & parking registry",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = plate,
                            onValueChange = {
                                plate = it
                                plateError = null
                            },
                            label = { Text("Plate Number *") },
                            placeholder = { Text("e.g. MH 02 CZ 4488") },
                            isError = plateError != null,
                            supportingText = plateError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vehicle_plate_input")
                        )
                    }

                    item {
                        Text("Vehicle Type", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(vehicleTypes) { type ->
                                FilterChip(
                                    selected = vehicleType == type,
                                    onClick = { vehicleType = type },
                                    label = { Text(type, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = makeModel,
                            onValueChange = {
                                makeModel = it
                                makeError = null
                            },
                            label = { Text("Make & Model *") },
                            placeholder = { Text("e.g. Tata Nexon EV, Honda City") },
                            isError = makeError != null,
                            supportingText = makeError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("vehicle_make_model_input")
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = color,
                                onValueChange = { color = it },
                                label = { Text("Color") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = parkingSlot,
                                onValueChange = { parkingSlot = it },
                                label = { Text("Parking Bay") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = tower,
                                onValueChange = { tower = it },
                                label = { Text("Tower") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = flatNumber,
                                onValueChange = { flatNumber = it },
                                label = { Text("Flat") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Name") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = ownerPhone,
                            onValueChange = { ownerPhone = it },
                            label = { Text("Contact Phone") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            var valid = true
                            if (plate.isBlank()) {
                                plateError = "Plate number is required"
                                valid = false
                            }
                            if (makeModel.isBlank()) {
                                makeError = "Make and model is required"
                                valid = false
                            }
                            if (valid) {
                                onSubmit(
                                    plate.trim(),
                                    vehicleType,
                                    makeModel.trim(),
                                    color.trim(),
                                    tower,
                                    flatNumber,
                                    ownerName.trim(),
                                    ownerPhone.trim(),
                                    parkingSlot.trim(),
                                    notes.trim()
                                )
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_vehicle_btn")
                    ) {
                        Text("Save Vehicle")
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleDetailDialog(
    vehicle: ResidentVehicle,
    currentRole: UserRole,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("vehicle_detail_dialog")
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
                        text = "Vehicle Security Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Plate Header Card
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = vehicle.plateNumber,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${vehicle.vehicleType} • ${vehicle.makeModel}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                DetailRow("Resident Owner", vehicle.ownerName)
                DetailRow("Apartment / Flat", "${vehicle.tower} - ${vehicle.flatNumber}")
                DetailRow("Allotted Parking Bay", vehicle.parkingSlot)
                DetailRow("Security Sticker", vehicle.stickerNumber.ifBlank { "Verified" })
                DetailRow("Color", vehicle.color)
                if (vehicle.ownerPhone.isNotBlank()) {
                    DetailRow("Owner Phone", vehicle.ownerPhone)
                }
                DetailRow(
                    "Registered Date",
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(vehicle.registeredAt))
                )

                Spacer(modifier = Modifier.height(16.dp))

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

                    if (currentRole == UserRole.ADMIN || currentRole == UserRole.GUARD) {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("delete_vehicle_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}
