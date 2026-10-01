package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ResidentVehicle
import com.example.data.model.VisitorParkingSlot
import com.example.ui.components.StatusBadge
import com.example.ui.components.VisitorPhotoCaptureBox

data class VisitorTypeOption(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val defaultCompany: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorRegistrationScreen(
    currentFlat: String,
    prefillVehiclePlate: String = "",
    registeredVehicles: List<ResidentVehicle> = emptyList(),
    allParkingSlots: List<VisitorParkingSlot> = emptyList(),
    prefillParkingSlot: String = "",
    onBack: () -> Unit,
    onSubmit: (
        name: String,
        phone: String,
        type: String,
        company: String,
        vehicleNo: String,
        tower: String,
        flatNo: String,
        hostName: String,
        isPreApproved: Boolean,
        remarks: String,
        photoUri: String?,
        parkingSlot: String
    ) -> Unit
) {
    BackHandler { onBack() }
    val focusManager = LocalFocusManager.current

    // Form states
    var visitorName by remember { mutableStateOf("") }
    var visitorPhone by remember { mutableStateOf("") }
    var visitorType by remember { mutableStateOf("Guest") }
    var companyName by remember { mutableStateOf("") }
    var vehicleNumber by remember { mutableStateOf(prefillVehiclePlate) }
    var selectedParkingSlot by remember { mutableStateOf(prefillParkingSlot) }
    var isParkingEnabled by remember { mutableStateOf(prefillParkingSlot.isNotBlank() || prefillVehiclePlate.isNotBlank()) }
    var selectedParkingZoneFilter by remember { mutableStateOf("All") }
    var visitorPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedTower by remember {
        mutableStateOf(
            if (currentFlat.startsWith("B")) "Tower B"
            else if (currentFlat.startsWith("C")) "Tower C"
            else if (currentFlat.startsWith("D")) "Tower D"
            else "Tower A"
        )
    }
    var selectedFlat by remember { mutableStateOf(currentFlat) }
    var hostResidentName by remember { mutableStateOf("Host Resident") }
    var isPreApproved by remember { mutableStateOf(false) }
    var remarks by remember { mutableStateOf("") }

    // Validation errors
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val visitorTypeOptions = remember {
        listOf(
            VisitorTypeOption("Guest", Icons.Default.Person, "Family, friends & social guests"),
            VisitorTypeOption("Delivery", Icons.Default.LocalShipping, "E-commerce, food & parcel deliveries", "Amazon"),
            VisitorTypeOption("Cab / Ride", Icons.Default.DirectionsCar, "Taxi & ride-hailing pickups", "Uber"),
            VisitorTypeOption("Service / Repair", Icons.Default.Handyman, "Technicians, electrical, plumbing", "Urban Company"),
            VisitorTypeOption("Daily Help", Icons.Default.CleaningServices, "Housekeeping, cook, maid staff"),
            VisitorTypeOption("Other", Icons.Default.MoreHoriz, "Official or miscellaneous visitors")
        )
    }

    val towers = listOf("Tower A", "Tower B", "Tower C", "Tower D")
    val flatsByTower = mapOf(
        "Tower A" to listOf("A-101", "A-102", "A-201"),
        "Tower B" to listOf("B-103", "B-204"),
        "Tower C" to listOf("C-105", "C-302"),
        "Tower D" to listOf("D-202", "D-401")
    )

    fun validate(): Boolean {
        var isValid = true
        if (visitorName.trim().isBlank()) {
            nameError = "Visitor name is required"
            isValid = false
        } else {
            nameError = null
        }

        val cleanedPhone = visitorPhone.filter { it.isDigit() }
        if (cleanedPhone.isBlank()) {
            phoneError = "Phone number is required"
            isValid = false
        } else if (cleanedPhone.length < 7) {
            phoneError = "Enter a valid phone number (at least 7 digits)"
            isValid = false
        } else {
            phoneError = null
        }
        return isValid
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "New Visitor Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record entry log to database",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("visitor_form_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp)
        ) {
            // Quick preset chips for rapid security logging
            item {
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        SuggestionChip(
                            onClick = {
                                visitorType = "Delivery"
                                companyName = "Amazon"
                                if (remarks.isBlank()) remarks = "Parcel delivery"
                            },
                            label = { Text("📦 Amazon Delivery", fontSize = 12.sp) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                visitorType = "Delivery"
                                companyName = "Swiggy / Zomato"
                                if (remarks.isBlank()) remarks = "Food delivery"
                            },
                            label = { Text("🍔 Food Courier", fontSize = 12.sp) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                visitorType = "Cab / Ride"
                                companyName = "Uber / Ola"
                                if (remarks.isBlank()) remarks = "Pickup at tower lobby"
                            },
                            label = { Text("🚖 Cab Pick-up", fontSize = 12.sp) }
                        )
                    }
                    item {
                        SuggestionChip(
                            onClick = {
                                visitorType = "Guest"
                                isPreApproved = true
                                if (remarks.isBlank()) remarks = "Pre-approved family guest"
                            },
                            label = { Text("✨ Invited Guest", fontSize = 12.sp) }
                        )
                    }
                }
            }

            // Visitor Information Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Visitor Personal Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Visitor Name field
                        OutlinedTextField(
                            value = visitorName,
                            onValueChange = {
                                visitorName = it
                                if (hasAttemptedSubmit) validate()
                            },
                            label = { Text("Visitor Full Name *") },
                            placeholder = { Text("e.g. Ramesh Patel") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            isError = nameError != null,
                            supportingText = {
                                if (nameError != null) {
                                    Text(nameError!!, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("visitor_name_input")
                        )

                        // Visitor Phone Number field
                        OutlinedTextField(
                            value = visitorPhone,
                            onValueChange = {
                                visitorPhone = it
                                if (hasAttemptedSubmit) validate()
                            },
                            label = { Text("Visitor Phone Number *") },
                            placeholder = { Text("e.g. +91 98765 43210") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            isError = phoneError != null,
                            supportingText = {
                                if (phoneError != null) {
                                    Text(phoneError!!, color = MaterialTheme.colorScheme.error)
                                } else {
                                    Text("Used for gate security SMS/OTP verification")
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("visitor_phone_input")
                        )
                    }
                }
            }

            // Visitor Identification Photo (Camera Capture)
            item {
                VisitorPhotoCaptureBox(
                    currentPhotoPath = visitorPhotoPath,
                    onPhotoCaptured = { visitorPhotoPath = it }
                )
            }

            // Visitor Type Selection Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Select Visitor Type *",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Categorize the visitor purpose for security logging",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        visitorTypeOptions.forEach { option ->
                            val isSelected = visitorType == option.name
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        visitorType = option.name
                                        if (option.defaultCompany.isNotBlank() && companyName.isBlank()) {
                                            companyName = option.defaultCompany
                                        }
                                    }
                                    .testTag("visitor_type_option_${option.name.replace(" ", "_")}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            visitorType = option.name
                                            if (option.defaultCompany.isNotBlank() && companyName.isBlank()) {
                                                companyName = option.defaultCompany
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            option.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                        Text(
                                            text = option.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Destination Flat & Security Details Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Destination & Gate Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        // Tower selector
                        Column {
                            Text("Destination Tower:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                towers.forEach { tower ->
                                    val isSelected = selectedTower == tower
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTower = tower
                                            selectedFlat = flatsByTower[tower]?.firstOrNull() ?: "A-101"
                                        },
                                        label = { Text(tower, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // Flat selector
                        Column {
                            Text("Flat Number:", style = MaterialTheme.typography.labelMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                flatsByTower[selectedTower]?.forEach { flat ->
                                    val isSelected = selectedFlat == flat
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedFlat = flat },
                                        label = { Text(flat, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        // Company / Service
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company / Delivery Brand") },
                            placeholder = { Text("e.g. Amazon, Swiggy, Uber, Urban Company") },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Vehicle Number
                        OutlinedTextField(
                            value = vehicleNumber,
                            onValueChange = { vehicleNumber = it },
                            label = { Text("Vehicle Registration Plate (Optional)") },
                            placeholder = { Text("e.g. KA-03-AB-4021 or Walking") },
                            leadingIcon = {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Live Plate Cross-Reference with Resident Registry
                        val matchedVehicle = remember(vehicleNumber, registeredVehicles) {
                            val clean = ResidentVehicle.normalizePlate(vehicleNumber)
                            if (clean.length >= 4) {
                                registeredVehicles.firstOrNull { it.cleanPlate == clean }
                            } else null
                        }

                        if (matchedVehicle != null) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("vehicle_cross_ref_matched_badge")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "Matched Resident Vehicle: ${matchedVehicle.ownerName}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF92400E)
                                        )
                                        Text(
                                            text = "${matchedVehicle.flatNumber} (${matchedVehicle.tower}) • ${matchedVehicle.makeModel} • Slot: ${matchedVehicle.parkingSlot}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        } else if (vehicleNumber.isNotBlank() && vehicleNumber.length >= 4) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Unregistered Vehicle • Logging as Guest / Delivery entry",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Gate Remarks
                        OutlinedTextField(
                            value = remarks,
                            onValueChange = { remarks = it },
                            label = { Text("Gate Security Remarks / Notes") },
                            placeholder = { Text("e.g. ID verified, parcel bag checked") },
                            leadingIcon = {
                                Icon(Icons.Default.Notes, contentDescription = null)
                            },
                            singleLine = false,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Visitor Parking Allocation
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isParkingEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(
                                1.dp,
                                if (isParkingEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("visitor_parking_section")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocalParking,
                                            contentDescription = null,
                                            tint = if (isParkingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Assign Visitor Parking Bay",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Designates a reserved parking bay for the visitor",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = isParkingEnabled,
                                        onCheckedChange = {
                                            isParkingEnabled = it
                                            if (!it) selectedParkingSlot = ""
                                        },
                                        modifier = Modifier.testTag("toggle_visitor_parking")
                                    )
                                }

                                if (isParkingEnabled) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val parkingSlotsToDisplay = remember(allParkingSlots, selectedParkingZoneFilter) {
                                        val source = if (allParkingSlots.isNotEmpty()) allParkingSlots else VisitorParkingSlot.ALL_DEFAULT_SLOTS
                                        if (selectedParkingZoneFilter == "All") source
                                        else source.filter { it.zone.contains(selectedParkingZoneFilter, ignoreCase = true) }
                                    }

                                    // Filter chips for zones
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("All", "Surface", "Basement", "EV", "Two-Wheeler").forEach { z ->
                                            FilterChip(
                                                selected = selectedParkingZoneFilter == z,
                                                onClick = { selectedParkingZoneFilter = z },
                                                label = { Text(z, fontSize = 10.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Select Available Parking Bay:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(parkingSlotsToDisplay) { slot ->
                                            val isSelected = selectedParkingSlot == slot.slotNumber
                                            val isOccupied = slot.isOccupied

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = when {
                                                    isSelected -> Color(0xFFDCFCE7)
                                                    isOccupied -> Color(0xFFFEE2E2)
                                                    else -> Color.White
                                                },
                                                border = BorderStroke(
                                                    if (isSelected) 2.dp else 1.dp,
                                                    when {
                                                        isSelected -> Color(0xFF16A34A)
                                                        isOccupied -> Color(0xFFEF4444)
                                                        else -> Color(0xFFCBD5E1)
                                                    }
                                                ),
                                                modifier = Modifier
                                                    .clickable(enabled = !isOccupied) {
                                                        selectedParkingSlot = if (isSelected) "" else slot.slotNumber
                                                    }
                                                    .testTag("slot_chip_${slot.slotNumber}")
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = slot.slotNumber,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 12.sp,
                                                            color = if (isOccupied) Color(0xFFDC2626) else if (isSelected) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (isSelected) {
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = "Selected",
                                                                tint = Color(0xFF16A34A),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = if (isOccupied) "Occupied" else "Available",
                                                        fontSize = 9.sp,
                                                        color = if (isOccupied) Color(0xFF991B1B) else Color(0xFF166534),
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (selectedParkingSlot.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFF0FDF4),
                                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFF16A34A),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Assigned Bay: $selectedParkingSlot",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF15803D)
                                                    )
                                                }
                                                TextButton(
                                                    onClick = { selectedParkingSlot = "" },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Clear", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Pre-approval toggle
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Pre-Approve Entry",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Mark as pre-verified without waiting for resident app approval",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isPreApproved,
                                    onCheckedChange = { isPreApproved = it }
                                )
                            }
                        }
                    }
                }
            }

            // Live Entry Log Preview Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Preview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Entry Log Preview",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            StatusBadge(if (isPreApproved) "APPROVED" else "WAITING_APPROVAL")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (visitorName.isNotBlank()) visitorName else "Visitor Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${visitorType} • ${if (visitorPhone.isNotBlank()) visitorPhone else "Phone: Pending"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Destination: $selectedTower ($selectedFlat) • Vehicle: ${vehicleNumber.ifBlank { "Walking / None" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isParkingEnabled && selectedParkingSlot.isNotBlank()) {
                            Text(
                                text = "🅿️ Assigned Visitor Bay: $selectedParkingSlot",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (validate()) {
                            onSubmit(
                                visitorName.trim(),
                                visitorPhone.trim(),
                                visitorType,
                                companyName.trim(),
                                vehicleNumber.trim().ifBlank { "None" },
                                selectedTower,
                                selectedFlat,
                                hostResidentName,
                                isPreApproved,
                                remarks.trim(),
                                visitorPhotoPath,
                                if (isParkingEnabled) selectedParkingSlot.trim() else ""
                            )
                            onBack()
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_visitor_form_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAVE ENTRY LOG TO DATABASE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
