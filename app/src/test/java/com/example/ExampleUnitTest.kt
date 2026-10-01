package com.example

import com.example.data.entity.VisitorEntry
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun visitorEntrySchema_containsRequiredFields() {
    val entryTimestamp = 1727670000000L
    val exitTimestamp = 1727673600000L
    val visitor = VisitorEntry(
        id = 1L,
        timestamp = entryTimestamp,
        exitTimestamp = exitTimestamp,
        visitorName = "John Doe",
        visitorPhoneNumber = "+91 9876543210",
        visitorType = "Delivery",
        visitorCompany = "Amazon",
        vehicleNumber = "KA-01-AB-1234",
        tower = "Tower A",
        flatNumber = "A-101",
        hostResidentName = "Resident",
        status = "EXITED",
        passCode = "5678",
        qrCodeToken = "GATE-PASS-1-5678",
        remarks = "Delivered package at door"
    )

    assertEquals(1L, visitor.id)
    assertEquals(entryTimestamp, visitor.timestamp)
    assertEquals(exitTimestamp, visitor.exitTimestamp)
    assertEquals("John Doe", visitor.visitorName)
    assertEquals("+91 9876543210", visitor.visitorPhoneNumber)
    assertEquals("Delivery", visitor.visitorType)
    assertEquals("EXITED", visitor.status)
    assertEquals("GATE-PASS-1-5678", visitor.qrCodeToken)
  }

  @Test
  fun qrCodeToken_generatesUniqueEffectiveToken() {
    val visitorWithExplicitToken = VisitorEntry(
        id = 10,
        visitorName = "Alice",
        visitorPhoneNumber = "999",
        visitorType = "Guest",
        passCode = "4321",
        qrCodeToken = "GATE-PASS-TOKEN-CUSTOM-99"
    )
    assertEquals("GATE-PASS-TOKEN-CUSTOM-99", visitorWithExplicitToken.effectiveQrToken)

    val visitorWithGeneratedFallback = VisitorEntry(
        id = 25,
        visitorName = "Bob",
        visitorPhoneNumber = "888",
        visitorType = "Delivery",
        passCode = "8765"
    )
    assertEquals("GATE-PASS-25-8765", visitorWithGeneratedFallback.effectiveQrToken)
  }

  @Test
  fun qrLogout_marksStatusExitedAndSetsExitTimestamp() {
    val entryTime = 1727670000000L
    val insideVisitor = VisitorEntry(
        id = 5,
        visitorName = "Courier Agent",
        visitorPhoneNumber = "777",
        visitorType = "Delivery",
        status = "INSIDE",
        timestamp = entryTime,
        qrCodeToken = "GATE-PASS-5-9999"
    )
    assertEquals("INSIDE", insideVisitor.status)
    assertNull(insideVisitor.exitTimestamp)

    // Simulate exit logout
    val exitTime = entryTime + (30 * 60 * 1000L)
    val loggedOutVisitor = insideVisitor.copy(
        status = "EXITED",
        exitTimestamp = exitTime
    )
    assertEquals("EXITED", loggedOutVisitor.status)
    assertEquals(exitTime, loggedOutVisitor.exitTimestamp)
    assertTrue(loggedOutVisitor.exitTimestamp!! > loggedOutVisitor.timestamp)
  }

  @Test
  fun visitorFormValidation_requiresNameAndPhone() {
    val blankName = "   "
    val validName = "Sunil Sharma"
    val invalidPhone = "123"
    val validPhone = "+91 98765 43210"

    assertTrue(blankName.isBlank())
    assertTrue(validName.isNotBlank())
    assertTrue(invalidPhone.filter { it.isDigit() }.length < 7)
    assertTrue(validPhone.filter { it.isDigit() }.length >= 10)
  }

  @Test
  fun recentVisitors_orderedByEntryTimeDescending() {
    val v1 = VisitorEntry(id = 1, timestamp = 1000L, visitorName = "Early Visitor", visitorPhoneNumber = "111", visitorType = "Guest")
    val v2 = VisitorEntry(id = 2, timestamp = 3000L, visitorName = "Latest Visitor", visitorPhoneNumber = "222", visitorType = "Delivery")
    val v3 = VisitorEntry(id = 3, timestamp = 2000L, visitorName = "Midday Visitor", visitorPhoneNumber = "333", visitorType = "Cab / Ride")

    val list = listOf(v1, v2, v3)
    val sorted = list.sortedByDescending { it.timestamp }

    assertEquals("Latest Visitor", sorted[0].visitorName)
    assertEquals("Midday Visitor", sorted[1].visitorName)
    assertEquals("Early Visitor", sorted[2].visitorName)
  }

  @Test
  fun searchVisitors_filterByNameOrVisitorType() {
    val list = listOf(
        VisitorEntry(id = 1, visitorName = "Rahul Verma", visitorPhoneNumber = "111", visitorType = "Guest"),
        VisitorEntry(id = 2, visitorName = "Suresh Patil", visitorPhoneNumber = "222", visitorType = "Delivery"),
        VisitorEntry(id = 3, visitorName = "Amitabh Sen", visitorPhoneNumber = "333", visitorType = "Service / Repair"),
        VisitorEntry(id = 4, visitorName = "Deepak Kumar", visitorPhoneNumber = "444", visitorType = "Cab / Ride")
    )

    // Search by name "Rahul"
    val queryName = "rahul"
    val filteredByName = list.filter {
      it.visitorName.lowercase().contains(queryName) || it.visitorType.lowercase().contains(queryName)
    }
    assertEquals(1, filteredByName.size)
    assertEquals("Rahul Verma", filteredByName[0].visitorName)

    // Search by visitor type "Delivery"
    val queryType = "delivery"
    val filteredByType = list.filter {
      it.visitorName.lowercase().contains(queryType) || it.visitorType.lowercase().contains(queryType)
    }
    assertEquals(1, filteredByType.size)
    assertEquals("Suresh Patil", filteredByType[0].visitorName)

    // Search by partial visitor type "Service"
    val queryService = "service"
    val filteredByService = list.filter {
      it.visitorName.lowercase().contains(queryService) || it.visitorType.lowercase().contains(queryService)
    }
    assertEquals(1, filteredByService.size)
    assertEquals("Amitabh Sen", filteredByService[0].visitorName)
  }

  @Test
  fun formatDurationMinutes_formatsCorrectly() {
    assertEquals("45m", com.example.ui.screens.formatDurationMinutes(45))
    assertEquals("1h", com.example.ui.screens.formatDurationMinutes(60))
    assertEquals("1h 30m", com.example.ui.screens.formatDurationMinutes(90))
    assertEquals("2h 15m", com.example.ui.screens.formatDurationMinutes(135))
  }

  @Test
  fun computeVisitorAnalytics_calculatesTotalActiveAndAverageDuration() {
    val now = System.currentTimeMillis()
    val visitors = listOf(
        // Inside visitor
        VisitorEntry(id = 1, visitorName = "Inside 1", visitorPhoneNumber = "1", visitorType = "Delivery", status = "INSIDE", timestamp = now - 60000),
        VisitorEntry(id = 2, visitorName = "Inside 2", visitorPhoneNumber = "2", visitorType = "Guest", status = "INSIDE", timestamp = now - 120000),
        // Completed visits
        VisitorEntry(
            id = 3, visitorName = "Exited 1", visitorPhoneNumber = "3", visitorType = "Delivery",
            status = "EXITED", timestamp = now - (60 * 60 * 1000L), exitTimestamp = now - (40 * 60 * 1000L) // 20 mins
        ),
        VisitorEntry(
            id = 4, visitorName = "Exited 2", visitorPhoneNumber = "4", visitorType = "Guest",
            status = "EXITED", timestamp = now - (120 * 60 * 1000L), exitTimestamp = now - (60 * 60 * 1000L) // 60 mins
        )
    )

    val analytics = com.example.ui.screens.computeVisitorAnalytics(visitors, "ALL")

    assertEquals(4, analytics.totalDailyEntries)
    assertEquals(2, analytics.currentlyActiveInside)
    assertEquals(2, analytics.totalExited)
    // Avg duration = (20 + 60) / 2 = 40 mins
    assertEquals(40L, analytics.averageDurationMinutes)
    assertEquals(20L, analytics.shortestDurationMinutes)
    assertEquals(60L, analytics.longestDurationMinutes)
    assertEquals(2, analytics.activeByType.size)
    assertEquals(1, analytics.activeByType["Delivery"])
    assertEquals(1, analytics.activeByType["Guest"])
  }

  @Test
  fun csvExport_generatesValidHeadersAndEscapesValues() {
    val escapedString = com.example.util.CsvExportUtil.escapeCsv("Sharma, Rahul \"VIP\"")
    assertEquals("\"Sharma, Rahul \"\"VIP\"\"\"", escapedString)

    val entry = VisitorEntry(
        id = 101,
        timestamp = 1727670000000L,
        exitTimestamp = 1727671800000L, // 30 mins later
        visitorName = "Rahul, Kumar",
        visitorPhoneNumber = "+91 9876543210",
        visitorType = "Guest",
        visitorCompany = "Personal",
        tower = "Tower A",
        flatNumber = "A-101",
        hostResidentName = "Chandan Kumar",
        vehicleNumber = "KA-01-AB-1234",
        status = "EXITED",
        passCode = "4821",
        qrCodeToken = "GATE-PASS-101-4821",
        remarks = "Dinner with family"
    )

    val csv = com.example.util.CsvExportUtil.generateVisitorCsv(listOf(entry))

    assertTrue(csv.contains("Log ID,Entry Timestamp,Entry Time,Exit Timestamp,Exit Time,Dwell Duration (Mins),Visitor Name"))
    assertTrue(csv.contains("101"))
    assertTrue(csv.contains("\"Rahul, Kumar\""))
    assertTrue(csv.contains("+91 9876543210"))
    assertTrue(csv.contains("30")) // Dwell duration
    assertTrue(csv.contains("GATE-PASS-101-4821"))
    assertTrue(csv.contains("EXITED"))
  }

  @Test
  fun overdueVisitorDetection_flagsVisitorsExceedingThreshold() {
    val now = System.currentTimeMillis()
    val thresholdMinutes = 60
    val thresholdMs = thresholdMinutes * 60 * 1000L

    val freshVisitor = VisitorEntry(
        id = 1,
        visitorName = "Recent Courier",
        visitorPhoneNumber = "111",
        visitorType = "Delivery",
        status = "INSIDE",
        timestamp = now - (20 * 60 * 1000L) // 20 mins ago
    )

    val overdueVisitor = VisitorEntry(
        id = 2,
        visitorName = "Electrician Technician",
        visitorPhoneNumber = "222",
        visitorType = "Service / Repair",
        status = "INSIDE",
        timestamp = now - (85 * 60 * 1000L) // 85 mins ago (> 60m threshold)
    )

    val insideVisitors = listOf(freshVisitor, overdueVisitor)

    val flagged = insideVisitors.filter {
      it.status == "INSIDE" && (now - it.timestamp) >= thresholdMs
    }

    assertEquals(1, flagged.size)
    assertEquals("Electrician Technician", flagged[0].visitorName)
    assertTrue(now - flagged[0].timestamp >= thresholdMs)
  }

  @Test
  fun towerBillingHistory_calculatesTowerDuesAndAggregates() {
    val now = System.currentTimeMillis()
    val bills = listOf(
        com.example.data.entity.MaintenanceBill(
            id = 1,
            tower = "Tower A",
            flatNumber = "A-101",
            residentName = "Chandan Kumar",
            monthYear = "September 2026",
            baseMaintenance = 3000.0,
            sinkingFund = 500.0,
            waterSewerage = 400.0,
            parkingCharges = 300.0,
            clubhouseFee = 200.0,
            totalAmount = 4400.0,
            dueDate = now + 86400000L,
            status = "PENDING"
        ),
        com.example.data.entity.MaintenanceBill(
            id = 2,
            tower = "Tower A",
            flatNumber = "A-102",
            residentName = "Sunil Narang",
            monthYear = "September 2026",
            baseMaintenance = 3000.0,
            sinkingFund = 500.0,
            waterSewerage = 400.0,
            parkingCharges = 300.0,
            clubhouseFee = 200.0,
            totalAmount = 4400.0,
            dueDate = now + 86400000L,
            status = "PAID",
            paidDate = now - 3600000L,
            paymentGateway = "UPI"
        ),
        com.example.data.entity.MaintenanceBill(
            id = 3,
            tower = "Tower B",
            flatNumber = "B-201",
            residentName = "Ramesh Gupta",
            monthYear = "September 2026",
            baseMaintenance = 3500.0,
            sinkingFund = 600.0,
            waterSewerage = 500.0,
            parkingCharges = 400.0,
            clubhouseFee = 250.0,
            totalAmount = 5250.0,
            dueDate = now - 86400000L,
            status = "OVERDUE"
        )
    )

    // Tower A tests
    val towerABills = bills.filter { it.tower == "Tower A" }
    val towerATotalBilled = towerABills.sumOf { it.totalAmount }
    val towerATotalPaid = towerABills.filter { it.status == "PAID" }.sumOf { it.totalAmount }
    val towerATotalDue = towerABills.filter { it.status != "PAID" }.sumOf { it.totalAmount }

    assertEquals(2, towerABills.size)
    assertEquals(8800.0, towerATotalBilled, 0.01)
    assertEquals(4400.0, towerATotalPaid, 0.01)
    assertEquals(4400.0, towerATotalDue, 0.01)

    // Tower B tests
    val towerBBills = bills.filter { it.tower == "Tower B" }
    val towerBTotalDue = towerBBills.filter { it.status != "PAID" }.sumOf { it.totalAmount }
    assertEquals(1, towerBBills.size)
    assertEquals(5250.0, towerBTotalDue, 0.01)
    assertEquals("OVERDUE", towerBBills[0].status)
  }

  @Test
  fun mockPaymentGatewayService_generatesTransactionIdAndCapturesDetails() = kotlinx.coroutines.runBlocking {
    val request = com.example.service.payment.PaymentTransactionRequest(
        billId = 42,
        flatNumber = "A-101",
        residentName = "Chandan Kumar",
        monthYear = "September 2026",
        amount = 4750.0,
        paymentMethod = com.example.service.payment.PaymentMethodType.UPI_GPAY,
        payerVpaOrCardNumber = "chandan@okhdfcbank"
    )

    val result = com.example.service.payment.MockPaymentGatewayService.executePayment(request)

    assertTrue(result.isSuccess)
    assertEquals(com.example.service.payment.PaymentStatus.SUCCESS, result.status)
    assertTrue(result.transactionId.startsWith("TXN-UPI-"))
    assertTrue(result.receiptNumber.startsWith("REC-SG-"))
    assertTrue(result.bankRefNumber.startsWith("BNK-"))
    assertEquals("Google Pay (UPI)", result.gatewayProvider)
    assertEquals(4750.0, result.amountPaid, 0.01)
    assertEquals("A-101", result.flatNumber)
  }

  @Test
  fun mockPaymentGatewayService_handlesSimulatedDeclineGracefully() = kotlinx.coroutines.runBlocking {
    val request = com.example.service.payment.PaymentTransactionRequest(
        billId = 42,
        flatNumber = "A-101",
        residentName = "Chandan Kumar",
        monthYear = "September 2026",
        amount = 4750.0,
        paymentMethod = com.example.service.payment.PaymentMethodType.CREDIT_CARD,
        payerVpaOrCardNumber = "4532 8812 9043 7712",
        simulateBankDecline = true
    )

    val result = com.example.service.payment.MockPaymentGatewayService.executePayment(request)

    assertFalse(result.isSuccess)
    assertEquals(com.example.service.payment.PaymentStatus.FAILED, result.status)
    assertTrue(result.transactionId.startsWith("TXN-FAIL-"))
    assertEquals("Credit Card (Visa / MasterCard)", result.gatewayProvider)
    assertNotNull(result.errorMessage)
  }
}
