package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "society_documents")
data class SocietyDocument(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String, // RULEBOOK, BYE_LAWS, FINANCIALS, SAFETY, PARKING
    val fileType: String = "PDF",
    val fileSize: String = "1.2 MB",
    val uploadDate: Long = System.currentTimeMillis(),
    val version: String = "v2.0",
    val summary: String,
    val contentSections: String, // Structured rules / chapters text
    val downloadCount: Int = 18
)
