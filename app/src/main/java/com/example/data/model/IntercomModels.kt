package com.example.data.model

import java.util.UUID

enum class IntercomCallState {
    RINGING,
    CONNECTED,
    ENDED,
    DECLINED
}

data class IntercomResident(
    val flatNumber: String,
    val tower: String,
    val residentName: String,
    val phone: String,
    val intercomExtension: String,
    val isOnline: Boolean = true
) {
    companion object {
        val DIRECTORY = listOf(
            IntercomResident("A-101", "Tower A", "Chandan Kumar", "+91 98201 12345", "101"),
            IntercomResident("A-202", "Tower A", "Amit Verma", "+91 98205 11223", "102"),
            IntercomResident("B-103", "Tower B", "Kavita Deshmukh", "+91 98202 33445", "201"),
            IntercomResident("B-105", "Tower B", "Priya Mehta", "+91 98202 67890", "202"),
            IntercomResident("B-204", "Tower B", "Priya Sharma", "+91 98202 11223", "204"),
            IntercomResident("C-105", "Tower C", "Rajesh Gopinath", "+91 98203 77889", "301"),
            IntercomResident("C-301", "Tower C", "Vikram Malhotra", "+91 98203 54321", "302"),
            IntercomResident("C-302", "Tower C", "Dr. Ananya Roy", "+91 98203 99887", "303"),
            IntercomResident("D-402", "Tower D", "Ananya Desai", "+91 98204 98765", "401")
        )
    }
}

data class IntercomActiveCall(
    val callId: String = UUID.randomUUID().toString(),
    val callerRole: String, // "SECURITY_GUARD" or "RESIDENT"
    val callerName: String,
    val targetFlat: String,
    val targetResidentName: String,
    val visitorId: Long? = null,
    val visitorName: String? = null,
    val visitorType: String? = null,
    val visitorVehicle: String? = null,
    val callState: IntercomCallState = IntercomCallState.RINGING,
    val startTime: Long = System.currentTimeMillis(),
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true
)

data class IntercomMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderRole: String, // "GUARD" or "RESIDENT"
    val senderName: String,
    val targetFlat: String,
    val visitorId: Long? = null,
    val visitorName: String? = null,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val decision: String? = null // "APPROVED", "DENIED", "PARCEL_AT_GATE", null
)
