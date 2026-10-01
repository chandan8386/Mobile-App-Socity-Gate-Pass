package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.MaintenanceBill
import com.example.ui.SocietyFinancialOverview
import com.example.ui.TowerFinancialStats
import com.example.ui.UserRole
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(
    bills: List<MaintenanceBill>,
    financialOverview: SocietyFinancialOverview,
    currentRole: UserRole,
    currentFlat: String,
    onPayBill: (MaintenanceBill) -> Unit,
    onViewReceipt: (MaintenanceBill) -> Unit,
    onGenerateBilling: (String) -> Unit,
    onDispatchReminders: (String?) -> Unit
) {
    var selectedTowerFilter by remember { mutableStateOf("ALL") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }
    var showStatementDialog by remember { mutableStateOf(false) }
    var showGenerateBillingDialog by remember { mutableStateOf(false) }
    var residentViewMode by remember { mutableStateOf("TOWER_HISTORY") } // TOWER_HISTORY, MY_FLAT

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        if (currentRole == UserRole.RESIDENT) {
            PrimaryTabRow(
                selectedTabIndex = if (residentViewMode == "TOWER_HISTORY") 0 else 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Tab(
                    selected = residentViewMode == "TOWER_HISTORY",
                    onClick = { residentViewMode = "TOWER_HISTORY" },
                    text = {
                        Text(
                            "Tower Billing History",
                            fontWeight = if (residentViewMode == "TOWER_HISTORY") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.Apartment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_sub_tower_billing_history")
                )
                Tab(
                    selected = residentViewMode == "MY_FLAT",
                    onClick = { residentViewMode = "MY_FLAT" },
                    text = {
                        Text(
                            "My Flat ($currentFlat)",
                            fontWeight = if (residentViewMode == "MY_FLAT") FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    },
                    icon = {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_sub_my_flat_dues")
                )
            }

            when (residentViewMode) {
                "TOWER_HISTORY" -> {
                    TowerBillingHistoryScreen(
                        bills = bills,
                        currentFlat = currentFlat,
                        onPayBill = onPayBill,
                        onViewReceipt = onViewReceipt
                    )
                }
                else -> {
                    ResidentLedgerView(
                        currentFlat = currentFlat,
                        bills = bills.filter { it.flatNumber == currentFlat },
                        onPayBill = onPayBill,
                        onViewReceipt = onViewReceipt,
                        onOpenTowerHistory = { residentViewMode = "TOWER_HISTORY" }
                    )
                }
            }
        } else {
            // Property Manager / Admin Tower-Wise Collection & Reports View
            ManagerFinancialDashboard(
                bills = bills,
                overview = financialOverview,
                selectedTower = selectedTowerFilter,
                selectedStatus = selectedStatusFilter,
                onSelectTower = { selectedTowerFilter = it },
                onSelectStatus = { selectedStatusFilter = it },
                onPayBill = onPayBill,
                onViewReceipt = onViewReceipt,
                onOpenStatement = { showStatementDialog = true },
                onOpenGenerateBilling = { showGenerateBillingDialog = true },
                onDispatchReminders = onDispatchReminders
            )
        }
    }

    if (showStatementDialog) {
        FinancialStatementDialog(
            overview = financialOverview,
            bills = bills,
            onDismiss = { showStatementDialog = false }
        )
    }

    if (showGenerateBillingDialog) {
        GenerateBillingDialog(
            onDismiss = { showGenerateBillingDialog = false },
            onConfirm = { month ->
                onGenerateBilling(month)
                showGenerateBillingDialog = false
            }
        )
    }
}

@Composable
private fun ResidentLedgerView(
    currentFlat: String,
    bills: List<MaintenanceBill>,
    onPayBill: (MaintenanceBill) -> Unit,
    onViewReceipt: (MaintenanceBill) -> Unit,
    onOpenTowerHistory: () -> Unit = {}
) {
    val pendingBills = bills.filter { it.status == "PENDING" || it.status == "OVERDUE" }
    val totalPendingAmount = pendingBills.sumOf { it.totalAmount }
    val latestPending = pendingBills.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
    ) {
        // Tower Billing History Shortcut Banner
        item {
            OutlinedButton(
                onClick = onOpenTowerHistory,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_tower_billing_history_btn")
            ) {
                Icon(Icons.Default.Apartment, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Tower-Wise Billing History & All Flats")
            }
        }

        // Outstanding Balance Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalPendingAmount > 0) Color(0xFF1E3A8A) else Color(0xFF0F766E)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("resident_balance_hero_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PERSONAL LEDGER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Flat $currentFlat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (totalPendingAmount > 0) "${pendingBills.size} Due" else "All Cleared",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Total Outstanding Dues",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = formatCurrency(totalPendingAmount),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (latestPending != null) {
                        Button(
                            onClick = { onPayBill(latestPending) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("pay_maintenance_button")
                        ) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pay ${formatCurrency(latestPending.totalAmount)} (${latestPending.monthYear})",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4ADE80))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "No outstanding maintenance balance! Good standing.",
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Itemized breakdown preview for latest pending bill
        if (latestPending != null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Itemized Fee Structure (${latestPending.monthYear})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BreakdownRow("Base Maintenance Rate", formatCurrency(latestPending.baseMaintenance))
                        BreakdownRow("Sinking Fund Reserve", formatCurrency(latestPending.sinkingFund))
                        BreakdownRow("Water & Drainage Charges", formatCurrency(latestPending.waterSewerage))
                        BreakdownRow("Dedicated Parking Bay", formatCurrency(latestPending.parkingCharges))
                        BreakdownRow("Clubhouse & Gym Maintenance", formatCurrency(latestPending.clubhouseFee))
                    }
                }
            }
        }

        // Monthly Invoices List
        item {
            Text(
                text = "Billing & Receipt History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(bills, key = { it.id }) { bill ->
            ResidentBillCard(
                bill = bill,
                onPay = { onPayBill(bill) },
                onViewReceipt = { onViewReceipt(bill) }
            )
        }
    }
}

@Composable
private fun BreakdownRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ResidentBillCard(
    bill: MaintenanceBill,
    onPay: () -> Unit,
    onViewReceipt: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bill_card_${bill.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = bill.monthYear,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (bill.status == "PAID") "Paid via ${bill.paymentGateway ?: "Online"}" else "Due by 15th",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (bill.transactionRef != null) {
                        Text(
                            text = "Txn ID: ${bill.transactionRef}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                StatusBadge(bill.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(bill.totalAmount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (bill.status == "PAID") {
                    OutlinedButton(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("view_receipt_button_${bill.id}")
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Receipt")
                    }
                } else {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pay_bill_button_${bill.id}")
                    ) {
                        Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pay Now")
                    }
                }
            }
        }
    }
}

// Property Manager / Administration View
@Composable
private fun ManagerFinancialDashboard(
    bills: List<MaintenanceBill>,
    overview: SocietyFinancialOverview,
    selectedTower: String,
    selectedStatus: String,
    onSelectTower: (String) -> Unit,
    onSelectStatus: (String) -> Unit,
    onPayBill: (MaintenanceBill) -> Unit,
    onViewReceipt: (MaintenanceBill) -> Unit,
    onOpenStatement: () -> Unit,
    onOpenGenerateBilling: () -> Unit,
    onDispatchReminders: (String?) -> Unit
) {
    val filteredBills = remember(bills, selectedTower, selectedStatus) {
        bills.filter { bill ->
            val towerMatch = selectedTower == "ALL" || bill.tower == selectedTower
            val statusMatch = when (selectedStatus) {
                "PAID" -> bill.status == "PAID"
                "PENDING" -> bill.status == "PENDING"
                "OVERDUE" -> bill.status == "OVERDUE"
                else -> true
            }
            towerMatch && statusMatch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // Overall Collection Summary Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SOCIETY MAINTENANCE COLLECTION",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Tower-Wise Financial Health",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = onOpenStatement) {
                            Icon(Icons.Default.Assessment, contentDescription = "Reports", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Total Demand", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatCurrency(overview.totalBilled), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text("Collected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A))
                            Text(formatCurrency(overview.totalCollected), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        }
                        Column {
                            Text("Outstanding", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                            Text(formatCurrency(overview.totalPending), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val animatedProgress by animateFloatAsState(targetValue = overview.overallCollectionRate, label = "progress")
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(text = "Overall Collection Rate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Text(text = "${(overview.overallCollectionRate * 100).toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color(0xFF16A34A),
                            trackColor = Color(0xFFE2E8F0)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Management Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenGenerateBilling,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Month", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { onDispatchReminders(if (selectedTower == "ALL") null else selectedTower) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dispatch Reminders", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Tower Breakdown Cards Grid
        item {
            Text(
                text = "Tower-Wise Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(overview.towerBreakdowns) { towerStats ->
            TowerBreakdownCard(
                stats = towerStats,
                isSelected = selectedTower == towerStats.towerName,
                onClick = {
                    onSelectTower(if (selectedTower == towerStats.towerName) "ALL" else towerStats.towerName)
                }
            )
        }

        // Tower Filter Chips
        item {
            Column {
                Text(
                    text = "Tower & Unit Ledger",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "Tower A", "Tower B", "Tower C", "Tower D").forEach { t ->
                        FilterChip(
                            selected = selectedTower == t,
                            onClick = { onSelectTower(t) },
                            label = { Text(t, fontSize = 11.sp) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL", "OVERDUE", "PENDING", "PAID").forEach { s ->
                        FilterChip(
                            selected = selectedStatus == s,
                            onClick = { onSelectStatus(s) },
                            label = { Text(s, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        // Filtered Unit Bills
        items(filteredBills, key = { it.id }) { bill ->
            ManagerUnitBillCard(
                bill = bill,
                onPay = { onPayBill(bill) },
                onViewReceipt = { onViewReceipt(bill) }
            )
        }
    }
}

@Composable
fun TowerBreakdownCard(
    stats: TowerFinancialStats,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("tower_card_${stats.towerName.replace(" ", "_")}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stats.towerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stats.paidFlats}/${stats.totalFlats} Units Paid (${(stats.collectionRate * 100).toInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stats.collectionRate >= 0.7f) Color(0xFF16A34A) else Color(0xFFD97706)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { stats.collectionRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (stats.collectionRate >= 0.7f) Color(0xFF16A34A) else Color(0xFFD97706)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Collected: ${formatCurrency(stats.collectedAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pending: ${formatCurrency(stats.pendingAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (stats.pendingAmount > 0) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ManagerUnitBillCard(
    bill: MaintenanceBill,
    onPay: () -> Unit,
    onViewReceipt: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${bill.flatNumber} (${bill.tower})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(bill.status)
                }
                Text(
                    text = "${bill.residentName} • ${bill.monthYear}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(bill.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (bill.status == "PAID") {
                IconButton(onClick = onViewReceipt) {
                    Icon(Icons.Default.Receipt, contentDescription = "Receipt", tint = Color(0xFF16A34A))
                }
            } else {
                Button(
                    onClick = onPay,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Collect", fontSize = 12.sp)
                }
            }
        }
    }
}

// Financial Statement & Audit Dialog for Property Manager
@Composable
fun FinancialStatementDialog(
    overview: SocietyFinancialOverview,
    bills: List<MaintenanceBill>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .testTag("statement_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Financial Statement Report",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Automated Property Manager Financial Audit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        Text(
                            text = "Executive Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        BreakdownRow("Total Maintenance Billed", formatCurrency(overview.totalBilled))
                        BreakdownRow("Total Amount Realized", formatCurrency(overview.totalCollected))
                        BreakdownRow("Outstanding Receivables", formatCurrency(overview.totalPending))
                        BreakdownRow("Realization Rate", "${(overview.overallCollectionRate * 100).toInt()}%")
                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                    }

                    item {
                        Text(
                            text = "Tower-Wise Summary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        overview.towerBreakdowns.forEach { tower ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(tower.towerName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${formatCurrency(tower.collectedAmount)} / ${formatCurrency(tower.totalDemand)} (${(tower.collectionRate * 100).toInt()}%)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 10.dp))
                    }

                    item {
                        Text(
                            text = "Defaulting Flats (Overdue Alerts)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val overdueBills = bills.filter { it.status == "OVERDUE" }
                        if (overdueBills.isEmpty()) {
                            Text("No overdue accounts.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            overdueBills.forEach { ob ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${ob.flatNumber} (${ob.residentName})", style = MaterialTheme.typography.bodySmall)
                                    Text(formatCurrency(ob.totalAmount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Statement (PDF / Excel)")
                }
            }
        }
    }
}

// Generate Next Month Billing Dialog
@Composable
fun GenerateBillingDialog(
    onDismiss: () -> Unit,
    onConfirm: (month: String) -> Unit
) {
    var selectedMonth by remember { mutableStateOf("October 2026") }
    val months = listOf("October 2026", "November 2026", "December 2026")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Generate Recurring Billing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Automate maintenance invoicing across all 4 towers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Select Billing Cycle:", style = MaterialTheme.typography.titleSmall)
                months.forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMonth = m }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedMonth == m, onClick = { selectedMonth = m })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(m, fontWeight = if (selectedMonth == m) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(onClick = { onConfirm(selectedMonth) }, modifier = Modifier.weight(1f)) {
                        Text("Generate")
                    }
                }
            }
        }
    }
}
