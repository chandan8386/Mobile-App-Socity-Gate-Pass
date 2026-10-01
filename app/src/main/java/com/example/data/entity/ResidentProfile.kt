package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room Entity for resident profile data.
 * Used for personalized gate notifications, intercom directory,
 * SMS dispatch, and high-priority emergency broadcasts.
 */
@Entity(tableName = "resident_profiles")
data class ResidentProfile(
    @PrimaryKey
    val flatNumber: String, // Unique identifier e.g., "A-101"
    val fullName: String,
    val tower: String, // e.g., "Tower A"
    val primaryPhone: String,
    val alternatePhone: String = "",
    val email: String,
    val ownershipType: String = "OWNER", // "OWNER" or "TENANT"
    val moveInDate: String = "Jan 2023",
    val intercomExtension: String = "101",
    val parkingSlot: String = "B1-P12",
    val residentCount: Int = 3,
    val bloodGroup: String = "O+",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelation: String = "Family",
    val enableSmsAlerts: Boolean = true,
    val enablePushAlerts: Boolean = true,
    val enableVisitorArrivalAlerts: Boolean = true,
    val enableEmergencyBroadcasts: Boolean = true,
    val specialNotes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
