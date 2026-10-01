package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.VisitorEntry
import com.example.util.CsvExportUtil
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Modal dialog for managers and admins to filter and export visitor logs into a standard CSV file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitorCsvExportDialog(
    visitors: List<VisitorEntry>,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var selectedTimeframe by remember { mutableStateOf("ALL") } // ALL, TODAY, WEEK
    var selectedStatus by remember { mutableStateOf("ALL") } // ALL, INSIDE, EXITED
    var selectedType by remember { mutableStateOf("ALL") } // ALL, DELIVERY, GUEST, SERVICE, CAB
    var isCopied by remember { mutableStateOf(false) }

    // Filter visitors based on manager selections
    val filteredVisitors = remember(visitors, selectedTimeframe, selectedStatus, selectedType) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis
        val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)

        visitors.filter { v ->
            val timeframeMatch = when (selectedTimeframe) {
                "TODAY" -> v.timestamp >= startOfToday
                "WEEK" -> v.timestamp >= sevenDaysAgo
                else -> true
            }

            val statusMatch = when (selectedStatus) {
                "INSIDE" -> v.status == "INSIDE"
                "EXITED" -> v.status == "EXITED"
                else -> true
            }

            val typeMatch = when (selectedType) {
                "DELIVERY" -> v.visitorType.equals("Delivery", ignoreCase = true)
                "GUEST" -> v.visitorType.equals("Guest", ignoreCase = true)
                "SERVICE" -> v.visitorType.contains("Service", ignoreCase = true)
                "CAB" -> v.visitorType.contains("Cab", ignoreCase = true)
                else -> true
            }

            timeframeMatch && statusMatch && typeMatch
        }
    }

    val csvContent = remember(filteredVisitors) {
        CsvExportUtil.generateVisitorCsv(filteredVisitors)
    }

    val fileName = remember(selectedTimeframe) {
        CsvExportUtil.generateFileName()
    }

    val estimatedBytes = remember(csvContent) {
        csvContent.toByteArray(Charsets.UTF_8).size
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("visitor_csv_export_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Export Visitor CSV Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Auditable records for property management",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                // Scrollable filters and preview content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Filter Section 1: Timeframe
                    Text(
                        text = "1. Export Timeframe",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All Records (${visitors.size})",
                            "TODAY" to "Today Only",
                            "WEEK" to "Past 7 Days"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedTimeframe == key,
                                onClick = { selectedTimeframe = key },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("csv_filter_timeframe_$key")
                            )
                        }
                    }

                    // Filter Section 2: Visitor Status
                    Text(
                        text = "2. Filter by Gate Status",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All Statuses",
                            "INSIDE" to "Active Inside",
                            "EXITED" to "Departed / Exited"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedStatus == key,
                                onClick = { selectedStatus = key },
                                label = { Text(label, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    // Metadata Box
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("MATCHING ROWS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${filteredVisitors.size} visitor logs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("FILE SIZE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${estimatedBytes} bytes (~${(estimatedBytes / 1024) + 1} KB)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "File: $fileName",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // CSV Data Monospace Preview Snippet
                    Text(
                        text = "CSV Content Preview (RFC 4180)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 120.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                                .horizontalScroll(rememberScrollState())
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = csvContent.take(800) + if (csvContent.length > 800) "\n... [${filteredVisitors.size} total records]" else "",
                                color = Color(0xFFE2E8F0),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Share / Save CSV & Copy Text
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val file = CsvExportUtil.writeCsvToFile(context, csvContent, fileName)
                            CsvExportUtil.shareCsvFile(context, file, "Share or Save Visitor Audit CSV")
                            onShowSnackbar("CSV export generated: ${file.name}")
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_csv_share_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export & Share CSV File (${filteredVisitors.size} rows)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(csvContent))
                                isCopied = true
                                onShowSnackbar("CSV data copied to clipboard!")
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_copy_btn")
                        ) {
                            Icon(
                                if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isCopied) "Copied" else "Copy CSV")
                        }

                        OutlinedButton(
                            onClick = {
                                val file = CsvExportUtil.writeCsvToFile(context, csvContent, fileName)
                                onShowSnackbar("Saved to app exports: ${file.name}")
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_csv_save_local_btn")
                        ) {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save File")
                        }
                    }
                }
            }
        }
    }
}
