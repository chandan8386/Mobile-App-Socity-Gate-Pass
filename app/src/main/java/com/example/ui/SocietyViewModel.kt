package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.SocietyDatabase
import com.example.data.entity.GateNotification
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.Notice
import com.example.data.entity.ResidentProfile
import com.example.data.entity.ResidentVehicle
import com.example.data.entity.SocietyDocument
import com.example.data.entity.VisitorEntry
import com.example.data.model.IntercomActiveCall
import com.example.data.model.IntercomCallState
import com.example.data.model.IntercomMessage
import com.example.data.model.IntercomResident
import com.example.data.model.VisitorParkingSlot
import com.example.data.repository.SocietyRepository
import com.example.service.notification.SmsDispatchRecord
import com.example.service.notification.VisitorNotificationService
import com.example.service.payment.PaymentTransactionRequest
import com.example.service.payment.PaymentTransactionResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class UserRole(val displayName: String, val badge: String) {
    RESIDENT("Resident", "Flat A-101"),
    GUARD("Gate Security", "Main Gate Station"),
    ADMIN("Property Manager", "Society Administration")
}

data class TowerFinancialStats(
    val towerName: String,
    val totalDemand: Double,
    val collectedAmount: Double,
    val pendingAmount: Double,
    val totalFlats: Int,
    val paidFlats: Int,
    val collectionRate: Float // 0.0 to 1.0
)

data class SocietyFinancialOverview(
    val totalBilled: Double,
    val totalCollected: Double,
    val totalPending: Double,
    val overallCollectionRate: Float,
    val towerBreakdowns: List<TowerFinancialStats>
)

class SocietyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SocietyRepository
    private val notificationService: VisitorNotificationService
    init {
        val db = SocietyDatabase.getDatabase(application, viewModelScope)
        repository = SocietyRepository(db.societyDao())
        notificationService = VisitorNotificationService(application)
    }

    val dispatchedSmsList: StateFlow<List<SmsDispatchRecord>> = notificationService.dispatchedSmsList

    // Role state
    private val _currentRole = MutableStateFlow(UserRole.RESIDENT)
    val currentRole: StateFlow<UserRole> = _currentRole.asStateFlow()

    private val _currentResidentFlat = MutableStateFlow("A-101")
    val currentResidentFlat: StateFlow<String> = _currentResidentFlat.asStateFlow()

    private val _currentResidentName = MutableStateFlow("Chandan Kumar")
    val currentResidentName: StateFlow<String> = _currentResidentName.asStateFlow()

    // App Theme State (Defaults to clean Light theme)
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
        _snackbarMessage.value = if (_isDarkMode.value) "Dark theme enabled" else "Light theme enabled"
    }

    fun setDarkMode(enabled: Boolean) {
        _isDarkMode.value = enabled
        _snackbarMessage.value = if (enabled) "Dark theme enabled" else "Light theme enabled"
    }

    // Processing indicator (for payments, billing generation, etc.)
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Active receipt to display in modal
    private val _selectedBillForReceipt = MutableStateFlow<MaintenanceBill?>(null)
    val selectedBillForReceipt: StateFlow<MaintenanceBill?> = _selectedBillForReceipt.asStateFlow()

    // Document to view in modal
    private val _selectedDocument = MutableStateFlow<SocietyDocument?>(null)
    val selectedDocument: StateFlow<SocietyDocument?> = _selectedDocument.asStateFlow()

    // Visitor Digital QR Pass modal
    private val _selectedVisitorForQrPass = MutableStateFlow<VisitorEntry?>(null)
    val selectedVisitorForQrPass: StateFlow<VisitorEntry?> = _selectedVisitorForQrPass.asStateFlow()

    // Scanner state for scanning QR exit
    private val _isQrExitScannerOpen = MutableStateFlow(false)
    val isQrExitScannerOpen: StateFlow<Boolean> = _isQrExitScannerOpen.asStateFlow()

    // --- Intercom State ---
    private val _activeIntercomCall = MutableStateFlow<IntercomActiveCall?>(null)
    val activeIntercomCall: StateFlow<IntercomActiveCall?> = _activeIntercomCall.asStateFlow()

    private val _intercomMessages = MutableStateFlow<List<IntercomMessage>>(
        listOf(
            IntercomMessage(
                senderRole = "GUARD",
                senderName = "Gate Guard Deshmukh",
                targetFlat = "A-101",
                visitorName = "Swiggy Delivery Partner",
                messageText = "Swiggy food delivery is at the main gate for Flat A-101. Verify entry?",
                decision = "APPROVED",
                timestamp = System.currentTimeMillis() - (45 * 60 * 1000L)
            ),
            IntercomMessage(
                senderRole = "GUARD",
                senderName = "Security Officer Pawar",
                targetFlat = "B-204",
                visitorName = "Amitabh Sen",
                messageText = "Urban Company technician Amitabh Sen has arrived for AC servicing. Allow inside?",
                decision = "APPROVED",
                timestamp = System.currentTimeMillis() - (120 * 60 * 1000L)
            )
        )
    )
    val intercomMessages: StateFlow<List<IntercomMessage>> = _intercomMessages.asStateFlow()

    private val _selectedResidentForIntercomMsg = MutableStateFlow<IntercomResident?>(null)
    val selectedResidentForIntercomMsg: StateFlow<IntercomResident?> = _selectedResidentForIntercomMsg.asStateFlow()

    private val _showIntercomDirectory = MutableStateFlow(false)
    val showIntercomDirectory: StateFlow<Boolean> = _showIntercomDirectory.asStateFlow()

    private val _visitorForIntercom = MutableStateFlow<VisitorEntry?>(null)
    val visitorForIntercom: StateFlow<VisitorEntry?> = _visitorForIntercom.asStateFlow()

    // Data streams from repository
    val allVisitors: StateFlow<List<VisitorEntry>> = repository.allVisitors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentVisitors: StateFlow<List<VisitorEntry>> = repository.getRecentVisitors(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insideVisitors: StateFlow<List<VisitorEntry>> = repository.insideVisitors
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBills: StateFlow<List<MaintenanceBill>> = repository.allBills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotices: StateFlow<List<Notice>> = repository.allNotices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDocuments: StateFlow<List<SocietyDocument>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotifications: StateFlow<List<GateNotification>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Resident Vehicles State & Gate Cross-Referencing
    val allVehicles: StateFlow<List<ResidentVehicle>> = repository.allVehicles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentFlatVehicles: StateFlow<List<ResidentVehicle>> = allVehicles.combine(_currentResidentFlat) { vehicles, flat ->
        vehicles.filter { it.flatNumber == flat }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Resident Profiles State
    val allResidentProfiles: StateFlow<List<ResidentProfile>> = repository.getAllResidentProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentResidentProfile: StateFlow<ResidentProfile?> = _currentResidentFlat
        .flatMapLatest { flat -> repository.getResidentProfile(flat) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _livePlateCheckResult = MutableStateFlow<PlateCheckResult?>(null)
    val livePlateCheckResult: StateFlow<PlateCheckResult?> = _livePlateCheckResult.asStateFlow()

    // Visitor Parking Management State
    val visitorParkingSlots: StateFlow<List<VisitorParkingSlot>> = insideVisitors.combine(allVisitors) { insideList, _ ->
        val activeMap = insideList
            .filter { it.status == "INSIDE" && it.parkingSlot.isNotBlank() }
            .associateBy { it.parkingSlot.trim().uppercase() }

        VisitorParkingSlot.ALL_DEFAULT_SLOTS.map { defaultSlot ->
            val activeVisitor = activeMap[defaultSlot.slotNumber.uppercase()]
            if (activeVisitor != null) {
                defaultSlot.copy(
                    isOccupied = true,
                    visitorId = activeVisitor.id,
                    visitorName = activeVisitor.visitorName,
                    vehicleNumber = activeVisitor.vehicleNumber,
                    flatNumber = activeVisitor.flatNumber,
                    tower = activeVisitor.tower,
                    occupiedSince = activeVisitor.timestamp
                )
            } else {
                defaultSlot.copy(isOccupied = false)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VisitorParkingSlot.ALL_DEFAULT_SLOTS)

    val availableParkingSlots: StateFlow<List<VisitorParkingSlot>> = visitorParkingSlots.combine(_currentResidentFlat) { slots, _ ->
        slots.filter { !it.isOccupied }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tower financial statistics computed reactively
    val financialOverview: StateFlow<SocietyFinancialOverview> = allBills.combine(_currentResidentFlat) { bills, _ ->
        val towers = listOf("Tower A", "Tower B", "Tower C", "Tower D")
        val breakdowns = towers.map { towerName ->
            val towerBills = bills.filter { it.tower == towerName }
            val demand = towerBills.sumOf { it.totalAmount }
            val collected = towerBills.filter { it.status == "PAID" }.sumOf { it.totalAmount }
            val pending = demand - collected
            val totalFlats = towerBills.size.coerceAtLeast(1)
            val paidFlats = towerBills.count { it.status == "PAID" }
            val rate = if (demand > 0) (collected / demand).toFloat() else 0f
            TowerFinancialStats(
                towerName = towerName,
                totalDemand = demand,
                collectedAmount = collected,
                pendingAmount = pending,
                totalFlats = totalFlats,
                paidFlats = paidFlats,
                collectionRate = rate
            )
        }

        val totalBilled = bills.sumOf { it.totalAmount }
        val totalCollected = bills.filter { it.status == "PAID" }.sumOf { it.totalAmount }
        val totalPending = totalBilled - totalCollected
        val overallRate = if (totalBilled > 0) (totalCollected / totalBilled).toFloat() else 0f

        SocietyFinancialOverview(
            totalBilled = totalBilled,
            totalCollected = totalCollected,
            totalPending = totalPending,
            overallCollectionRate = overallRate,
            towerBreakdowns = breakdowns
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SocietyFinancialOverview(0.0, 0.0, 0.0, 0f, emptyList())
    )

    // Unread count
    val unreadNotificationsCount: StateFlow<Int> = allNotifications.combine(_currentResidentFlat) { notifs, _ ->
        notifs.count { !it.isRead }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun switchRole(role: UserRole) {
        _currentRole.value = role
        _snackbarMessage.value = "Switched to ${role.displayName} view"
    }

    fun selectResidentFlat(flatNumber: String, residentName: String) {
        _currentResidentFlat.value = flatNumber
        _currentResidentName.value = residentName
        _snackbarMessage.value = "Viewing flat $flatNumber ($residentName)"
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun viewReceipt(bill: MaintenanceBill) {
        _selectedBillForReceipt.value = bill
    }

    fun closeReceipt() {
        _selectedBillForReceipt.value = null
    }

    fun viewDocument(doc: SocietyDocument) {
        _selectedDocument.value = doc
        viewModelScope.launch {
            repository.recordDownload(doc.id)
        }
    }

    fun closeDocument() {
        _selectedDocument.value = null
    }

    // --- Visitor Operations ---
    fun registerNewVisitor(
        name: String,
        phone: String,
        purpose: String,
        company: String,
        vehicleNo: String,
        tower: String,
        flatNo: String,
        hostName: String,
        isPreApproved: Boolean = false,
        remarks: String = "",
        photoUri: String? = null,
        parkingSlot: String = ""
    ) {
        viewModelScope.launch {
            val passCode = Random.nextInt(1000, 9999).toString()
            val entry = VisitorEntry(
                visitorName = name,
                visitorPhoneNumber = phone,
                visitorType = purpose,
                visitorCompany = company,
                vehicleNumber = vehicleNo.ifBlank { "None" },
                tower = tower,
                flatNumber = flatNo,
                hostResidentName = hostName,
                status = if (isPreApproved) "APPROVED" else "WAITING_APPROVAL",
                passCode = passCode,
                qrCodeToken = "GATE-PASS-${System.currentTimeMillis()}-$passCode",
                photoUri = photoUri,
                parkingSlot = parkingSlot.trim().uppercase(),
                remarks = remarks
            )
            repository.registerVisitor(entry)
            _snackbarMessage.value = if (isPreApproved) {
                "Guest pre-invite generated! Passcode: $passCode"
            } else if (parkingSlot.isNotBlank()) {
                "Visitor registered! Allocated Parking Bay: $parkingSlot"
            } else {
                "Visitor registered! Approval request sent to flat $flatNo"
            }
        }
    }

    fun assignVisitorParkingSlot(visitorId: Long, slotNumber: String) {
        viewModelScope.launch {
            repository.assignVisitorParkingSlot(visitorId, slotNumber.trim().uppercase())
            _snackbarMessage.value = "Assigned parking bay $slotNumber to visitor"
        }
    }

    fun releaseVisitorParkingSlot(visitorId: Long, slotNumber: String = "") {
        viewModelScope.launch {
            repository.releaseVisitorParkingSlot(visitorId)
            _snackbarMessage.value = if (slotNumber.isNotBlank()) "Parking bay $slotNumber released" else "Visitor parking released"
        }
    }

    fun approveVisitor(id: Long) {
        viewModelScope.launch {
            repository.approveVisitor(id)
            _snackbarMessage.value = "Visitor entry approved"
        }
    }

    fun checkInVisitor(id: Long) {
        viewModelScope.launch {
            repository.checkInVisitor(id)
            val visitor = repository.getVisitorById(id)
            if (visitor != null) {
                val profile = repository.getResidentProfileOnce(visitor.flatNumber)
                val smsRecord = notificationService.sendCheckInAlerts(visitor, profile)
                val parkingNote = if (visitor.parkingSlot.isNotBlank()) " Assigned Bay: ${visitor.parkingSlot}." else ""
                val notif = GateNotification(
                    title = "🔔 Guest Checked In at Gate: ${visitor.visitorName}",
                    message = "${visitor.visitorName} (${visitor.visitorType}) verified at security gate for Flat ${visitor.flatNumber}.$parkingNote Push & SMS delivered to ${smsRecord.recipientPhone}.",
                    type = "VISITOR",
                    flatNumber = visitor.flatNumber,
                    priority = "HIGH"
                )
                repository.addNotification(notif)
                _snackbarMessage.value = "✓ Checked IN! Push & SMS alert sent to Flat ${visitor.flatNumber} (${smsRecord.recipientPhone})"
            } else {
                _snackbarMessage.value = "Visitor marked INSIDE gate"
            }
        }
    }

    fun checkInVisitorWithPhoto(id: Long, photoUri: String?) {
        viewModelScope.launch {
            repository.checkInVisitor(id)
            if (!photoUri.isNullOrBlank()) {
                repository.updateVisitorPhoto(id, photoUri)
            }
            val visitor = repository.getVisitorById(id)
            if (visitor != null) {
                val profile = repository.getResidentProfileOnce(visitor.flatNumber)
                val smsRecord = notificationService.sendCheckInAlerts(visitor, profile)
                val parkingNote = if (visitor.parkingSlot.isNotBlank()) " Assigned Bay: ${visitor.parkingSlot}." else ""
                val notif = GateNotification(
                    title = "🔔 Guest Verified & Checked In: ${visitor.visitorName}",
                    message = "${visitor.visitorName} (${visitor.visitorType}) checked in with photo verification for Flat ${visitor.flatNumber}.$parkingNote Push & SMS sent to ${smsRecord.recipientPhone}.",
                    type = "VISITOR",
                    flatNumber = visitor.flatNumber,
                    priority = "HIGH"
                )
                repository.addNotification(notif)
                _snackbarMessage.value = "✓ Photo verified! Push & SMS dispatched to Flat ${visitor.flatNumber} (${smsRecord.recipientPhone})"
            } else {
                _snackbarMessage.value = "Visitor verified with photo & checked IN"
            }
        }
    }

    fun updateVisitorPhoto(id: Long, photoUri: String) {
        viewModelScope.launch {
            repository.updateVisitorPhoto(id, photoUri)
            _snackbarMessage.value = "Visitor photo updated in security log"
        }
    }

    fun checkOutVisitor(id: Long) {
        viewModelScope.launch {
            repository.checkOutVisitor(id)
            _snackbarMessage.value = "Visitor exited successfully"
        }
    }

    // --- QR Code Pass & Fast Logout Features ---
    fun showVisitorQrPass(visitor: VisitorEntry) {
        _selectedVisitorForQrPass.value = visitor
    }

    fun closeVisitorQrPass() {
        _selectedVisitorForQrPass.value = null
    }

    fun openQrExitScanner() {
        _isQrExitScannerOpen.value = true
    }

    fun closeQrExitScanner() {
        _isQrExitScannerOpen.value = false
    }

    fun processQrLogout(qrCodeToken: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            val exitedVisitor = repository.processVisitorExitByQr(qrCodeToken)
            _isProcessing.value = false
            if (exitedVisitor != null) {
                _snackbarMessage.value = "✅ Fast Logout: ${exitedVisitor.visitorName} exited successfully via QR"
                onComplete(true, "Visitor ${exitedVisitor.visitorName} (${exitedVisitor.visitorType}) verified & checked out.")
            } else {
                _snackbarMessage.value = "⚠️ Invalid QR: No active visitor matching this pass"
                onComplete(false, "No active visitor record found for QR: $qrCodeToken")
            }
        }
    }

    fun denyVisitor(id: Long) {
        viewModelScope.launch {
            repository.denyVisitor(id)
            _snackbarMessage.value = "Visitor entry DENIED"
        }
    }

    // --- Intercom Call & Messaging Operations ---
    fun startIntercomCall(flatNumber: String, visitor: VisitorEntry? = null) {
        val resident = IntercomResident.DIRECTORY.find { it.flatNumber.equals(flatNumber, ignoreCase = true) }
            ?: IntercomResident(flatNumber, "Tower", "Resident of $flatNumber", "+91 98201 12345", flatNumber.replace(Regex("[^0-9]"), ""))

        _activeIntercomCall.value = IntercomActiveCall(
            callerRole = if (_currentRole.value == UserRole.GUARD) "SECURITY_GUARD" else "RESIDENT",
            callerName = if (_currentRole.value == UserRole.GUARD) "Gate Guard Desk" else _currentResidentName.value,
            targetFlat = resident.flatNumber,
            targetResidentName = resident.residentName,
            visitorId = visitor?.id,
            visitorName = visitor?.visitorName,
            visitorType = visitor?.visitorType,
            visitorVehicle = visitor?.vehicleNumber,
            callState = IntercomCallState.RINGING,
            startTime = System.currentTimeMillis()
        )
        _snackbarMessage.value = "Dialing Intercom EXT: ${resident.intercomExtension} (Flat ${resident.flatNumber})..."
    }

    fun answerIntercomCall() {
        _activeIntercomCall.value = _activeIntercomCall.value?.copy(callState = IntercomCallState.CONNECTED)
    }

    fun endIntercomCall() {
        _activeIntercomCall.value = null
        _snackbarMessage.value = "Intercom call ended"
    }

    fun toggleIntercomMute() {
        _activeIntercomCall.value = _activeIntercomCall.value?.let { it.copy(isMuted = !it.isMuted) }
    }

    fun toggleIntercomSpeaker() {
        _activeIntercomCall.value = _activeIntercomCall.value?.let { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun handleIntercomCallDecision(decision: String) {
        val currentCall = _activeIntercomCall.value
        val visitorId = currentCall?.visitorId
        viewModelScope.launch {
            if (visitorId != null) {
                when (decision) {
                    "APPROVED" -> {
                        repository.approveVisitor(visitorId)
                        repository.checkInVisitor(visitorId)
                        _snackbarMessage.value = "✓ Resident APPROVED visitor via Intercom! Gate opened."
                    }
                    "DENIED" -> {
                        repository.denyVisitor(visitorId)
                        _snackbarMessage.value = "✗ Resident DENIED visitor access via Intercom."
                    }
                    "PARCEL_AT_GATE" -> {
                        _snackbarMessage.value = "📦 Resident instructed: Hold parcel at Gate Guard Desk."
                    }
                }
            } else {
                _snackbarMessage.value = "Decision '$decision' registered from resident"
            }
            _activeIntercomCall.value = null
        }
    }

    fun openIntercomMessaging(resident: IntercomResident, visitor: VisitorEntry? = null) {
        _selectedResidentForIntercomMsg.value = resident
        _visitorForIntercom.value = visitor
    }

    fun closeIntercomMessaging() {
        _selectedResidentForIntercomMsg.value = null
        _visitorForIntercom.value = null
    }

    fun sendIntercomMessage(targetFlat: String, text: String, visitorId: Long? = null, visitorName: String? = null) {
        val newMsg = IntercomMessage(
            senderRole = if (_currentRole.value == UserRole.GUARD) "GUARD" else "RESIDENT",
            senderName = if (_currentRole.value == UserRole.GUARD) "Gate Security" else _currentResidentName.value,
            targetFlat = targetFlat,
            visitorId = visitorId,
            visitorName = visitorName,
            messageText = text,
            timestamp = System.currentTimeMillis()
        )
        _intercomMessages.value = _intercomMessages.value + newMsg
        _snackbarMessage.value = "Intercom message sent to Flat $targetFlat"

        viewModelScope.launch {
            repository.addNotification(
                GateNotification(
                    title = "💬 Gate Intercom Message",
                    message = "Security: $text",
                    type = "SECURITY",
                    flatNumber = targetFlat,
                    priority = "NORMAL"
                )
            )
        }
    }

    fun replyIntercomDecision(messageId: String, decision: String) {
        val msg = _intercomMessages.value.find { it.id == messageId }
        _intercomMessages.value = _intercomMessages.value.map {
            if (it.id == messageId) it.copy(decision = decision) else it
        }
        val visitorId = msg?.visitorId
        viewModelScope.launch {
            if (visitorId != null) {
                if (decision == "APPROVED") {
                    repository.approveVisitor(visitorId)
                    repository.checkInVisitor(visitorId)
                    _snackbarMessage.value = "✓ Visitor Approved via Intercom!"
                } else if (decision == "DENIED") {
                    repository.denyVisitor(visitorId)
                    _snackbarMessage.value = "✗ Visitor Denied via Intercom"
                }
            } else {
                _snackbarMessage.value = "Response '$decision' transmitted to Gate"
            }
        }
    }

    fun openIntercomDirectory(visitor: VisitorEntry? = null) {
        _visitorForIntercom.value = visitor
        _showIntercomDirectory.value = true
    }

    fun closeIntercomDirectory() {
        _showIntercomDirectory.value = false
        _visitorForIntercom.value = null
    }

    // --- Maintenance & Payments ---
    fun payMaintenanceBill(billId: Long, gateway: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            // Simulate realistic payment gateway processing delay
            delay(1200)
            val receipt = repository.processPayment(billId, gateway)
            _isProcessing.value = false
            _snackbarMessage.value = "Payment of bill #$billId successful via $gateway! Receipt: $receipt"
        }
    }

    fun processGatewayTransaction(
        request: PaymentTransactionRequest,
        onStageUpdate: (String) -> Unit = {},
        onComplete: (PaymentTransactionResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.executeGatewayPayment(request, onStageUpdate)
            _isProcessing.value = false
            if (result.isSuccess) {
                _snackbarMessage.value = "Payment Successful! Txn ID: ${result.transactionId}"
            } else {
                _snackbarMessage.value = "Payment Failed: ${result.errorMessage ?: "Transaction declined"}"
            }
            onComplete(result)
        }
    }

    fun generateNextMonthBilling(monthName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            delay(800)
            repository.generateMonthlyBilling(monthName)
            _isProcessing.value = false
            _snackbarMessage.value = "Maintenance bills for $monthName generated for all towers!"
        }
    }

    fun dispatchOverdueReminders(tower: String? = null) {
        viewModelScope.launch {
            repository.dispatchDueReminders(tower)
            _snackbarMessage.value = "Overdue maintenance payment reminders dispatched via SMS & Push!"
        }
    }

    // --- Notices & Docs ---
    fun publishNotice(
        title: String,
        description: String,
        category: String,
        priority: String,
        targetAudience: String
    ) {
        viewModelScope.launch {
            val notice = Notice(
                title = title,
                description = description,
                category = category,
                priority = priority,
                postedBy = "Society Secretary",
                isPinned = priority == "URGENT",
                targetAudience = targetAudience
            )
            repository.postNotice(notice)
            _snackbarMessage.value = "Notice published to community board"
        }
    }

    fun deleteNotice(id: Long) {
        viewModelScope.launch {
            repository.deleteNotice(id)
            _snackbarMessage.value = "Notice removed"
        }
    }

    fun uploadDocument(
        title: String,
        category: String,
        summary: String,
        sections: String,
        version: String
    ) {
        viewModelScope.launch {
            val doc = SocietyDocument(
                title = title,
                category = category,
                fileType = "PDF",
                fileSize = "1.5 MB",
                version = version,
                summary = summary,
                contentSections = sections,
                downloadCount = 1
            )
            repository.addDocument(doc)
            _snackbarMessage.value = "Document published to repository"
        }
    }

    // --- Notifications & SOS ---
    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            _snackbarMessage.value = "All notifications marked as read"
        }
    }

    fun triggerEmergencySOS(sosType: String, remarks: String = "") {
        viewModelScope.launch {
            val flat = _currentResidentFlat.value
            val resident = _currentResidentName.value
            repository.triggerEmergencySOS(flat, resident, sosType, remarks.ifBlank { "Immediate security assistance requested" })
            _snackbarMessage.value = "🚨 EMERGENCY ALERT SENT TO GATE SECURITY DESK & PROPERTY MANAGER!"
        }
    }

    // --- Vehicle Registry & Plate Cross-Referencing ---
    fun checkPlateNumber(plate: String) {
        val clean = ResidentVehicle.normalizePlate(plate)
        if (clean.isBlank()) {
            _livePlateCheckResult.value = null
            return
        }
        viewModelScope.launch {
            val matchedVehicle = repository.findVehicleByPlate(clean)
            val matchedVisitor = allVisitors.value.firstOrNull {
                ResidentVehicle.normalizePlate(it.vehicleNumber) == clean
            }
            _livePlateCheckResult.value = PlateCheckResult(
                queryPlate = plate.trim().uppercase(),
                isMatch = matchedVehicle != null,
                vehicle = matchedVehicle,
                matchedVisitorEntry = matchedVisitor
            )
        }
    }

    fun clearPlateCheck() {
        _livePlateCheckResult.value = null
    }

    fun registerResidentVehicle(
        plateNumber: String,
        vehicleType: String,
        makeModel: String,
        color: String,
        tower: String,
        flatNumber: String,
        ownerName: String,
        ownerPhone: String,
        parkingSlot: String,
        stickerNumber: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val clean = ResidentVehicle.normalizePlate(plateNumber)
            val existing = repository.findVehicleByPlate(plateNumber)
            if (existing != null) {
                _snackbarMessage.value = "Plate $plateNumber is already registered to Flat ${existing.flatNumber} (${existing.ownerName})"
                return@launch
            }
            val generatedSticker = stickerNumber.ifBlank {
                "SOC-${tower.takeLast(1)}-${flatNumber.replace("-", "")}-${(1000..9999).random()}"
            }
            val vehicle = ResidentVehicle(
                plateNumber = plateNumber.trim().uppercase(),
                cleanPlate = clean,
                vehicleType = vehicleType,
                makeModel = makeModel.trim(),
                color = color.trim().ifBlank { "White" },
                tower = tower,
                flatNumber = flatNumber,
                ownerName = ownerName.trim(),
                ownerPhone = ownerPhone.trim(),
                parkingSlot = parkingSlot.trim().ifBlank { "Unallocated" },
                stickerNumber = generatedSticker,
                notes = notes.trim()
            )
            repository.registerVehicle(vehicle)
            _snackbarMessage.value = "Vehicle ${vehicle.plateNumber} added to registry! Sticker: $generatedSticker"
        }
    }

    fun deleteVehicle(id: Long) {
        viewModelScope.launch {
            repository.deleteVehicle(id)
            _snackbarMessage.value = "Vehicle removed from society registry"
        }
    }

    // --- Resident Profile & Contact Management ---
    fun updateResidentProfile(profile: ResidentProfile) {
        viewModelScope.launch {
            _isProcessing.value = true
            repository.saveResidentProfile(profile)
            _currentResidentName.value = profile.fullName
            _isProcessing.value = false
            _snackbarMessage.value = "✓ Flat ${profile.flatNumber} profile updated & saved to local database"
        }
    }

    fun sendTestEmergencyBroadcast(flatNumber: String, customMessage: String? = null) {
        viewModelScope.launch {
            val profile = repository.getResidentProfileOnce(flatNumber)
            val name = profile?.fullName ?: _currentResidentName.value
            val phone = profile?.primaryPhone ?: "+91 98201 12345"
            val emergencyContactNote = if (profile != null && profile.emergencyContactName.isNotBlank()) {
                " Emergency Contact: ${profile.emergencyContactName} (${profile.emergencyContactPhone} • ${profile.emergencyContactRelation})."
            } else ""
            val msg = customMessage ?: "This is a priority emergency broadcast test for Flat $flatNumber.$emergencyContactNote All emergency response systems operational."

            notificationService.sendEmergencyBroadcast(
                flatNumber = flatNumber,
                residentName = name,
                residentPhone = phone,
                title = "🚨 Emergency Broadcast Test",
                messageText = msg
            )
            repository.addNotification(
                GateNotification(
                    title = "🚨 Emergency Broadcast Test",
                    message = msg,
                    type = "EMERGENCY",
                    flatNumber = flatNumber,
                    priority = "HIGH"
                )
            )
            _snackbarMessage.value = "Emergency broadcast dispatched to $phone"
        }
    }
}

data class PlateCheckResult(
    val queryPlate: String,
    val isMatch: Boolean,
    val vehicle: ResidentVehicle? = null,
    val matchedVisitorEntry: VisitorEntry? = null,
    val checkedAt: Long = System.currentTimeMillis()
)

