package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_bills")
data class MaintenanceBill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tower: String, // Tower A, Tower B, Tower C, Tower D
    val flatNumber: String,
    val residentName: String,
    val monthYear: String, // "September 2026", "August 2026"
    val baseMaintenance: Double,
    val sinkingFund: Double,
    val waterSewerage: Double,
    val parkingCharges: Double,
    val clubhouseFee: Double,
    val totalAmount: Double,
    val dueDate: Long,
    val status: String = "PENDING", // PENDING, PAID, OVERDUE
    val paidDate: Long? = null,
    val paymentGateway: String? = null, // Razorpay, UPI, NetBanking, Credit Card
    val transactionRef: String? = null,
    val receiptNumber: String? = null
)
