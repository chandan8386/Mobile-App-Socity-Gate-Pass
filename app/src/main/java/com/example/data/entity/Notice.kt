package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notices")
data class Notice(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val category: String, // GENERAL, MAINTENANCE, SECURITY, EVENTS, EMERGENCY
    val priority: String = "NORMAL", // NORMAL, IMPORTANT, URGENT
    val postedDate: Long = System.currentTimeMillis(),
    val postedBy: String = "Society Management Committee",
    val isPinned: Boolean = false,
    val targetAudience: String = "All Residents"
)
