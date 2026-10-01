package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/**
 * Room database schema for visitor entry and exit logs.
 * Stores comprehensive audit records including timestamp, visitor name,
 * visitor phone number, and visitor type, along with exit timestamps, status,
 * and a unique QR code token for fast gate exit logout scanning.
 */
@Entity(tableName = "visitor_entries")
data class VisitorEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(), // Entry timestamp log
    @ColumnInfo(name = "exit_timestamp")
    val exitTimestamp: Long? = null, // Exit timestamp log
    @ColumnInfo(name = "visitor_name")
    val visitorName: String,
    @ColumnInfo(name = "visitor_phone_number")
    val visitorPhoneNumber: String,
    @ColumnInfo(name = "visitor_type")
    val visitorType: String, // Guest, Delivery, Cab / Ride, Service / Repair, Daily Help, Other
    @ColumnInfo(name = "visitor_company")
    val visitorCompany: String = "", // Amazon, Swiggy, Uber, Urban Company, Personal
    @ColumnInfo(name = "vehicle_number")
    val vehicleNumber: String = "",
    @ColumnInfo(name = "tower")
    val tower: String = "Tower A",
    @ColumnInfo(name = "flat_number")
    val flatNumber: String = "A-101",
    @ColumnInfo(name = "host_resident_name")
    val hostResidentName: String = "Resident",
    @ColumnInfo(name = "status")
    val status: String = "INSIDE", // WAITING_APPROVAL, APPROVED, INSIDE, EXITED, DENIED
    @ColumnInfo(name = "pass_code")
    val passCode: String = "",
    @ColumnInfo(name = "qr_code_token")
    val qrCodeToken: String = "", // Unique QR token for fast barcode exit scanning
    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null, // Stored local image path for security photo verification
    @ColumnInfo(name = "parking_slot")
    val parkingSlot: String = "", // Assigned visitor parking bay e.g. "V-01", "V-EV2"
    @ColumnInfo(name = "remarks")
    val remarks: String = ""
) {
    @get:Ignore
    val phone: String get() = visitorPhoneNumber

    @get:Ignore
    val purpose: String get() = visitorType

    @get:Ignore
    val entryTime: Long get() = timestamp

    @get:Ignore
    val exitTime: Long? get() = exitTimestamp

    @get:Ignore
    val effectiveQrToken: String
        get() = if (qrCodeToken.isNotBlank()) qrCodeToken else "GATE-PASS-${if (id != 0L) id else "NEW"}-${passCode.ifBlank { "0000" }}"
}
