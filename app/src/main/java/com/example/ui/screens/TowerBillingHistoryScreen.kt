package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.MaintenanceBill
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen allowing residents and managers to view their tower-wise maintenance billing history,
 * including individual ledger entries, payment statuses (PAID, PENDING, OVERDUE),
 * and due amount breakdowns directly from Room database data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TowerBillingHistoryScreen(
    bills: List<MaintenanceBill>,
    currentFlat: String,
    onPayBill: (MaintenanceBill) -> Unit,
    onViewReceipt: (MaintenanceBill) -> Unit,
    onBack: (() -> Unit)? = null
) {
    // Derive resident's current tower from flat number (e.g., "A-101" -> "Tower A")
    val residentTower = remember(currentFlat) {
        when {
            currentFlat.startsWith("A", ignoreCase = true) -> "Tower A"
            currentFlat.startsWith("B", ignoreCase = true) -> "Tower B"
            currentFlat.startsWith("C", ignoreCase = true) -> "Tower C"
            currentFlat.startsWith("D", ignoreCase = true) -> "Tower D"
            else -> "Tower A"
        }
    }

    var selectedTower by remember { mutableStateOf(residentTower) }
    var selectedStatus by remember { mutableStateOf("ALL") } // ALL, DUE, PAID
    var searchQuery by remember { mutableStateOf("") }
    var expandedBillId by remember { mutableStateOf<Long?>(null) }

    // List of towers present in the society
    val towers = listOf("ALL", "Tower A", "Tower B", "Tower C", "Tower D")

    // Filter bills according to selected tower, status, and search query
    val filteredBills = remember(bills, selectedTower, selectedStatus, searchQuery) {
        bills.filter { bill ->
            val matchesTower = if (selectedTower == "ALL") true else bill.tower.equals(selectedTower, ignoreCase = true)
            val matchesStatus = when (selectedStatus) {
                "DUE" -> bill.status == "PENDING" || bill.status == "OVERDUE"
                "PAID" -> bill.status == "PAID"
                else -> true
            }
            val matchesSearch = searchQuery.isBlank() ||
                    bill.flatNumber.contains(searchQuery, ignoreCase = true) ||
                    bill.residentName.contains(searchQuery, ignoreCase = true) ||
                    bill.monthYear.contains(searchQuery, ignoreCase = true)

            matchesTower && matchesStatus && matchesSearch
        }.sortedWith(
            compareByDescending<MaintenanceBill> { it.flatNumber == currentFlat } // Highlight current resident flat first
                .thenByDescending { it.dueDate }
        )
    }

    // Tower financial aggregates for the selected tower
    val towerTotalBilled = remember(bills, selectedTower) {
        val towerSubset = if (selectedTower == "ALL") bills else bills.filter { it.tower.equals(selectedTower, ignoreCase = true) }
        towerSubset.sumOf { it.totalAmount }
    }
    val towerTotalPaid = remember(bills, selectedTower) {
        val towerSubset = if (selectedTower == "ALL") bills else bills.filter { it.tower.equals(selectedTower, ignoreCase = true) }
        towerSubset.filter { it.status == "PAID" }.sumOf { it.totalAmount }
    }
    val towerTotalDue = remember(bills, selectedTower) {
        val towerSubset = if (selectedTower == "ALL") bills else bills.filter { it.tower.equals(selectedTower, ignoreCase = true) }
        towerSubset.filter { it.status != "PAID" }.sumOf { it.totalAmount }
    }
    val towerPaidCount = remember(bills, selectedTower) {
        val towerSubset = if (selectedTower == "ALL") bills else bills.filter { it.tower.equals(selectedTower, ignoreCase = true) }
        towerSubset.count { it.status == "PAID" }
    }
    val towerPendingCount = remember(bills, selectedTower) {
        val towerSubset = if (selectedTower == "ALL") bills else bills.filter { it.tower.equals(selectedTower, ignoreCase = true) }
        towerSubset.count { it.status != "PAID" }
    }

    val collectionProgress by animateFloatAsState(
        targetValue = if (towerTotalBilled > 0) (towerTotalPaid / towerTotalBilled).toFloat() else 0f,
        label = "collectionRate"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tower_billing_history_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("billing_history_back_btn")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Column {
                        Text(
                            text = "Tower-Wise Billing History",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Maintenance ledger, due amounts & receipts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Flat $currentFlat",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // 2. Tower Selector Tabs / Chips
        item {
            Column {
                Text(
                    text = "Select Tower / Block",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    towers.forEach { towerName ->
                        val isSelected = selectedTower == towerName
                        val isMyTower = towerName == residentTower
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTower = towerName },
                            label = {
                                Text(
                                    text = if (isMyTower && towerName != "ALL") "$towerName (My Tower)" else if (towerName == "ALL") "All Towers" else towerName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = if (isMyTower) {
                                { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("tower_filter_${towerName.replace(" ", "_")}")
                        )
                    }
                }
            }
        }

        // 3. Tower Financial Health & Due Amount Summary Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tower_due_summary_banner")
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (selectedTower == "ALL") "All Towers Overview" else "$selectedTower Accounts",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (towerTotalDue > 0) Color(0xFFF59E0B) else Color(0xFF10B981)
                            ) {
                                Text(
                                    text = if (towerTotalDue > 0) "${towerPendingCount} Flat(s) Pending" else "100% Cleared",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Outstanding Due Amount",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formatIndianCurrency(towerTotalDue),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (towerTotalDue > 0) Color(0xFFFDE68A) else Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Settled / Collected",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = formatIndianCurrency(towerTotalPaid),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Progress Meter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Settlement Rate: ${(collectionProgress * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$towerPaidCount Paid • $towerPendingCount Due",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { collectionProgress },
                            color = Color(0xFF34D399),
                            trackColor = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }

        // 4. Search & Status Filter Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search flat (e.g. 101), resident, or month...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_field")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All Ledger Rows (${filteredBills.size})",
                        "DUE" to "Due Amounts",
                        "PAID" to "Paid Invoices"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedStatus == key,
                            onClick = { selectedStatus = key },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("status_filter_$key")
                        )
                    }
                }
            }
        }

        // 5. Ledger Entries Section Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Individual Ledger Entries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredBills.size} records",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6. Individual Ledger Entries
        if (filteredBills.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No billing ledger entries match your filter",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Try switching towers or resetting filters.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredBills, key = { it.id }) { bill ->
                val isCurrentFlat = bill.flatNumber == currentFlat
                val isExpanded = expandedBillId == bill.id

                TowerLedgerCard(
                    bill = bill,
                    isCurrentFlat = isCurrentFlat,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedBillId = if (isExpanded) null else bill.id
                    },
                    onPay = { onPayBill(bill) },
                    onViewReceipt = { onViewReceipt(bill) }
                )
            }
        }
    }
}

/**
 * Individual maintenance billing ledger card displaying flat number, resident,
 * total due amount, payment status tag, breakdown, and payment/receipt actions.
 */
@Composable
fun TowerLedgerCard(
    bill: MaintenanceBill,
    isCurrentFlat: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPay: () -> Unit,
    onViewReceipt: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentFlat) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentFlat) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentFlat) 3.dp else 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ledger_entry_card_${bill.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Flat & Resident vs Status Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(
                                if (bill.status == "PAID") Color(0xFFDCFCE7)
                                else if (bill.status == "OVERDUE") Color(0xFFFEE2E2)
                                else Color(0xFFFEF3C7)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = bill.flatNumber.take(4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (bill.status == "PAID") Color(0xFF166534)
                            else if (bill.status == "OVERDUE") Color(0xFF991B1B)
                            else Color(0xFF92400E)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Flat ${bill.flatNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (isCurrentFlat) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "YOU",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${bill.residentName} • ${bill.tower}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Payment Status Badge
                StatusPill(status = bill.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Billing Cycle & Due Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BILLING CYCLE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = bill.monthYear,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (bill.status == "PAID") {
                            "Paid on ${bill.paidDate?.let { dateFormat.format(Date(it)) } ?: "Recent"}"
                        } else {
                            "Due by ${dateFormat.format(Date(bill.dueDate))}"
                        },
                        fontSize = 11.sp,
                        color = if (bill.status == "OVERDUE") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (bill.status == "PAID") "AMOUNT PAID" else "DUE AMOUNT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatIndianCurrency(bill.totalAmount),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (bill.status == "PAID") Color(0xFF16A34A)
                        else if (bill.status == "OVERDUE") Color(0xFFDC2626)
                        else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Expandable Itemized Fee Breakdown Accordion
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                    Text(
                        text = "Itemized Fee Ledger Breakdown",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            BreakdownItemRow("Base Maintenance Rate", bill.baseMaintenance)
                            BreakdownItemRow("Sinking Fund Reserve", bill.sinkingFund)
                            BreakdownItemRow("Water & Sewerage Charges", bill.waterSewerage)
                            BreakdownItemRow("Dedicated Parking Bay", bill.parkingCharges)
                            BreakdownItemRow("Clubhouse & Gym Charges", bill.clubhouseFee)

                            if (bill.receiptNumber != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Receipt Number:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(bill.receiptNumber, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (bill.transactionRef != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Transaction Ref:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(bill.transactionRef, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onToggleExpand,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                    modifier = Modifier.testTag("toggle_expand_btn_${bill.id}")
                ) {
                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isExpanded) "Hide Breakdown" else "View Breakdown",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (bill.status == "PAID") {
                    FilledTonalButton(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("view_receipt_button_${bill.id}")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Receipt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bill.status == "OVERDUE") Color(0xFFDC2626) else MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("pay_bill_button_${bill.id}")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pay Dues", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownItemRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = formatIndianCurrency(amount), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusPill(status: String) {
    val (bgColor, textColor, icon) = when (status) {
        "PAID" -> Triple(Color(0xFFDCFCE7), Color(0xFF15803D), Icons.Default.CheckCircle)
        "OVERDUE" -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.Warning)
        else -> Triple(Color(0xFFFEF3C7), Color(0xFFB45309), Icons.Default.Schedule)
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Helper to format standard Indian currency format (e.g., ₹4,750).
 */
private fun formatIndianCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    return format.format(amount).replace("INR", "₹").trim()
}
