package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gate_notifications")
data class GateNotification(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: String, // VISITOR, BILLING, NOTICE, EMERGENCY, SECURITY
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val flatNumber: String? = null, // null means broadcast
    val priority: String = "NORMAL" // NORMAL, HIGH, EMERGENCY
)
