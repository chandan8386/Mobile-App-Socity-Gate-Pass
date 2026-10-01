package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.SocietyDao
import com.example.data.entity.GateNotification
import com.example.data.entity.MaintenanceBill
import com.example.data.entity.Notice
import com.example.data.entity.ResidentProfile
import com.example.data.entity.ResidentVehicle
import com.example.data.entity.SocietyDocument
import com.example.data.entity.VisitorEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        VisitorEntry::class,
        MaintenanceBill::class,
        Notice::class,
        SocietyDocument::class,
        GateNotification::class,
        ResidentVehicle::class,
        ResidentProfile::class
    ],
    version = 6,
    exportSchema = false
)
abstract class SocietyDatabase : RoomDatabase() {

    abstract fun societyDao(): SocietyDao

    companion object {
        @Volatile
        private var INSTANCE: SocietyDatabase? = null

        fun getInstance(context: Context): SocietyDatabase {
            return getDatabase(context)
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        ): SocietyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SocietyDatabase::class.java,
                    "society_gate_database"
                )
                    .addCallback(SocietyDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class SocietyDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.societyDao())
                    }
                }
            }
        }

        private suspend fun populateInitialData(dao: SocietyDao) {
            val now = System.currentTimeMillis()
            val hourMs = 3600000L
            val dayMs = 86400000L

            // 1. Seed Visitors
            val initialVisitors = listOf(
                VisitorEntry(
                    visitorName = "Rahul Verma",
                    visitorPhoneNumber = "+91 98765 43210",
                    visitorType = "Guest",
                    visitorCompany = "Personal",
                    vehicleNumber = "KA-03-MB-2041",
                    tower = "Tower A",
                    flatNumber = "A-101",
                    hostResidentName = "Chandan Kumar",
                    timestamp = now - (30 * 60 * 1000L),
                    status = "INSIDE",
                    passCode = "4821",
                    qrCodeToken = "GATE-PASS-1-4821",
                    parkingSlot = "V-02",
                    remarks = "Family friend visiting for dinner"
                ),
                VisitorEntry(
                    visitorName = "Suresh Patil",
                    visitorPhoneNumber = "+91 98451 22334",
                    visitorType = "Delivery",
                    visitorCompany = "Amazon",
                    vehicleNumber = "KA-01-EQ-9812",
                    tower = "Tower A",
                    flatNumber = "A-101",
                    hostResidentName = "Chandan Kumar",
                    timestamp = now - (5 * 60 * 1000L),
                    status = "WAITING_APPROVAL",
                    passCode = "9134",
                    qrCodeToken = "GATE-PASS-2-9134",
                    parkingSlot = "",
                    remarks = "Package delivery - Otp verification requested"
                ),
                VisitorEntry(
                    visitorName = "Amitabh Sen",
                    visitorPhoneNumber = "+91 91234 56789",
                    visitorType = "Service / Repair",
                    visitorCompany = "Urban Company",
                    vehicleNumber = "MH-12-PQ-5520",
                    tower = "Tower B",
                    flatNumber = "B-204",
                    hostResidentName = "Priya Sharma",
                    timestamp = now - (2 * hourMs),
                    status = "INSIDE",
                    passCode = "3391",
                    qrCodeToken = "GATE-PASS-3-3391",
                    parkingSlot = "V-08",
                    remarks = "AC deep servicing"
                ),
                VisitorEntry(
                    visitorName = "Deepak Kumar",
                    visitorPhoneNumber = "+91 99887 66554",
                    visitorType = "Cab / Ride",
                    visitorCompany = "Uber",
                    vehicleNumber = "KA-51-Z-1002",
                    tower = "Tower C",
                    flatNumber = "C-302",
                    hostResidentName = "Dr. Ananya Roy",
                    timestamp = now - (4 * hourMs),
                    exitTimestamp = now - (3 * hourMs),
                    status = "EXITED",
                    passCode = "7721",
                    qrCodeToken = "GATE-PASS-4-7721",
                    remarks = "Drop off at lobby"
                ),
                VisitorEntry(
                    visitorName = "Lakshmi Devi",
                    visitorPhoneNumber = "+91 97654 32190",
                    visitorType = "Daily Help",
                    visitorCompany = "Housekeeping",
                    vehicleNumber = "Walking",
                    tower = "Tower D",
                    flatNumber = "D-401",
                    hostResidentName = "Vikramaditya Rao",
                    timestamp = now - (5 * hourMs),
                    exitTimestamp = now - (2 * hourMs),
                    status = "EXITED",
                    passCode = "1098",
                    qrCodeToken = "GATE-PASS-5-1098",
                    remarks = "Daily maid morning shift"
                ),
                VisitorEntry(
                    visitorName = "Karan Sharma",
                    visitorPhoneNumber = "+91 98111 22334",
                    visitorType = "Delivery",
                    visitorCompany = "Swiggy",
                    vehicleNumber = "KA-05-HJ-3321",
                    tower = "Tower B",
                    flatNumber = "B-204",
                    hostResidentName = "Priya Sharma",
                    timestamp = now - (50 * 60 * 1000L),
                    exitTimestamp = now - (32 * 60 * 1000L),
                    status = "EXITED",
                    passCode = "6612",
                    qrCodeToken = "GATE-PASS-6-6612",
                    remarks = "Food delivery at flat door"
                ),
                VisitorEntry(
                    visitorName = "Rajesh Electrician",
                    visitorPhoneNumber = "+91 99345 67812",
                    visitorType = "Service / Repair",
                    visitorCompany = "Urban Company",
                    vehicleNumber = "KA-04-E-8890",
                    tower = "Tower C",
                    flatNumber = "C-302",
                    hostResidentName = "Dr. Ananya Roy",
                    timestamp = now - (80 * 60 * 1000L),
                    status = "INSIDE",
                    passCode = "2490",
                    qrCodeToken = "GATE-PASS-7-2490",
                    remarks = "Water purifier filter replacement"
                ),
                VisitorEntry(
                    visitorName = "Sneha Kapur",
                    visitorPhoneNumber = "+91 98777 55443",
                    visitorType = "Cab / Ride",
                    visitorCompany = "Ola",
                    vehicleNumber = "KA-01-MJ-9912",
                    tower = "Tower A",
                    flatNumber = "A-101",
                    hostResidentName = "Chandan Kumar",
                    timestamp = now - (25 * 60 * 1000L),
                    exitTimestamp = now - (15 * 60 * 1000L),
                    status = "EXITED",
                    passCode = "8831",
                    qrCodeToken = "GATE-PASS-8-8831",
                    remarks = "Airport pickup drop"
                ),
                VisitorEntry(
                    visitorName = "Sunita Bai",
                    visitorPhoneNumber = "+91 91223 34455",
                    visitorType = "Daily Help",
                    visitorCompany = "Housekeeping",
                    vehicleNumber = "Walking",
                    tower = "Tower B",
                    flatNumber = "B-204",
                    hostResidentName = "Priya Sharma",
                    timestamp = now - (40 * 60 * 1000L),
                    status = "INSIDE",
                    passCode = "4419",
                    qrCodeToken = "GATE-PASS-9-4419",
                    remarks = "Evening cleaning shift"
                ),
                VisitorEntry(
                    visitorName = "Vikram Oberoi",
                    visitorPhoneNumber = "+91 98888 12345",
                    visitorType = "Guest",
                    visitorCompany = "Personal",
                    vehicleNumber = "DL-01-AB-7766",
                    tower = "Tower D",
                    flatNumber = "D-401",
                    hostResidentName = "Vikramaditya Rao",
                    timestamp = now - (6 * hourMs),
                    exitTimestamp = now - (3 * hourMs),
                    status = "EXITED",
                    passCode = "5512",
                    qrCodeToken = "GATE-PASS-10-5512",
                    remarks = "Weekend family visit"
                )
            )
            dao.insertVisitors(initialVisitors)

            // 2. Seed Maintenance Bills (Tower-wise collection)
            val initialBills = listOf(
                // Tower A
                MaintenanceBill(
                    tower = "Tower A",
                    flatNumber = "A-101",
                    residentName = "Chandan Kumar",
                    monthYear = "September 2026",
                    baseMaintenance = 3200.0,
                    sinkingFund = 500.0,
                    waterSewerage = 450.0,
                    parkingCharges = 350.0,
                    clubhouseFee = 250.0,
                    totalAmount = 4750.0,
                    dueDate = now + (5 * dayMs),
                    status = "PENDING"
                ),
                MaintenanceBill(
                    tower = "Tower A",
                    flatNumber = "A-101",
                    residentName = "Chandan Kumar",
                    monthYear = "August 2026",
                    baseMaintenance = 3200.0,
                    sinkingFund = 500.0,
                    waterSewerage = 420.0,
                    parkingCharges = 350.0,
                    clubhouseFee = 250.0,
                    totalAmount = 4720.0,
                    dueDate = now - (25 * dayMs),
                    status = "PAID",
                    paidDate = now - (28 * dayMs),
                    paymentGateway = "UPI - GPay",
                    transactionRef = "UPI982310492811",
                    receiptNumber = "RCP-2026-08-A101"
                ),
                MaintenanceBill(
                    tower = "Tower A",
                    flatNumber = "A-102",
                    residentName = "Sunil Narang",
                    monthYear = "September 2026",
                    baseMaintenance = 3200.0,
                    sinkingFund = 500.0,
                    waterSewerage = 400.0,
                    parkingCharges = 350.0,
                    clubhouseFee = 250.0,
                    totalAmount = 4700.0,
                    dueDate = now + (5 * dayMs),
                    status = "PAID",
                    paidDate = now - (2 * dayMs),
                    paymentGateway = "Razorpay",
                    transactionRef = "pay_Oq7K92mXza1",
                    receiptNumber = "RCP-2026-09-A102"
                ),
                MaintenanceBill(
                    tower = "Tower A",
                    flatNumber = "A-201",
                    residentName = "Neha Agarwal",
                    monthYear = "September 2026",
                    baseMaintenance = 3200.0,
                    sinkingFund = 500.0,
                    waterSewerage = 450.0,
                    parkingCharges = 350.0,
                    clubhouseFee = 250.0,
                    totalAmount = 4750.0,
                    dueDate = now - (10 * dayMs),
                    status = "OVERDUE"
                ),
                // Tower B
                MaintenanceBill(
                    tower = "Tower B",
                    flatNumber = "B-204",
                    residentName = "Priya Sharma",
                    monthYear = "September 2026",
                    baseMaintenance = 3500.0,
                    sinkingFund = 600.0,
                    waterSewerage = 480.0,
                    parkingCharges = 400.0,
                    clubhouseFee = 300.0,
                    totalAmount = 5280.0,
                    dueDate = now + (5 * dayMs),
                    status = "PAID",
                    paidDate = now - (1 * dayMs),
                    paymentGateway = "NetBanking - HDFC",
                    transactionRef = "HDFC0091823101",
                    receiptNumber = "RCP-2026-09-B204"
                ),
                MaintenanceBill(
                    tower = "Tower B",
                    flatNumber = "B-103",
                    residentName = "Kavita Deshmukh",
                    monthYear = "September 2026",
                    baseMaintenance = 3500.0,
                    sinkingFund = 600.0,
                    waterSewerage = 480.0,
                    parkingCharges = 400.0,
                    clubhouseFee = 300.0,
                    totalAmount = 5280.0,
                    dueDate = now - (3 * dayMs),
                    status = "OVERDUE"
                ),
                // Tower C
                MaintenanceBill(
                    tower = "Tower C",
                    flatNumber = "C-302",
                    residentName = "Dr. Ananya Roy",
                    monthYear = "September 2026",
                    baseMaintenance = 3800.0,
                    sinkingFund = 650.0,
                    waterSewerage = 500.0,
                    parkingCharges = 450.0,
                    clubhouseFee = 300.0,
                    totalAmount = 5700.0,
                    dueDate = now + (5 * dayMs),
                    status = "PENDING"
                ),
                MaintenanceBill(
                    tower = "Tower C",
                    flatNumber = "C-105",
                    residentName = "Rajesh Gopinath",
                    monthYear = "September 2026",
                    baseMaintenance = 3800.0,
                    sinkingFund = 650.0,
                    waterSewerage = 500.0,
                    parkingCharges = 450.0,
                    clubhouseFee = 300.0,
                    totalAmount = 5700.0,
                    dueDate = now + (5 * dayMs),
                    status = "PAID",
                    paidDate = now - (3 * dayMs),
                    paymentGateway = "Razorpay",
                    transactionRef = "pay_P8xY4wQz19",
                    receiptNumber = "RCP-2026-09-C105"
                ),
                // Tower D
                MaintenanceBill(
                    tower = "Tower D",
                    flatNumber = "D-401",
                    residentName = "Vikramaditya Rao",
                    monthYear = "September 2026",
                    baseMaintenance = 4200.0,
                    sinkingFund = 700.0,
                    waterSewerage = 550.0,
                    parkingCharges = 500.0,
                    clubhouseFee = 350.0,
                    totalAmount = 6300.0,
                    dueDate = now + (5 * dayMs),
                    status = "PAID",
                    paidDate = now - (4 * dayMs),
                    paymentGateway = "Credit Card - Visa",
                    transactionRef = "CC_AUTH_998412",
                    receiptNumber = "RCP-2026-09-D401"
                ),
                MaintenanceBill(
                    tower = "Tower D",
                    flatNumber = "D-202",
                    residentName = "Arjun Kapoor",
                    monthYear = "September 2026",
                    baseMaintenance = 4200.0,
                    sinkingFund = 700.0,
                    waterSewerage = 550.0,
                    parkingCharges = 500.0,
                    clubhouseFee = 350.0,
                    totalAmount = 6300.0,
                    dueDate = now - (8 * dayMs),
                    status = "OVERDUE"
                )
            )
            dao.insertBills(initialBills)

            // 3. Seed Digital Notices
            val initialNotices = listOf(
                Notice(
                    title = "Scheduled Water Supply Maintenance - Tower A & B",
                    description = "Overhead tank cleaning and valve replacements will be conducted this Saturday between 10:00 AM and 03:00 PM. Please store sufficient water in advance.",
                    category = "MAINTENANCE",
                    priority = "URGENT",
                    postedDate = now - (1 * dayMs),
                    postedBy = "Facility Engineering Dept",
                    isPinned = true,
                    targetAudience = "Tower A & Tower B"
                ),
                Notice(
                    title = "Annual General Meeting (AGM) Notice & Agenda",
                    description = "The 12th Annual General Body Meeting will be held on Sunday, October 18, 2026, at 10:30 AM in the Society Clubhouse Amphitheatre. Agenda includes annual financial audit approval and election of committee members.",
                    category = "EVENTS",
                    priority = "IMPORTANT",
                    postedDate = now - (2 * dayMs),
                    postedBy = "Society Secretary",
                    isPinned = true,
                    targetAudience = "All Residents"
                ),
                Notice(
                    title = "Upgraded RFID Gate Barrier & Visitor Verification",
                    description = "All delivery personnel and cab drivers must present an OTP / digital gate pass generated through the SocietyGate app. Gate boom barriers now operate automatically for tagged resident vehicles.",
                    category = "SECURITY",
                    priority = "IMPORTANT",
                    postedDate = now - (4 * dayMs),
                    postedBy = "Chief Security Officer",
                    isPinned = false,
                    targetAudience = "All Residents"
                ),
                Notice(
                    title = "Diwali Cultural Night & Children's Fancy Dress",
                    description = "Registrations are now open for society cultural performances, food stalls, and kids fancy dress competition. Contact the Cultural Committee desk before Oct 5.",
                    category = "GENERAL",
                    priority = "NORMAL",
                    postedDate = now - (6 * dayMs),
                    postedBy = "Cultural Committee",
                    isPinned = false,
                    targetAudience = "All Residents"
                )
            )
            dao.insertNotices(initialNotices)

            // 4. Seed Documents Repository
            val initialDocuments = listOf(
                SocietyDocument(
                    title = "Society Bye-Laws & Governance Framework",
                    category = "BYE_LAWS",
                    fileType = "PDF",
                    fileSize = "2.4 MB",
                    uploadDate = now - (60 * dayMs),
                    version = "v3.1 (2026)",
                    summary = "Official registered constitution covering member voting rights, general body meeting procedures, common area jurisdiction, and election guidelines.",
                    contentSections = "1. Membership and Rights\n2. Common Amenities and Maintenance Allocation\n3. Committee Terms and Elections\n4. Dispute Resolution Process",
                    downloadCount = 84
                ),
                SocietyDocument(
                    title = "Resident Community Rulebook & Code of Conduct",
                    category = "RULEBOOK",
                    fileType = "PDF",
                    fileSize = "1.8 MB",
                    uploadDate = now - (40 * dayMs),
                    version = "v2.5",
                    summary = "Comprehensive guidelines covering quiet hours (10:30 PM - 6:30 AM), pet handling, interior renovation hours, balcony aesthetics, and garbage segregation standards.",
                    contentSections = "1. Quiet Hours and Noise Limits\n2. Pet Policy and Leash Requirements\n3. Renovation & Carpentry Hours (10 AM - 6 PM on weekdays)\n4. Solid Waste Segregation & Penalties",
                    downloadCount = 142
                ),
                SocietyDocument(
                    title = "Fire Safety & High-Rise Evacuation Protocols",
                    category = "SAFETY",
                    fileType = "PDF",
                    fileSize = "3.1 MB",
                    uploadDate = now - (20 * dayMs),
                    version = "v2.0",
                    summary = "Detailed floor-by-floor emergency escape routes, dry riser valve locations, fire alarm response procedures, and assembly points on the outer perimeter.",
                    contentSections = "1. Alarm Siren Codes\n2. Use of Refuge Areas (Floors 7, 14, 21)\n3. Lift Lockdown during Fire Drills\n4. Emergency Assembly Zones A, B & C",
                    downloadCount = 67
                ),
                SocietyDocument(
                    title = "Vehicle Parking & EV Charging Policy",
                    category = "PARKING",
                    fileType = "PDF",
                    fileSize = "1.1 MB",
                    uploadDate = now - (15 * dayMs),
                    version = "v1.8",
                    summary = "Designated basement parking rules, visitor guest bay permits, rules against double-parking, and guidelines for installing private EV charging sockets.",
                    contentSections = "1. Assigned Slot Numbering\n2. Guest Parking 4-hour Maximum Rule\n3. EV Charger Metering & Load Approvals\n4. Towing Charges for Unauthorized Vehicles",
                    downloadCount = 95
                ),
                SocietyDocument(
                    title = "Annual Financial Audit & Ledger Balance Sheet",
                    category = "FINANCIALS",
                    fileType = "PDF",
                    fileSize = "4.2 MB",
                    uploadDate = now - (5 * dayMs),
                    version = "FY 2025-26",
                    summary = "Audited financial statements by Chartered Accountants detailing maintenance collections, diesel generator fuel costs, security staff contracts, and sinking fund deposits.",
                    contentSections = "1. Revenue from Maintenance & Sinking Fund\n2. Capital Expenditures & Facility Maintenance\n3. Reserve Fund Balances\n4. Auditor's Certificate of Compliance",
                    downloadCount = 112
                )
            )
            dao.insertDocuments(initialDocuments)

            // 5. Seed Real-time Gate & Resident Notifications
            val initialNotifications = listOf(
                GateNotification(
                    title = "Visitor At Main Gate",
                    message = "Suresh Patil (Amazon Delivery) is requesting gate entry to Flat A-101. Passcode: 9134.",
                    type = "VISITOR",
                    timestamp = now - (5 * 60 * 1000L),
                    isRead = false,
                    flatNumber = "A-101",
                    priority = "HIGH"
                ),
                GateNotification(
                    title = "Maintenance Invoice Due",
                    message = "Your September 2026 maintenance invoice of ₹4,750 is due in 5 days.",
                    type = "BILLING",
                    timestamp = now - (2 * hourMs),
                    isRead = false,
                    flatNumber = "A-101",
                    priority = "NORMAL"
                ),
                GateNotification(
                    title = "Urgent: Water Tank Cleaning",
                    message = "Tower A water supply will be paused from 10 AM to 3 PM this Saturday.",
                    type = "NOTICE",
                    timestamp = now - (1 * dayMs),
                    isRead = true,
                    flatNumber = null,
                    priority = "HIGH"
                ),
                GateNotification(
                    title = "Security Alert: Gate 2 Speed Barrier",
                    message = "Gate 2 boom barrier inspection scheduled today at 4:00 PM. Please use Gate 1.",
                    type = "SECURITY",
                    timestamp = now - (2 * dayMs),
                    isRead = true,
                    flatNumber = null,
                    priority = "NORMAL"
                )
            )
            dao.insertNotifications(initialNotifications)

            // 6. Seed Resident Registered Vehicles for Gate Security Cross-Referencing
            val initialVehicles = listOf(
                ResidentVehicle(
                    plateNumber = "MH 02 CZ 4488",
                    cleanPlate = "MH02CZ4488",
                    vehicleType = "Electric EV Car",
                    makeModel = "Tata Nexon EV Max",
                    color = "Pristine White",
                    tower = "Tower A",
                    flatNumber = "A-101",
                    ownerName = "Rajesh Sharma",
                    ownerPhone = "+91 98201 12345",
                    parkingSlot = "B1-P12",
                    stickerNumber = "SOC-V-1024",
                    status = "ACTIVE",
                    notes = "Green EV number plate with fast charging slot"
                ),
                ResidentVehicle(
                    plateNumber = "MH 02 EE 7812",
                    cleanPlate = "MH02EE7812",
                    vehicleType = "Electric EV Scooter",
                    makeModel = "Ather 450X Gen 3",
                    color = "Space Grey",
                    tower = "Tower A",
                    flatNumber = "A-101",
                    ownerName = "Rajesh Sharma",
                    ownerPhone = "+91 98201 12345",
                    parkingSlot = "B1-S05",
                    stickerNumber = "SOC-V-1025",
                    status = "ACTIVE",
                    notes = "Resident primary two-wheeler"
                ),
                ResidentVehicle(
                    plateNumber = "MH 04 BK 9911",
                    cleanPlate = "MH04BK9911",
                    vehicleType = "4-Wheeler Car",
                    makeModel = "Hyundai Creta SX(O)",
                    color = "Phantom Black",
                    tower = "Tower B",
                    flatNumber = "B-105",
                    ownerName = "Priya Mehta",
                    ownerPhone = "+91 98202 67890",
                    parkingSlot = "B2-P34",
                    stickerNumber = "SOC-V-2051",
                    status = "ACTIVE",
                    notes = "Covered basement parking slot"
                ),
                ResidentVehicle(
                    plateNumber = "DL 01 AA 9999",
                    cleanPlate = "DL01AA9999",
                    vehicleType = "Electric EV Car",
                    makeModel = "MG ZS EV Excite",
                    color = "Currant Red",
                    tower = "Tower C",
                    flatNumber = "C-301",
                    ownerName = "Vikram Malhotra",
                    ownerPhone = "+91 98203 54321",
                    parkingSlot = "B1-P88",
                    stickerNumber = "SOC-V-3012",
                    status = "ACTIVE",
                    notes = "Fastag and RFID gate sensor enabled"
                ),
                ResidentVehicle(
                    plateNumber = "KA 05 MN 5678",
                    cleanPlate = "KA05MN5678",
                    vehicleType = "2-Wheeler Bike",
                    makeModel = "Royal Enfield Meteor 350",
                    color = "Fireball Yellow",
                    tower = "Tower D",
                    flatNumber = "D-402",
                    ownerName = "Ananya Desai",
                    ownerPhone = "+91 98204 98765",
                    parkingSlot = "B2-S19",
                    stickerNumber = "SOC-V-4022",
                    status = "ACTIVE",
                    notes = "Motorcycle bay sticker verified"
                ),
                ResidentVehicle(
                    plateNumber = "MH 12 QX 3321",
                    cleanPlate = "MH12QX3321",
                    vehicleType = "4-Wheeler Car",
                    makeModel = "Maruti Suzuki Grand Vitara",
                    color = "Splendid Silver",
                    tower = "Tower A",
                    flatNumber = "A-202",
                    ownerName = "Amit Verma",
                    ownerPhone = "+91 98205 11223",
                    parkingSlot = "B1-P18",
                    stickerNumber = "SOC-V-1202",
                    status = "ACTIVE",
                    notes = "Dual parking permit resident"
                )
            )
            dao.insertVehicles(initialVehicles)

            // 7. Seed Initial Resident Profiles for Personalized Alerts & Emergency Broadcasts
            val initialProfiles = listOf(
                ResidentProfile(
                    flatNumber = "A-101",
                    fullName = "Chandan Kumar",
                    tower = "Tower A",
                    primaryPhone = "+91 98201 12345",
                    alternatePhone = "+91 98201 54321",
                    email = "chandan.kumar8386@gmail.com",
                    ownershipType = "OWNER",
                    moveInDate = "Jan 2023",
                    intercomExtension = "101",
                    parkingSlot = "B1-P12",
                    residentCount = 3,
                    bloodGroup = "O+",
                    emergencyContactName = "Kavita Kumar",
                    emergencyContactPhone = "+91 98201 99887",
                    emergencyContactRelation = "Spouse",
                    enableSmsAlerts = true,
                    enablePushAlerts = true,
                    enableVisitorArrivalAlerts = true,
                    enableEmergencyBroadcasts = true,
                    specialNotes = "Senior family member at home; Ring intercom before buzz-in"
                ),
                ResidentProfile(
                    flatNumber = "B-204",
                    fullName = "Priya Sharma",
                    tower = "Tower B",
                    primaryPhone = "+91 98202 11223",
                    alternatePhone = "+91 98202 99887",
                    email = "priya.sharma@example.com",
                    ownershipType = "OWNER",
                    moveInDate = "Aug 2022",
                    intercomExtension = "204",
                    parkingSlot = "B1-P24",
                    residentCount = 2,
                    bloodGroup = "B+",
                    emergencyContactName = "Rohan Sharma",
                    emergencyContactPhone = "+91 98202 55667",
                    emergencyContactRelation = "Brother",
                    enableSmsAlerts = true,
                    enablePushAlerts = true,
                    enableVisitorArrivalAlerts = true,
                    enableEmergencyBroadcasts = true,
                    specialNotes = "Pet dog inside apartment"
                ),
                ResidentProfile(
                    flatNumber = "C-302",
                    fullName = "Dr. Ananya Roy",
                    tower = "Tower C",
                    primaryPhone = "+91 98203 99887",
                    alternatePhone = "",
                    email = "dr.ananya.roy@apollohospitals.com",
                    ownershipType = "OWNER",
                    moveInDate = "Nov 2021",
                    intercomExtension = "303",
                    parkingSlot = "B2-P08",
                    residentCount = 1,
                    bloodGroup = "AB+",
                    emergencyContactName = "Dr. Subhash Roy",
                    emergencyContactPhone = "+91 98203 11224",
                    emergencyContactRelation = "Father",
                    enableSmsAlerts = true,
                    enablePushAlerts = true,
                    enableVisitorArrivalAlerts = true,
                    enableEmergencyBroadcasts = true,
                    specialNotes = "Medical Doctor - Available for Society medical emergencies"
                ),
                ResidentProfile(
                    flatNumber = "D-401",
                    fullName = "Vikramaditya Rao",
                    tower = "Tower D",
                    primaryPhone = "+91 98204 98765",
                    alternatePhone = "+91 98204 11223",
                    email = "vikram.rao@fintech.co",
                    ownershipType = "TENANT",
                    moveInDate = "May 2024",
                    intercomExtension = "401",
                    parkingSlot = "B2-P19",
                    residentCount = 4,
                    bloodGroup = "A+",
                    emergencyContactName = "Meera Rao",
                    emergencyContactPhone = "+91 98204 77889",
                    emergencyContactRelation = "Spouse",
                    enableSmsAlerts = true,
                    enablePushAlerts = true,
                    enableVisitorArrivalAlerts = true,
                    enableEmergencyBroadcasts = true,
                    specialNotes = "Frequent business travel; Deliveries to be held at guard desk"
                )
            )
            dao.insertProfiles(initialProfiles)
        }
    }
}
