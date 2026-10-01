package com.example.data.model

/**
 * Model representing a Visitor Parking Slot in the society.
 * Used by security personnel to monitor parking bay availability and assign slots.
 */
data class VisitorParkingSlot(
    val slotNumber: String, // e.g. "V-01", "V-EV1", "V-2W1"
    val zone: String, // "Surface Clubhouse", "Basement B1", "EV Station", "Two-Wheeler"
    val slotType: String, // "4-Wheeler Car", "EV Fast Charging", "2-Wheeler"
    val isOccupied: Boolean = false,
    val visitorId: Long? = null,
    val visitorName: String? = null,
    val vehicleNumber: String? = null,
    val flatNumber: String? = null,
    val tower: String? = null,
    val occupiedSince: Long? = null
) {
    companion object {
        val ALL_DEFAULT_SLOTS = listOf(
            // Surface Clubhouse Zone (6 bays)
            VisitorParkingSlot("V-01", "Surface Clubhouse", "4-Wheeler Car"),
            VisitorParkingSlot("V-02", "Surface Clubhouse", "4-Wheeler Car"),
            VisitorParkingSlot("V-03", "Surface Clubhouse", "4-Wheeler Car"),
            VisitorParkingSlot("V-04", "Surface Clubhouse", "4-Wheeler Car"),
            VisitorParkingSlot("V-05", "Surface Clubhouse", "4-Wheeler Car"),
            VisitorParkingSlot("V-06", "Surface Clubhouse", "4-Wheeler Car"),

            // Basement B1 Zone (6 bays)
            VisitorParkingSlot("V-07", "Basement B1", "4-Wheeler Car"),
            VisitorParkingSlot("V-08", "Basement B1", "4-Wheeler Car"),
            VisitorParkingSlot("V-09", "Basement B1", "4-Wheeler Car"),
            VisitorParkingSlot("V-10", "Basement B1", "4-Wheeler Car"),
            VisitorParkingSlot("V-11", "Basement B1", "4-Wheeler Car"),
            VisitorParkingSlot("V-12", "Basement B1", "4-Wheeler Car"),

            // EV Fast Charging Station (3 bays)
            VisitorParkingSlot("V-EV1", "EV Station", "EV Fast Charging"),
            VisitorParkingSlot("V-EV2", "EV Station", "EV Fast Charging"),
            VisitorParkingSlot("V-EV3", "EV Station", "EV Fast Charging"),

            // Two-Wheeler Guest Bay (3 bays)
            VisitorParkingSlot("V-2W1", "Two-Wheeler", "2-Wheeler"),
            VisitorParkingSlot("V-2W2", "Two-Wheeler", "2-Wheeler"),
            VisitorParkingSlot("V-2W3", "Two-Wheeler", "2-Wheeler")
        )
    }
}
