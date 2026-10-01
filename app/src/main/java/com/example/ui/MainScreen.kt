package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.entity.MaintenanceBill
import com.example.ui.components.DocumentViewerDialog
import com.example.ui.components.GateQrExitScannerDialog
import com.example.ui.components.IntercomCallDialog
import com.example.ui.components.IntercomDirectoryDialog
import com.example.ui.components.IntercomMessagingDialog
import com.example.ui.components.PaymentGatewayDialog
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.VisitorCsvExportDialog
import com.example.ui.components.VisitorDigitalPassDialog
import com.example.ui.screens.*
import kotlinx.coroutines.launch

enum class AppTab(val label: String, val testTag: String) {
    VISITORS("Visitors", "tab_visitors"),
    VEHICLES("Vehicles", "tab_vehicles"),
    MAINTENANCE("Maintenance", "tab_maintenance"),
    NOTICES("Notices", "tab_notices"),
    DOCUMENTS("Documents", "tab_documents"),
    ALERTS("Gate & SOS", "tab_alerts"),
    PROFILE("My Profile", "tab_profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: SocietyViewModel) {
    var currentTab by remember { mutableStateOf(AppTab.VISITORS) }
    var showRoleDialog by remember { mutableStateOf(false) }
    var billToPay by remember { mutableStateOf<MaintenanceBill?>(null) }
    var isRegisteringVisitor by remember { mutableStateOf(false) }
    var prefillPlateForVisitor by remember { mutableStateOf("") }
    var prefillParkingSlotForVisitor by remember { mutableStateOf("") }
    var visitorViewMode by remember { mutableStateOf("DASHBOARD") }
    var showCsvExportDialog by remember { mutableStateOf(false) }

    val currentRole by viewModel.currentRole.collectAsStateWithLifecycle()
    val currentFlat by viewModel.currentResidentFlat.collectAsStateWithLifecycle()
    val residentName by viewModel.currentResidentName.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val visitors by viewModel.allVisitors.collectAsStateWithLifecycle()
    val recentVisitors by viewModel.recentVisitors.collectAsStateWithLifecycle()
    val allVehicles by viewModel.allVehicles.collectAsStateWithLifecycle()
    val currentFlatVehicles by viewModel.currentFlatVehicles.collectAsStateWithLifecycle()
    val livePlateCheckResult by viewModel.livePlateCheckResult.collectAsStateWithLifecycle()
    val visitorParkingSlots by viewModel.visitorParkingSlots.collectAsStateWithLifecycle()
    val bills by viewModel.allBills.collectAsStateWithLifecycle()
    val notices by viewModel.allNotices.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle()
    val notifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val financialOverview by viewModel.financialOverview.collectAsStateWithLifecycle()
    val unreadNotifsCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    val selectedBillForReceipt by viewModel.selectedBillForReceipt.collectAsStateWithLifecycle()
    val selectedDocForView by viewModel.selectedDocument.collectAsStateWithLifecycle()
    val selectedVisitorForQrPass by viewModel.selectedVisitorForQrPass.collectAsStateWithLifecycle()
    val isQrExitScannerOpen by viewModel.isQrExitScannerOpen.collectAsStateWithLifecycle()
    val insideVisitors by viewModel.insideVisitors.collectAsStateWithLifecycle()
    val dispatchedSms by viewModel.dispatchedSmsList.collectAsStateWithLifecycle()
    val activeIntercomCall by viewModel.activeIntercomCall.collectAsStateWithLifecycle()
    val intercomMessages by viewModel.intercomMessages.collectAsStateWithLifecycle()
    val selectedResidentForIntercomMsg by viewModel.selectedResidentForIntercomMsg.collectAsStateWithLifecycle()
    val showIntercomDirectory by viewModel.showIntercomDirectory.collectAsStateWithLifecycle()
    val visitorForIntercom by viewModel.visitorForIntercom.collectAsStateWithLifecycle()
    val currentProfile by viewModel.currentResidentProfile.collectAsStateWithLifecycle()
    val allProfiles by viewModel.allResidentProfiles.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    if (isRegisteringVisitor) {
        VisitorRegistrationScreen(
            currentFlat = currentFlat,
            prefillVehiclePlate = prefillPlateForVisitor,
            registeredVehicles = allVehicles,
            allParkingSlots = visitorParkingSlots,
            prefillParkingSlot = prefillParkingSlotForVisitor,
            onBack = {
                isRegisteringVisitor = false
                prefillPlateForVisitor = ""
                prefillParkingSlotForVisitor = ""
            },
            onSubmit = { name, phone, type, company, vehicle, tower, flat, host, preApp, remarks, photoUri, parkingSlot ->
                viewModel.registerNewVisitor(
                    name = name,
                    phone = phone,
                    purpose = type,
                    company = company,
                    vehicleNo = vehicle,
                    tower = tower,
                    flatNo = flat,
                    hostName = host,
                    isPreApproved = preApp,
                    remarks = remarks,
                    photoUri = photoUri,
                    parkingSlot = parkingSlot
                )
                isRegisteringVisitor = false
                prefillPlateForVisitor = ""
                prefillParkingSlotForVisitor = ""
            }
        )
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SocietyGate",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = when (currentRole) {
                                UserRole.RESIDENT -> "Resident • $currentFlat ($residentName)"
                                UserRole.GUARD -> "Main Gate Security Station"
                                UserRole.ADMIN -> "Property Manager • Administration"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Intercom Quick Dial Button
                    IconButton(
                        onClick = { viewModel.openIntercomDirectory() },
                        modifier = Modifier.testTag("top_bar_intercom_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (intercomMessages.any { it.decision == null && it.targetFlat == currentFlat }) {
                                    Badge { Text("1") }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PhoneInTalk,
                                contentDescription = "Gate Intercom",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Profile Management Shortcut Button
                    IconButton(
                        onClick = { currentTab = AppTab.PROFILE },
                        modifier = Modifier.testTag("top_bar_profile_btn")
                    ) {
                        Icon(
                            if (currentTab == AppTab.PROFILE) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = "My Profile",
                            tint = if (currentTab == AppTab.PROFILE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Light / Dark Theme Color Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier.testTag("top_bar_theme_toggle")
                    ) {
                        Icon(
                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Switch to Light Theme" else "Switch to Dark Theme",
                            tint = if (isDarkMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Role Switcher Button
                    Surface(
                        onClick = { showRoleDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("role_switch_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                when (currentRole) {
                                    UserRole.RESIDENT -> Icons.Default.Home
                                    UserRole.GUARD -> Icons.Default.Security
                                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentRole.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val (icon, selectedIcon) = when (tab) {
                        AppTab.VISITORS -> Icons.Outlined.Badge to Icons.Filled.Badge
                        AppTab.VEHICLES -> Icons.Outlined.DirectionsCar to Icons.Filled.DirectionsCar
                        AppTab.MAINTENANCE -> Icons.Outlined.Payments to Icons.Filled.Payments
                        AppTab.NOTICES -> Icons.Outlined.Campaign to Icons.Filled.Campaign
                        AppTab.DOCUMENTS -> Icons.Outlined.MenuBook to Icons.Filled.MenuBook
                        AppTab.ALERTS -> Icons.Outlined.Notifications to Icons.Filled.Notifications
                        AppTab.PROFILE -> Icons.Outlined.AccountCircle to Icons.Filled.AccountCircle
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == AppTab.ALERTS && unreadNotifsCount > 0) {
                                BadgedBox(badge = { Badge { Text("$unreadNotifsCount") } }) {
                                    Icon(if (isSelected) selectedIcon else icon, contentDescription = tab.label)
                                }
                            } else {
                                Icon(if (isSelected) selectedIcon else icon, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.VISITORS -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        val selectedIndex = when (visitorViewMode) {
                            "DASHBOARD" -> 0
                            "RECENT" -> 1
                            else -> 2
                        }

                        TabRow(
                            selectedTabIndex = selectedIndex,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = visitorViewMode == "DASHBOARD",
                                onClick = { visitorViewMode = "DASHBOARD" },
                                text = {
                                    Text(
                                        "Summary & Charts",
                                        fontWeight = if (visitorViewMode == "DASHBOARD") FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Insights,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_sub_visitor_dashboard")
                            )
                            Tab(
                                selected = visitorViewMode == "RECENT",
                                onClick = { visitorViewMode = "RECENT" },
                                text = {
                                    Text(
                                        "Recent Logs",
                                        fontWeight = if (visitorViewMode == "RECENT") FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_sub_recent_logs")
                            )
                            Tab(
                                selected = visitorViewMode == "CONSOLE",
                                onClick = { visitorViewMode = "CONSOLE" },
                                text = {
                                    Text(
                                        "Gate Console",
                                        fontWeight = if (visitorViewMode == "CONSOLE") FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    )
                                },
                                icon = {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                modifier = Modifier.testTag("tab_sub_gate_console")
                            )
                        }

                        when (visitorViewMode) {
                            "DASHBOARD" -> {
                                VisitorSummaryDashboardScreen(
                                    visitors = visitors,
                                    onOpenQrScanner = { viewModel.openQrExitScanner() },
                                    onViewRecentLogs = { visitorViewMode = "RECENT" },
                                    onExportCsv = { showCsvExportDialog = true },
                                    onOpenIntercom = { viewModel.openIntercomDirectory() },
                                    onShowSnackbar = { msg ->
                                        coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                )
                            }
                            "RECENT" -> {
                                RecentVisitorsScreen(
                                    visitors = recentVisitors,
                                    onCheckOut = { viewModel.checkOutVisitor(it) },
                                    onCheckIn = { viewModel.checkInVisitor(it) },
                                    onOpenRegistrationForm = { isRegisteringVisitor = true },
                                    onShowQrPass = { viewModel.showVisitorQrPass(it) },
                                    onOpenQrExitScanner = { viewModel.openQrExitScanner() },
                                    onExportCsv = { showCsvExportDialog = true },
                                    onStartIntercomCall = { visitor ->
                                        viewModel.startIntercomCall(visitor.flatNumber, visitor)
                                    },
                                    onOpenIntercomMessage = { visitor ->
                                        val resident = com.example.data.model.IntercomResident.DIRECTORY.find { it.flatNumber.equals(visitor.flatNumber, ignoreCase = true) }
                                            ?: com.example.data.model.IntercomResident(visitor.flatNumber, visitor.tower, visitor.hostResidentName, "+91 98201 12345", visitor.flatNumber.replace(Regex("[^0-9]"), ""))
                                        viewModel.openIntercomMessaging(resident, visitor)
                                    }
                                )
                            }
                            else -> {
                                VisitorsScreen(
                                    visitors = visitors,
                                    currentRole = currentRole,
                                    currentFlat = currentFlat,
                                    onApprove = { viewModel.approveVisitor(it) },
                                    onDeny = { viewModel.denyVisitor(it) },
                                    onCheckIn = { viewModel.checkInVisitor(it) },
                                    onCheckOut = { viewModel.checkOutVisitor(it) },
                                    onOpenRegistrationForm = { isRegisteringVisitor = true },
                                    onExportCsv = { showCsvExportDialog = true },
                                    onRegisterVisitor = { name, phone, purpose, company, vehicle, tower, flat, host, preApp, remarks ->
                                        viewModel.registerNewVisitor(name, phone, purpose, company, vehicle, tower, flat, host, preApp, remarks)
                                    }
                                )
                            }
                        }
                    }
                }
                AppTab.VEHICLES -> {
                    VehicleManagementScreen(
                        vehicles = allVehicles,
                        currentFlatVehicles = currentFlatVehicles,
                        currentRole = currentRole,
                        currentFlat = currentFlat,
                        residentName = residentName,
                        livePlateCheckResult = livePlateCheckResult,
                        visitorParkingSlots = visitorParkingSlots,
                        insideVisitors = insideVisitors,
                        onCheckPlate = { plate -> viewModel.checkPlateNumber(plate) },
                        onClearPlateCheck = { viewModel.clearPlateCheck() },
                        onAssignVisitorParking = { visitorId, slot ->
                            viewModel.assignVisitorParkingSlot(visitorId, slot)
                        },
                        onReleaseVisitorParking = { visitorId, slot ->
                            viewModel.releaseVisitorParkingSlot(visitorId, slot)
                        },
                        onNavigateToCheckInWithSlot = { slot ->
                            prefillParkingSlotForVisitor = slot
                            isRegisteringVisitor = true
                        },
                        onRegisterVehicle = { plate, type, makeModel, color, tower, flat, owner, phone, slot, notes ->
                            viewModel.registerResidentVehicle(
                                plateNumber = plate,
                                vehicleType = type,
                                makeModel = makeModel,
                                color = color,
                                tower = tower,
                                flatNumber = flat,
                                ownerName = owner,
                                ownerPhone = phone,
                                parkingSlot = slot,
                                notes = notes
                            )
                        },
                        onDeleteVehicle = { id -> viewModel.deleteVehicle(id) },
                        onNavigateToVisitorRegistration = { prefillPlate ->
                            prefillPlateForVisitor = prefillPlate
                            isRegisteringVisitor = true
                        }
                    )
                }
                AppTab.MAINTENANCE -> {
                    MaintenanceScreen(
                        bills = bills,
                        financialOverview = financialOverview,
                        currentRole = currentRole,
                        currentFlat = currentFlat,
                        onPayBill = { bill -> billToPay = bill },
                        onViewReceipt = { bill -> viewModel.viewReceipt(bill) },
                        onGenerateBilling = { month -> viewModel.generateNextMonthBilling(month) },
                        onDispatchReminders = { tower -> viewModel.dispatchOverdueReminders(tower) }
                    )
                }
                AppTab.NOTICES -> {
                    NoticeBoardScreen(
                        notices = notices,
                        currentRole = currentRole,
                        onPublishNotice = { title, desc, cat, priority, aud ->
                            viewModel.publishNotice(title, desc, cat, priority, aud)
                        },
                        onDeleteNotice = { id -> viewModel.deleteNotice(id) }
                    )
                }
                AppTab.DOCUMENTS -> {
                    DocumentsScreen(
                        documents = documents,
                        currentRole = currentRole,
                        onViewDocument = { doc -> viewModel.viewDocument(doc) },
                        onUploadDocument = { title, cat, summary, sections, ver ->
                            viewModel.uploadDocument(title, cat, summary, sections, ver)
                        }
                    )
                }
                AppTab.ALERTS -> {
                    GateAlertsScreen(
                        notifications = notifications,
                        dispatchedSms = dispatchedSms,
                        currentRole = currentRole,
                        currentFlat = currentFlat,
                        residentName = residentName,
                        onMarkRead = { id -> viewModel.markNotificationRead(id) },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                        onTriggerSOS = { type, remarks -> viewModel.triggerEmergencySOS(type, remarks) }
                    )
                }
                AppTab.PROFILE -> {
                    ResidentProfileScreen(
                        currentFlat = currentFlat,
                        profile = currentProfile,
                        allProfiles = allProfiles,
                        isProcessing = isProcessing,
                        isDarkMode = isDarkMode,
                        onToggleTheme = { viewModel.toggleDarkMode() },
                        onSaveProfile = { updated -> viewModel.updateResidentProfile(updated) },
                        onSendTestEmergencyBroadcast = { flat -> viewModel.sendTestEmergencyBroadcast(flat) },
                        onSwitchFlat = { flat, name -> viewModel.selectResidentFlat(flat, name) }
                    )
                }
            }
        }
    }

    // Role Switcher Dialog
    if (showRoleDialog) {
        RoleSwitcherDialog(
            currentRole = currentRole,
            currentFlat = currentFlat,
            onSelectRole = { role ->
                viewModel.switchRole(role)
            },
            onSelectFlat = { flat, resident ->
                viewModel.selectResidentFlat(flat, resident)
            },
            onDismiss = { showRoleDialog = false }
        )
    }

    // Payment Gateway Processing Modal
    billToPay?.let { bill ->
        PaymentGatewayDialog(
            bill = bill,
            isProcessing = isProcessing,
            onProcessPayment = { request, onStageUpdate, onComplete ->
                viewModel.processGatewayTransaction(request, onStageUpdate, onComplete)
            },
            onDismiss = { billToPay = null }
        )
    }

    // Digital Receipt Modal
    selectedBillForReceipt?.let { bill ->
        ReceiptDialog(
            bill = bill,
            onDismiss = { viewModel.closeReceipt() }
        )
    }

    // Document Viewer Modal
    selectedDocForView?.let { doc ->
        DocumentViewerDialog(
            doc = doc,
            onDismiss = { viewModel.closeDocument() }
        )
    }

    // Digital Visitor QR Entry Pass Modal
    selectedVisitorForQrPass?.let { visitor ->
        VisitorDigitalPassDialog(
            visitor = visitor,
            onDismiss = { viewModel.closeVisitorQrPass() }
        )
    }

    // Gate QR Scanner & Fast Exit Logout Terminal Modal
    if (isQrExitScannerOpen) {
        GateQrExitScannerDialog(
            activeVisitorsInside = insideVisitors,
            onDismiss = { viewModel.closeQrExitScanner() },
            onProcessExit = { qrToken ->
                viewModel.processQrLogout(qrToken) { success, msg -> }
            }
        )
    }

    // Visitor CSV Audit Log Export Modal
    if (showCsvExportDialog) {
        VisitorCsvExportDialog(
            visitors = visitors,
            onDismiss = { showCsvExportDialog = false },
            onShowSnackbar = { msg ->
                coroutineScope.launch { snackbarHostState.showSnackbar(msg) }
            }
        )
    }

    // In-App Intercom Voice Call Modal
    activeIntercomCall?.let { call ->
        IntercomCallDialog(
            call = call,
            currentRole = currentRole,
            onAnswer = { viewModel.answerIntercomCall() },
            onEndCall = { viewModel.endIntercomCall() },
            onToggleMute = { viewModel.toggleIntercomMute() },
            onToggleSpeaker = { viewModel.toggleIntercomSpeaker() },
            onQuickDecision = { decision -> viewModel.handleIntercomCallDecision(decision) }
        )
    }

    // In-App Intercom Messaging Modal
    selectedResidentForIntercomMsg?.let { resident ->
        IntercomMessagingDialog(
            targetResident = resident,
            visitor = visitorForIntercom,
            messages = intercomMessages,
            currentRole = currentRole,
            onSendMessage = { text ->
                viewModel.sendIntercomMessage(
                    targetFlat = resident.flatNumber,
                    text = text,
                    visitorId = visitorForIntercom?.id,
                    visitorName = visitorForIntercom?.visitorName
                )
            },
            onDecisionReply = { messageId, decision ->
                viewModel.replyIntercomDecision(messageId, decision)
            },
            onDismiss = { viewModel.closeIntercomMessaging() }
        )
    }

    // In-App Intercom Resident Directory Dialing Modal
    if (showIntercomDirectory) {
        IntercomDirectoryDialog(
            visitorForVerification = visitorForIntercom,
            onCallResident = { resident ->
                viewModel.startIntercomCall(resident.flatNumber, visitorForIntercom)
                viewModel.closeIntercomDirectory()
            },
            onMessageResident = { resident ->
                viewModel.openIntercomMessaging(resident, visitorForIntercom)
                viewModel.closeIntercomDirectory()
            },
            onDismiss = { viewModel.closeIntercomDirectory() }
        )
    }
}

@Composable
fun RoleSwitcherDialog(
    currentRole: UserRole,
    currentFlat: String,
    onSelectRole: (UserRole) -> Unit,
    onSelectFlat: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sampleResidentFlats = listOf(
        Pair("A-101", "Chandan Kumar"),
        Pair("B-204", "Priya Sharma"),
        Pair("C-302", "Dr. Ananya Roy"),
        Pair("D-401", "Vikramaditya Rao")
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("role_switcher_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select Society Persona",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Experience the app from different user perspectives:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Role 1: Resident
                RoleOptionCard(
                    title = "Resident Profile",
                    subtitle = "Personal ledger, guest invites, approval alerts, notices",
                    icon = Icons.Default.Home,
                    isSelected = currentRole == UserRole.RESIDENT,
                    onClick = {
                        onSelectRole(UserRole.RESIDENT)
                    }
                )

                if (currentRole == UserRole.RESIDENT) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Switch Resident Flat:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sampleResidentFlats.forEach { (flat, name) ->
                            val isFlatSelected = currentFlat == flat
                            FilterChip(
                                selected = isFlatSelected,
                                onClick = { onSelectFlat(flat, name) },
                                label = { Text(flat, fontSize = 11.sp, fontWeight = if (isFlatSelected) FontWeight.Bold else FontWeight.Normal) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Role 2: Gate Security Guard
                RoleOptionCard(
                    title = "Gate Security Guard",
                    subtitle = "Rapid check-in/out, live inside gate tracker, verify OTP pass",
                    icon = Icons.Default.Security,
                    isSelected = currentRole == UserRole.GUARD,
                    onClick = {
                        onSelectRole(UserRole.GUARD)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Role 3: Property Manager
                RoleOptionCard(
                    title = "Property Manager / Admin",
                    subtitle = "Tower-wise collection analytics, automated billing, reports, notices",
                    icon = Icons.Default.AdminPanelSettings,
                    isSelected = currentRole == UserRole.ADMIN,
                    onClick = {
                        onSelectRole(UserRole.ADMIN)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply & Close")
                }
            }
        }
    }
}

@Composable
fun RoleOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
