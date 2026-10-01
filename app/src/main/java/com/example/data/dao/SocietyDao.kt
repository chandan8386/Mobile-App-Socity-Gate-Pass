package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.GateNotification
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.Notice
import com.example.data.entity.ResidentProfile
import com.example.data.entity.ResidentVehicle
import com.example.data.entity.SocietyDocument
import com.example.data.entity.VisitorEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SocietyDao {

    // --- Visitor Operations ---
    @Query("SELECT * FROM visitor_entries ORDER BY timestamp DESC")
    fun getAllVisitors(): Flow<List<VisitorEntry>>

    @Query("SELECT * FROM visitor_entries ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentVisitors(limit: Int = 30): Flow<List<VisitorEntry>>

    @Query("SELECT * FROM visitor_entries WHERE flat_number = :flat ORDER BY timestamp DESC")
    fun getVisitorsForFlat(flat: String): Flow<List<VisitorEntry>>

    @Query("SELECT * FROM visitor_entries WHERE status = 'INSIDE' ORDER BY timestamp DESC")
    fun getInsideVisitors(): Flow<List<VisitorEntry>>

    @Query("SELECT * FROM visitor_entries WHERE status = 'INSIDE'")
    suspend fun getInsideVisitorsList(): List<VisitorEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitor(entry: VisitorEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitors(entries: List<VisitorEntry>)

    @Update
    suspend fun updateVisitor(entry: VisitorEntry)

    @Query("SELECT * FROM visitor_entries WHERE id = :id LIMIT 1")
    suspend fun getVisitorById(id: Long): VisitorEntry?

    @Query("SELECT * FROM visitor_entries WHERE qr_code_token = :token LIMIT 1")
    suspend fun getVisitorByQrToken(token: String): VisitorEntry?

    @Query("SELECT * FROM visitor_entries WHERE pass_code = :passCode LIMIT 1")
    suspend fun getVisitorByPassCode(passCode: String): VisitorEntry?

    @Query("UPDATE visitor_entries SET status = :status, exit_timestamp = :exitTime WHERE id = :id")
    suspend fun updateVisitorStatus(id: Long, status: String, exitTime: Long? = null)

    @Query("UPDATE visitor_entries SET parking_slot = :slot WHERE id = :id")
    suspend fun updateVisitorParkingSlot(id: Long, slot: String)

    @Query("UPDATE visitor_entries SET photo_uri = :photoUri WHERE id = :id")
    suspend fun updateVisitorPhoto(id: Long, photoUri: String)

    @Query("DELETE FROM visitor_entries WHERE id = :id")
    suspend fun deleteVisitor(id: Long)

    // --- Maintenance Billing Operations ---
    @Query("SELECT * FROM maintenance_bills ORDER BY id DESC")
    fun getAllBills(): Flow<List<MaintenanceBill>>

    @Query("SELECT * FROM maintenance_bills WHERE flatNumber = :flat ORDER BY dueDate DESC")
    fun getBillsForFlat(flat: String): Flow<List<MaintenanceBill>>

    @Query("SELECT * FROM maintenance_bills WHERE tower = :tower ORDER BY flatNumber ASC")
    fun getBillsForTower(tower: String): Flow<List<MaintenanceBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: MaintenanceBill): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBills(bills: List<MaintenanceBill>)

    @Update
    suspend fun updateBill(bill: MaintenanceBill)

    @Query("""
        UPDATE maintenance_bills 
        SET status = 'PAID', paidDate = :paidDate, paymentGateway = :gateway, 
            transactionRef = :transactionRef, receiptNumber = :receiptNumber 
        WHERE id = :id
    """)
    suspend fun markBillAsPaid(
        id: Long,
        gateway: String,
        transactionRef: String,
        receiptNumber: String,
        paidDate: Long = System.currentTimeMillis()
    )

    // --- Digital Notices Operations ---
    @Query("SELECT * FROM notices ORDER BY isPinned DESC, postedDate DESC")
    fun getAllNotices(): Flow<List<Notice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotice(notice: Notice): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<Notice>)

    @Query("DELETE FROM notices WHERE id = :id")
    suspend fun deleteNotice(id: Long)

    // --- Document Repository Operations ---
    @Query("SELECT * FROM society_documents ORDER BY uploadDate DESC")
    fun getAllDocuments(): Flow<List<SocietyDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: SocietyDocument): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(documents: List<SocietyDocument>)

    @Query("UPDATE society_documents SET downloadCount = downloadCount + 1 WHERE id = :id")
    suspend fun incrementDownloadCount(id: Long)

    // --- Notification Alerts Operations ---
    @Query("SELECT * FROM gate_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<GateNotification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: GateNotification): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<GateNotification>)

    @Query("UPDATE gate_notifications SET isRead = 1 WHERE id = :id")
    suspend fun markNotificationAsRead(id: Long)

    @Query("UPDATE gate_notifications SET isRead = 1")
    suspend fun markAllNotificationsAsRead()

    @Query("DELETE FROM gate_notifications")
    suspend fun clearAllNotifications()

    // --- Resident Vehicle Operations ---
    @Query("SELECT * FROM resident_vehicles ORDER BY tower ASC, flat_number ASC")
    fun getAllVehicles(): Flow<List<ResidentVehicle>>

    @Query("SELECT * FROM resident_vehicles WHERE flat_number = :flat ORDER BY id DESC")
    fun getVehiclesForFlat(flat: String): Flow<List<ResidentVehicle>>

    @Query("SELECT * FROM resident_vehicles WHERE clean_plate = :cleanPlate LIMIT 1")
    suspend fun findVehicleByCleanPlate(cleanPlate: String): ResidentVehicle?

    @Query("SELECT * FROM resident_vehicles WHERE plate_number LIKE '%' || :query || '%' OR clean_plate LIKE '%' || :query || '%' OR owner_name LIKE '%' || :query || '%' OR flat_number LIKE '%' || :query || '%'")
    fun searchVehicles(query: String): Flow<List<ResidentVehicle>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: ResidentVehicle): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicles(vehicles: List<ResidentVehicle>)

    @Update
    suspend fun updateVehicle(vehicle: ResidentVehicle)

    @Query("DELETE FROM resident_vehicles WHERE id = :id")
    suspend fun deleteVehicle(id: Long)

    @Query("SELECT COUNT(*) FROM resident_vehicles")
    suspend fun countVehicles(): Int

    // --- Resident Profile Operations ---
    @Query("SELECT * FROM resident_profiles WHERE flatNumber = :flatNumber LIMIT 1")
    fun getResidentProfile(flatNumber: String): Flow<ResidentProfile?>

    @Query("SELECT * FROM resident_profiles WHERE flatNumber = :flatNumber LIMIT 1")
    suspend fun getResidentProfileOnce(flatNumber: String): ResidentProfile?

    @Query("SELECT * FROM resident_profiles ORDER BY tower ASC, flatNumber ASC")
    fun getAllResidentProfiles(): Flow<List<ResidentProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: ResidentProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<ResidentProfile>)

    @Query("DELETE FROM resident_profiles WHERE flatNumber = :flatNumber")
    suspend fun deleteProfile(flatNumber: String)
}
