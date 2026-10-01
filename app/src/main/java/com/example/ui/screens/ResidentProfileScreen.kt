package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ResidentProfile
import com.example.ui.components.formatTimestamp

@Composable
fun ResidentProfileScreen(
    currentFlat: String,
    profile: ResidentProfile?,
    allProfiles: List<ResidentProfile>,
    isProcessing: Boolean,
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onSaveProfile: (ResidentProfile) -> Unit,
    onSendTestEmergencyBroadcast: (flatNumber: String) -> Unit,
    onSwitchFlat: (String, String) -> Unit
) {
    // Editable form state initialized from Room entity (or default if null)
    var fullName by remember(profile, currentFlat) {
        mutableStateOf(profile?.fullName ?: "Resident of $currentFlat")
    }
    var tower by remember(profile, currentFlat) {
        mutableStateOf(profile?.tower ?: if (currentFlat.startsWith("A")) "Tower A" else if (currentFlat.startsWith("B")) "Tower B" else if (currentFlat.startsWith("C")) "Tower C" else "Tower D")
    }
    var flatNumber by remember(profile, currentFlat) {
        mutableStateOf(profile?.flatNumber ?: currentFlat)
    }
    var primaryPhone by remember(profile, currentFlat) {
        mutableStateOf(profile?.primaryPhone ?: "+91 98201 12345")
    }
    var alternatePhone by remember(profile, currentFlat) {
        mutableStateOf(profile?.alternatePhone ?: "")
    }
    var email by remember(profile, currentFlat) {
        mutableStateOf(profile?.email ?: "resident.$currentFlat@societygate.com")
    }
    var ownershipType by remember(profile, currentFlat) {
        mutableStateOf(profile?.ownershipType ?: "OWNER")
    }
    var moveInDate by remember(profile, currentFlat) {
        mutableStateOf(profile?.moveInDate ?: "Jan 2023")
    }
    var intercomExtension by remember(profile, currentFlat) {
        mutableStateOf(profile?.intercomExtension ?: currentFlat.replace(Regex("[^0-9]"), ""))
    }
    var parkingSlot by remember(profile, currentFlat) {
        mutableStateOf(profile?.parkingSlot ?: "B1-P12")
    }
    var residentCount by remember(profile, currentFlat) {
        mutableIntStateOf(profile?.residentCount ?: 3)
    }
    var bloodGroup by remember(profile, currentFlat) {
        mutableStateOf(profile?.bloodGroup ?: "O+")
    }
    var emergencyContactName by remember(profile, currentFlat) {
        mutableStateOf(profile?.emergencyContactName ?: "")
    }
    var emergencyContactPhone by remember(profile, currentFlat) {
        mutableStateOf(profile?.emergencyContactPhone ?: "")
    }
    var emergencyContactRelation by remember(profile, currentFlat) {
        mutableStateOf(profile?.emergencyContactRelation ?: "Family")
    }
    var enableSmsAlerts by remember(profile, currentFlat) {
        mutableStateOf(profile?.enableSmsAlerts ?: true)
    }
    var enablePushAlerts by remember(profile, currentFlat) {
        mutableStateOf(profile?.enablePushAlerts ?: true)
    }
    var enableVisitorArrivalAlerts by remember(profile, currentFlat) {
        mutableStateOf(profile?.enableVisitorArrivalAlerts ?: true)
    }
    var enableEmergencyBroadcasts by remember(profile, currentFlat) {
        mutableStateOf(profile?.enableEmergencyBroadcasts ?: true)
    }
    var specialNotes by remember(profile, currentFlat) {
        mutableStateOf(profile?.specialNotes ?: "")
    }

    var selectedTab by remember { mutableStateOf("CONTACT") } // "CONTACT", "APARTMENT", "EMERGENCY", "ALERTS"

    val bloodGroups = listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-")
    val towers = listOf("Tower A", "Tower B", "Tower C", "Tower D")
    val relations = listOf("Spouse", "Parent", "Child", "Sibling", "Friend", "Guardian", "Relative")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("resident_profile_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
    ) {
        // 1. Resident Identity Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = fullName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$tower • Flat $flatNumber • $ownershipType",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFDCFCE7),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ROOM SYNCED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("INTERCOM EXT", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text("EXT: $intercomExtension", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("PARKING BAY", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(parkingSlot.ifBlank { "Unassigned" }, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("BLOOD GROUP", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                Text(bloodGroup, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    if (profile?.updatedAt != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Last persisted: ${formatTimestamp(profile.updatedAt)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Quick Switch Flat Row (For testing multiple resident apartments)
        item {
            Column {
                Text(
                    text = "Switch Apartment Preview:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sampleFlats = listOf(
                        Triple("A-101", "Chandan Kumar", "Tower A"),
                        Triple("B-204", "Priya Sharma", "Tower B"),
                        Triple("C-302", "Dr. Ananya Roy", "Tower C"),
                        Triple("D-401", "Vikramaditya Rao", "Tower D")
                    )
                    items(sampleFlats) { (flat, name, _) ->
                        FilterChip(
                            selected = currentFlat == flat,
                            onClick = { onSwitchFlat(flat, name) },
                            label = { Text("Flat $flat ($name)", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("switch_preview_flat_$flat")
                        )
                    }
                }
            }
        }

        // 2. Section Navigation Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = when (selectedTab) {
                    "CONTACT" -> 0
                    "APARTMENT" -> 1
                    "EMERGENCY" -> 2
                    else -> 3
                },
                edgePadding = 0.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == "CONTACT",
                    onClick = { selectedTab = "CONTACT" },
                    text = { Text("Contact Info", fontSize = 12.sp, fontWeight = if (selectedTab == "CONTACT") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_profile_contact")
                )
                Tab(
                    selected = selectedTab == "APARTMENT",
                    onClick = { selectedTab = "APARTMENT" },
                    text = { Text("Apartment", fontSize = 12.sp, fontWeight = if (selectedTab == "APARTMENT") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_profile_apartment")
                )
                Tab(
                    selected = selectedTab == "EMERGENCY",
                    onClick = { selectedTab = "EMERGENCY" },
                    text = { Text("Emergency SOS", fontSize = 12.sp, fontWeight = if (selectedTab == "EMERGENCY") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_profile_emergency")
                )
                Tab(
                    selected = selectedTab == "ALERTS",
                    onClick = { selectedTab = "ALERTS" },
                    text = { Text("Alert Settings", fontSize = 12.sp, fontWeight = if (selectedTab == "ALERTS") FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("tab_profile_alerts")
                )
            }
        }

        // 3. Tab Contents
        when (selectedTab) {
            "CONTACT" -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Resident Contact Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_name")
                            )

                            OutlinedTextField(
                                value = primaryPhone,
                                onValueChange = { primaryPhone = it },
                                label = { Text("Primary Mobile Phone * (For SMS Gate Alerts)") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_phone")
                            )

                            OutlinedTextField(
                                value = alternatePhone,
                                onValueChange = { alternatePhone = it },
                                label = { Text("Alternate / Landline Phone (Optional)") },
                                leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_alt_phone")
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address (For Invoices & Notices)") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_email")
                            )

                            Text(
                                text = "Primary Resident Blood Group:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(bloodGroups) { bg ->
                                    FilterChip(
                                        selected = bloodGroup == bg,
                                        onClick = { bloodGroup = bg },
                                        label = { Text(bg, fontSize = 11.sp, fontWeight = if (bloodGroup == bg) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("blood_group_chip_$bg")
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "APARTMENT" -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Apartment & Tower Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = flatNumber,
                                    onValueChange = { flatNumber = it.trim().uppercase() },
                                    label = { Text("Flat Number *") },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_profile_flat")
                                )

                                OutlinedTextField(
                                    value = intercomExtension,
                                    onValueChange = { intercomExtension = it.trim() },
                                    label = { Text("Intercom EXT") },
                                    leadingIcon = { Icon(Icons.Default.PhoneInTalk, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_profile_intercom")
                                )
                            }

                            Text("Select Tower:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(towers) { t ->
                                    FilterChip(
                                        selected = tower == t,
                                        onClick = { tower = t },
                                        label = { Text(t, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("tower_chip_$t")
                                    )
                                }
                            }

                            Text("Residency Status:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = ownershipType == "OWNER",
                                    onClick = { ownershipType = "OWNER" },
                                    label = { Text("Apartment Owner", fontSize = 12.sp) },
                                    leadingIcon = {
                                        if (ownershipType == "OWNER") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_ownership_owner")
                                )
                                FilterChip(
                                    selected = ownershipType == "TENANT",
                                    onClick = { ownershipType = "TENANT" },
                                    label = { Text("Tenant / Lessee", fontSize = 12.sp) },
                                    leadingIcon = {
                                        if (ownershipType == "TENANT") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chip_ownership_tenant")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = parkingSlot,
                                    onValueChange = { parkingSlot = it.trim().uppercase() },
                                    label = { Text("Permanent Parking Bay") },
                                    leadingIcon = { Icon(Icons.Default.LocalParking, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_profile_parking")
                                )

                                OutlinedTextField(
                                    value = moveInDate,
                                    onValueChange = { moveInDate = it },
                                    label = { Text("Move-in Date") },
                                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("input_profile_move_in")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Resident / Family Count:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Total members residing in apartment", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { if (residentCount > 1) residentCount-- },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease")
                                    }
                                    Text(
                                        text = "$residentCount",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    IconButton(
                                        onClick = { if (residentCount < 15) residentCount++ },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "EMERGENCY" -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Emergency, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Emergency Broadcast & SOS Profile",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Used by Gate Security and Medical responders during emergencies",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = emergencyContactName,
                                onValueChange = { emergencyContactName = it },
                                label = { Text("Emergency Contact Person Name *") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_emergency_contact_name")
                            )

                            OutlinedTextField(
                                value = emergencyContactPhone,
                                onValueChange = { emergencyContactPhone = it },
                                label = { Text("Emergency Contact Mobile Phone *") },
                                leadingIcon = { Icon(Icons.Default.PhoneCallback, contentDescription = null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_emergency_contact_phone")
                            )

                            Text("Relationship to Resident:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(relations) { rel ->
                                    FilterChip(
                                        selected = emergencyContactRelation == rel,
                                        onClick = { emergencyContactRelation = rel },
                                        label = { Text(rel, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.testTag("relation_chip_$rel")
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = specialNotes,
                                onValueChange = { specialNotes = it },
                                label = { Text("Special Medical / Evacuation Notes") },
                                placeholder = { Text("e.g., Senior citizen on premises, wheelchair access, medical doctor, pet in house") },
                                leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null) },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_emergency_notes")
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Test Broadcast Button
                            FilledTonalButton(
                                onClick = { onSendTestEmergencyBroadcast(flatNumber) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_test_emergency_broadcast")
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Emergency Broadcast Alert", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            "ALERTS" -> {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "Personalized Notification Preferences",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Toggle 1: SMS Gate Alerts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Automated SMS Gate Alerts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Receive direct SMS when pre-approved visitors check in at main gate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = enableSmsAlerts,
                                    onCheckedChange = { enableSmsAlerts = it },
                                    modifier = Modifier.testTag("switch_sms_alerts")
                                )
                            }

                            HorizontalDivider()

                            // Toggle 2: Device Push Notifications
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("System Push Notifications", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Show high-priority system tray notifications on visitor arrival", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = enablePushAlerts,
                                    onCheckedChange = { enablePushAlerts = it },
                                    modifier = Modifier.testTag("switch_push_alerts")
                                )
                            }

                            HorizontalDivider()

                            // Toggle 3: Visitor Arrival Verification Alerts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Visitor Arrival Alerts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text("Notify when courier deliveries or cabs reach security booth", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = enableVisitorArrivalAlerts,
                                    onCheckedChange = { enableVisitorArrivalAlerts = it },
                                    modifier = Modifier.testTag("switch_visitor_alerts")
                                )
                            }

                            HorizontalDivider()

                            // Toggle 4: Emergency Broadcasts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Society Emergency Broadcasts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                    Text("Fire alarm, water tank maintenance, lift emergency, security lockdown alerts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = enableEmergencyBroadcasts,
                                    onCheckedChange = { enableEmergencyBroadcasts = it },
                                    modifier = Modifier.testTag("switch_emergency_broadcasts")
                                )
                            }

                            HorizontalDivider()

                            // Theme Mode Preference
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Display Theme Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        text = if (isDarkMode) "Dark Theme active • Tap to switch to Light Theme" else "Light Theme active • Bright, clean & high-contrast",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalButton(
                                    onClick = onToggleTheme,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("btn_toggle_theme_profile")
                                ) {
                                    Icon(
                                        if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isDarkMode) "Use Light" else "Use Dark", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Primary Save & Persist Action Button
        item {
            Button(
                onClick = {
                    val updated = ResidentProfile(
                        flatNumber = flatNumber.trim().uppercase(),
                        fullName = fullName.trim(),
                        tower = tower,
                        primaryPhone = primaryPhone.trim(),
                        alternatePhone = alternatePhone.trim(),
                        email = email.trim(),
                        ownershipType = ownershipType,
                        moveInDate = moveInDate.trim(),
                        intercomExtension = intercomExtension.trim(),
                        parkingSlot = parkingSlot.trim().uppercase(),
                        residentCount = residentCount,
                        bloodGroup = bloodGroup,
                        emergencyContactName = emergencyContactName.trim(),
                        emergencyContactPhone = emergencyContactPhone.trim(),
                        emergencyContactRelation = emergencyContactRelation,
                        enableSmsAlerts = enableSmsAlerts,
                        enablePushAlerts = enablePushAlerts,
                        enableVisitorArrivalAlerts = enableVisitorArrivalAlerts,
                        enableEmergencyBroadcasts = enableEmergencyBroadcasts,
                        specialNotes = specialNotes.trim(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSaveProfile(updated)
                },
                enabled = fullName.isNotBlank() && primaryPhone.isNotBlank() && flatNumber.isNotBlank() && !isProcessing,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_save_resident_profile")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Profile...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Persist Profile to Room", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
