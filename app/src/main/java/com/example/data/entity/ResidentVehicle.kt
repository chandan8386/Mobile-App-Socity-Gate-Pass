package com.example.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a registered resident vehicle in the society.
 * Used for parking management and gate security plate cross-referencing.
 */
@Entity(
    tableName = "resident_vehicles",
    indices = [
        Index(value = ["clean_plate"]),
        Index(value = ["flat_number"])
    ]
)
data class ResidentVehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "plate_number")
    val plateNumber: String, // e.g. "MH 02 CZ 4488"
    @ColumnInfo(name = "clean_plate")
    val cleanPlate: String, // Alphanumeric uppercase e.g. "MH02CZ4488" for fast cross-referencing
    @ColumnInfo(name = "vehicle_type")
    val vehicleType: String, // 4-Wheeler Car, 2-Wheeler Bike, EV Car, EV Scooter, Commercial
    @ColumnInfo(name = "make_model")
    val makeModel: String, // e.g. "Tata Nexon EV", "Honda City"
    @ColumnInfo(name = "color")
    val color: String = "White",
    @ColumnInfo(name = "tower")
    val tower: String = "Tower A",
    @ColumnInfo(name = "flat_number")
    val flatNumber: String = "A-101",
    @ColumnInfo(name = "owner_name")
    val ownerName: String = "Resident",
    @ColumnInfo(name = "owner_phone")
    val ownerPhone: String = "",
    @ColumnInfo(name = "parking_slot")
    val parkingSlot: String = "P-01",
    @ColumnInfo(name = "sticker_number")
    val stickerNumber: String = "",
    @ColumnInfo(name = "registered_at")
    val registeredAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "status")
    val status: String = "ACTIVE", // ACTIVE, INACTIVE, VISITOR_TEMP
    @ColumnInfo(name = "notes")
    val notes: String = ""
) {
    companion object {
        fun normalizePlate(plate: String): String {
            return plate.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
        }
    }
}
