package com.example.data.repository

import com.example.data.dao.SocietyDao
import com.example.data.entity.GateNotification
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.Notice
import com.example.data.entity.ResidentProfile
import com.example.data.entity.ResidentVehicle
import com.example.data.entity.SocietyDocument
import com.example.data.entity.VisitorEntry
import com.example.service.payment.MockPaymentGatewayService
import com.example.service.payment.PaymentTransactionRequest
import com.example.service.payment.PaymentTransactionResult
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SocietyRepository(private val dao: SocietyDao) {

    // --- Visitors ---
    val allVisitors: Flow<List<VisitorEntry>> = dao.getAllVisitors()
    val insideVisitors: Flow<List<VisitorEntry>> = dao.getInsideVisitors()

    fun getRecentVisitors(limit: Int = 30): Flow<List<VisitorEntry>> = dao.getRecentVisitors(limit)

    fun getVisitorsForFlat(flat: String): Flow<List<VisitorEntry>> = dao.getVisitorsForFlat(flat)

    suspend fun getVisitorById(id: Long): VisitorEntry? = dao.getVisitorById(id)

    suspend fun registerVisitor(entry: VisitorEntry): Long {
        val uniqueToken = if (entry.qrCodeToken.isNotBlank()) entry.qrCodeToken
        else "GATE-PASS-${System.currentTimeMillis()}-${kotlin.random.Random.nextInt(1000, 9999)}"
        val finalEntry = entry.copy(qrCodeToken = uniqueToken)
        val id = dao.insertVisitor(finalEntry)
        // Auto-generate a notification for the resident
        dao.insertNotification(
            GateNotification(
                title = "Visitor Alert: ${finalEntry.visitorName}",
                message = "${finalEntry.visitorName} (${finalEntry.purpose}) has arrived for flat ${finalEntry.flatNumber}. Entry QR code generated.",
                type = "VISITOR",
                flatNumber = finalEntry.flatNumber,
                priority = if (finalEntry.status == "WAITING_APPROVAL") "HIGH" else "NORMAL"
            )
        )
        return id
    }

    suspend fun getVisitorByQr(token: String): VisitorEntry? {
        val trimmed = token.trim()
        return dao.getVisitorByQrToken(trimmed)
            ?: dao.getVisitorByPassCode(trimmed)
            ?: trimmed.removePrefix("GATE-PASS-").split("-").firstOrNull()?.toLongOrNull()?.let { dao.getVisitorById(it) }
    }

    suspend fun processVisitorExitByQr(token: String): VisitorEntry? {
        val visitor = getVisitorByQr(token) ?: return null
        val exitTime = System.currentTimeMillis()
        dao.updateVisitorStatus(visitor.id, "EXITED", exitTime)
        dao.insertNotification(
            GateNotification(
                title = "Visitor Departed: ${visitor.visitorName}",
                message = "${visitor.visitorName} (${visitor.visitorType}) scanned QR code and exited via gate logout terminal.",
                type = "VISITOR",
                flatNumber = visitor.flatNumber,
                priority = "NORMAL"
            )
        )
        return visitor.copy(status = "EXITED", exitTimestamp = exitTime)
    }

    suspend fun approveVisitor(id: Long) {
        dao.updateVisitorStatus(id, "APPROVED")
    }

    suspend fun checkInVisitor(id: Long) {
        dao.updateVisitorStatus(id, "INSIDE")
    }

    suspend fun assignVisitorParkingSlot(id: Long, slot: String) {
        dao.updateVisitorParkingSlot(id, slot)
    }

    suspend fun releaseVisitorParkingSlot(id: Long) {
        dao.updateVisitorParkingSlot(id, "")
    }

    suspend fun updateVisitorPhoto(id: Long, photoUri: String) {
        dao.updateVisitorPhoto(id, photoUri)
    }

    suspend fun checkOutVisitor(id: Long) {
        dao.updateVisitorStatus(id, "EXITED", System.currentTimeMillis())
    }

    suspend fun denyVisitor(id: Long) {
        dao.updateVisitorStatus(id, "DENIED", System.currentTimeMillis())
    }

    // --- Maintenance Billing ---
    val allBills: Flow<List<MaintenanceBill>> = dao.getAllBills()

    fun getBillsForFlat(flat: String): Flow<List<MaintenanceBill>> = dao.getBillsForFlat(flat)

    fun getBillsForTower(tower: String): Flow<List<MaintenanceBill>> = dao.getBillsForTower(tower)

    suspend fun processPayment(billId: Long, gateway: String): String {
        val ref = "TXN" + UUID.randomUUID().toString().take(10).uppercase()
        val receiptNo = "REC-SG-" + System.currentTimeMillis().toString().takeLast(6)
        val now = System.currentTimeMillis()
        dao.markBillAsPaid(
            id = billId,
            gateway = gateway,
            transactionRef = ref,
            receiptNumber = receiptNo,
            paidDate = now
        )
        // Add confirmation notification
        dao.insertNotification(
            GateNotification(
                title = "Payment Successful ($gateway)",
                message = "Maintenance payment of bill #$billId processed successfully. Receipt: $receiptNo",
                type = "BILLING",
                priority = "NORMAL"
            )
        )
        return receiptNo
    }

    suspend fun executeGatewayPayment(
        request: PaymentTransactionRequest,
        onStageUpdate: ((String) -> Unit)? = null
    ): PaymentTransactionResult {
        val result = MockPaymentGatewayService.executePayment(request, onStageUpdate)
        if (result.isSuccess) {
            dao.markBillAsPaid(
                id = request.billId,
                gateway = result.gatewayProvider,
                transactionRef = result.transactionId,
                receiptNumber = result.receiptNumber,
                paidDate = result.timestamp
            )
            dao.insertNotification(
                GateNotification(
                    title = "Payment Success: ${result.receiptNumber}",
                    message = "Maintenance fee for ${request.monthYear} (Flat ${request.flatNumber}) paid via ${result.gatewayProvider}. Txn ID: ${result.transactionId}",
                    type = "BILLING",
                    flatNumber = request.flatNumber,
                    priority = "HIGH"
                )
            )
        }
        return result
    }

    suspend fun generateMonthlyBilling(monthYear: String) {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val sampleTowers = listOf(
            Triple("Tower A", "A-101", "Chandan Kumar"),
            Triple("Tower A", "A-102", "Sunil Narang"),
            Triple("Tower A", "A-201", "Neha Agarwal"),
            Triple("Tower B", "B-204", "Priya Sharma"),
            Triple("Tower B", "B-103", "Kavita Deshmukh"),
            Triple("Tower C", "C-302", "Dr. Ananya Roy"),
            Triple("Tower C", "C-105", "Rajesh Gopinath"),
            Triple("Tower D", "D-401", "Vikramaditya Rao"),
            Triple("Tower D", "D-202", "Arjun Kapoor")
        )

        val newBills = sampleTowers.map { (tower, flat, name) ->
            val base = if (tower.contains("D")) 4200.0 else if (tower.contains("C")) 3800.0 else 3200.0
            val sinking = base * 0.15
            val water = 450.0
            val parking = 350.0
            val clubhouse = 250.0
            MaintenanceBill(
                tower = tower,
                flatNumber = flat,
                residentName = name,
                monthYear = monthYear,
                baseMaintenance = base,
                sinkingFund = sinking,
                waterSewerage = water,
                parkingCharges = parking,
                clubhouseFee = clubhouse,
                totalAmount = base + sinking + water + parking + clubhouse,
                dueDate = now + (15 * dayMs),
                status = "PENDING"
            )
        }
        dao.insertBills(newBills)
        dao.insertNotification(
            GateNotification(
                title = "Automated Billing Generated: $monthYear",
                message = "Monthly maintenance invoices have been created for all towers. Residents notified.",
                type = "BILLING",
                priority = "HIGH"
            )
        )
    }

    suspend fun dispatchDueReminders(tower: String? = null) {
        val target = tower ?: "All Towers"
        dao.insertNotification(
            GateNotification(
                title = "Payment Reminder Dispatched",
                message = "Automated SMS & App push reminder sent to all outstanding accounts in $target.",
                type = "BILLING",
                priority = "NORMAL"
            )
        )
    }

    // --- Notices ---
    val allNotices: Flow<List<Notice>> = dao.getAllNotices()

    suspend fun postNotice(notice: Notice) {
        dao.insertNotice(notice)
        dao.insertNotification(
            GateNotification(
                title = "New Notice: ${notice.title}",
                message = notice.description.take(90) + "...",
                type = "NOTICE",
                priority = if (notice.priority == "URGENT") "HIGH" else "NORMAL"
            )
        )
    }

    suspend fun deleteNotice(id: Long) {
        dao.deleteNotice(id)
    }

    // --- Documents ---
    val allDocuments: Flow<List<SocietyDocument>> = dao.getAllDocuments()

    suspend fun addDocument(doc: SocietyDocument) {
        dao.insertDocument(doc)
    }

    suspend fun recordDownload(id: Long) {
        dao.incrementDownloadCount(id)
    }

    // --- Notifications & Emergency ---
    val allNotifications: Flow<List<GateNotification>> = dao.getAllNotifications()

    suspend fun addNotification(notification: GateNotification): Long {
        return dao.insertNotification(notification)
    }

    suspend fun markNotificationRead(id: Long) {
        dao.markNotificationAsRead(id)
    }

    suspend fun markAllNotificationsRead() {
        dao.markAllNotificationsAsRead()
    }

    suspend fun triggerEmergencySOS(
        flatNumber: String,
        residentName: String,
        sosType: String,
        details: String
    ) {
        val profile = dao.getResidentProfileOnce(flatNumber)
        val emergencyContactInfo = if (profile != null && profile.emergencyContactName.isNotBlank()) {
            "\nEmergency Contact: ${profile.emergencyContactName} (${profile.emergencyContactPhone} • ${profile.emergencyContactRelation}) | Blood Group: ${profile.bloodGroup}"
        } else ""

        val emergencyNotice = Notice(
            title = "EMERGENCY ALERT: $sosType ($flatNumber)",
            description = "Resident $residentName ($flatNumber) raised an emergency alert: $details.$emergencyContactInfo Security & Emergency Response deployed immediately.",
            category = "EMERGENCY",
            priority = "URGENT",
            postedBy = "Gate Emergency Desk",
            isPinned = true
        )
        dao.insertNotice(emergencyNotice)

        dao.insertNotification(
            GateNotification(
                title = "🚨 SOS ALERT: $sosType from $flatNumber",
                message = "$residentName ($flatNumber) triggered $sosType alert!$emergencyContactInfo Gate Personnel alerted.",
                type = "EMERGENCY",
                flatNumber = flatNumber,
                priority = "EMERGENCY"
            )
        )
    }

    // --- Resident Profiles & Apartment Management ---
    fun getResidentProfile(flatNumber: String): Flow<ResidentProfile?> = dao.getResidentProfile(flatNumber)

    suspend fun getResidentProfileOnce(flatNumber: String): ResidentProfile? = dao.getResidentProfileOnce(flatNumber)

    fun getAllResidentProfiles(): Flow<List<ResidentProfile>> = dao.getAllResidentProfiles()

    suspend fun saveResidentProfile(profile: ResidentProfile) {
        dao.insertOrUpdateProfile(profile)
        dao.insertNotification(
            GateNotification(
                title = "Profile Updated: Flat ${profile.flatNumber}",
                message = "${profile.fullName} updated contact (${profile.primaryPhone}) and emergency details for Flat ${profile.flatNumber}.",
                type = "NOTICE",
                flatNumber = profile.flatNumber,
                priority = "NORMAL"
            )
        )
    }

    // --- Resident Vehicles & Gate Cross-Referencing ---
    val allVehicles: Flow<List<ResidentVehicle>> = dao.getAllVehicles()

    fun getVehiclesForFlat(flat: String): Flow<List<ResidentVehicle>> {
        return dao.getVehiclesForFlat(flat)
    }

    fun searchVehicles(query: String): Flow<List<ResidentVehicle>> {
        return dao.searchVehicles(query)
    }

    suspend fun findVehicleByPlate(plateNumber: String): ResidentVehicle? {
        val clean = ResidentVehicle.normalizePlate(plateNumber)
        if (clean.isBlank()) return null
        return dao.findVehicleByCleanPlate(clean)
    }

    suspend fun registerVehicle(vehicle: ResidentVehicle): Long {
        return dao.insertVehicle(vehicle)
    }

    suspend fun updateVehicle(vehicle: ResidentVehicle) {
        dao.updateVehicle(vehicle)
    }

    suspend fun deleteVehicle(id: Long) {
        dao.deleteVehicle(id)
    }
}
