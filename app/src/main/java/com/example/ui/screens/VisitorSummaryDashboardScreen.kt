package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.VisitorEntry
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.max
import androidx.compose.ui.platform.LocalContext
import com.example.worker.OverdueVisitorManager
import com.example.worker.OverdueVisitorWorker
import kotlinx.coroutines.launch

/**
 * Data model for aggregated visitor analytics.
 */
data class VisitorDashboardAnalytics(
    val totalDailyEntries: Int,
    val currentlyActiveInside: Int,
    val totalExited: Int,
    val averageDurationMinutes: Long,
    val shortestDurationMinutes: Long,
    val longestDurationMinutes: Long,
    val hourlyBuckets: List<Pair<String, Int>>,
    val activeByType: Map<String, Int>,
    val averageDurationByType: Map<String, Long>
)

/**
 * Formats duration in minutes into a clean human-readable string (e.g. "45m" or "1h 30m").
 */
fun formatDurationMinutes(minutes: Long): String {
    if (minutes <= 0) return "< 1m"
    val hours = minutes / 60
    val remMins = minutes % 60
    return when {
        hours == 0L -> "${remMins}m"
        remMins == 0L -> "${hours}h"
        else -> "${hours}h ${remMins}m"
    }
}

/**
 * Computes visitor metrics from a list of VisitorEntry records.
 */
fun computeVisitorAnalytics(
    visitors: List<VisitorEntry>,
    filterTimeframe: String // "TODAY", "WEEK", "ALL"
): VisitorDashboardAnalytics {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val startOfToday = calendar.timeInMillis
    val sevenDaysAgo = now - TimeUnit.DAYS.toMillis(7)

    val scopedVisitors = when (filterTimeframe) {
        "TODAY" -> visitors.filter { it.timestamp >= startOfToday }
        "WEEK" -> visitors.filter { it.timestamp >= sevenDaysAgo }
        else -> visitors
    }

    val totalDailyEntries = scopedVisitors.size
    val activeInside = scopedVisitors.filter { it.status == "INSIDE" }
    val currentlyActiveCount = activeInside.size

    val completedVisits = scopedVisitors.filter { it.exitTimestamp != null && it.exitTimestamp!! > it.timestamp }
    val totalExited = scopedVisitors.count { it.status == "EXITED" }

    val durationsInMins = completedVisits.map {
        TimeUnit.MILLISECONDS.toMinutes(it.exitTimestamp!! - it.timestamp)
    }

    val avgDuration = if (durationsInMins.isNotEmpty()) durationsInMins.average().toLong() else 0L
    val shortest = if (durationsInMins.isNotEmpty()) durationsInMins.minOrNull() ?: 0L else 0L
    val longest = if (durationsInMins.isNotEmpty()) durationsInMins.maxOrNull() ?: 0L else 0L

    // Hourly Entry Buckets (Morning, Noon, Afternoon, Evening, Night)
    val bucketNames = listOf(
        "06-10 AM" to (6..9),
        "10-02 PM" to (10..13),
        "02-06 PM" to (14..17),
        "06-10 PM" to (18..21),
        "10-06 AM" to (22..23) // plus 0..5
    )

    val hourlyBuckets = bucketNames.map { (label, range) ->
        val count = scopedVisitors.count { v ->
            val cal = Calendar.getInstance().apply { timeInMillis = v.timestamp }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            if (label.startsWith("10-06")) {
                hour >= 22 || hour < 6
            } else {
                hour in range
            }
        }
        label to count
    }

    // Active Visitors grouped by Type
    val activeByType = activeInside
        .groupBy { it.visitorType }
        .mapValues { it.value.size }

    // Average Duration by Type
    val averageDurationByType = completedVisits
        .groupBy { it.visitorType }
        .mapValues { entry ->
            val mins = entry.value.map { TimeUnit.MILLISECONDS.toMinutes(it.exitTimestamp!! - it.timestamp) }
            if (mins.isNotEmpty()) mins.average().toLong() else 0L
        }

    return VisitorDashboardAnalytics(
        totalDailyEntries = totalDailyEntries,
        currentlyActiveInside = currentlyActiveCount,
        totalExited = totalExited,
        averageDurationMinutes = avgDuration,
        shortestDurationMinutes = shortest,
        longestDurationMinutes = longest,
        hourlyBuckets = hourlyBuckets,
        activeByType = activeByType,
        averageDurationByType = averageDurationByType
    )
}

/**
 * Visitor Summary Dashboard displaying total daily entries, active inside,
 * and average visit duration using charts.
 */
@Composable
fun VisitorSummaryDashboardScreen(
    visitors: List<VisitorEntry>,
    onOpenQrScanner: () -> Unit,
    onViewRecentLogs: () -> Unit,
    onExportCsv: () -> Unit = {},
    onOpenIntercom: () -> Unit = {},
    onShowSnackbar: (String) -> Unit = {}
) {
    var timeframeFilter by remember { mutableStateOf("TODAY") } // TODAY, WEEK, ALL

    val analytics = remember(visitors, timeframeFilter) {
        computeVisitorAnalytics(visitors, timeframeFilter)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("visitor_summary_dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dashboard Header & Timeframe Switcher
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Visitor Gate Analytics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Real-time summary, dwell time & entry traffic",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = onExportCsv,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("dashboard_export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        FilledTonalButton(
                            onClick = onOpenQrScanner,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("dashboard_qr_scanner_btn")
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR Exit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onOpenIntercom,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("dashboard_intercom_btn")
                        ) {
                            Icon(Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Intercom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Timeframe Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "TODAY" to "Today's Summary",
                        "WEEK" to "Past 7 Days",
                        "ALL" to "All-Time Records"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = timeframeFilter == key,
                            onClick = { timeframeFilter = key },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (timeframeFilter == key) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("timeframe_filter_$key")
                        )
                    }
                }
            }
        }

        // 2. Primary KPI Summary Cards Row (Total Daily Entries, Active Inside, Avg Duration)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // KPI 1: TOTAL DAILY ENTRIES
                KpiMetricCard(
                    title = if (timeframeFilter == "TODAY") "Daily Entries" else "Total Entries",
                    value = "${analytics.totalDailyEntries}",
                    subtitle = "${analytics.totalExited} departed",
                    icon = Icons.Default.Groups,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_daily_entries"
                )

                // KPI 2: ACTIVE INSIDE PREMISES
                KpiMetricCard(
                    title = "Active Inside",
                    value = "${analytics.currentlyActiveInside}",
                    subtitle = "Live on premises",
                    icon = Icons.Default.MeetingRoom,
                    accentColor = Color(0xFF16A34A),
                    isPulsing = analytics.currentlyActiveInside > 0,
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_active_inside"
                )

                // KPI 3: AVERAGE VISIT DURATION
                KpiMetricCard(
                    title = "Avg Duration",
                    value = formatDurationMinutes(analytics.averageDurationMinutes),
                    subtitle = "Turnover dwell",
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f),
                    testTag = "kpi_avg_duration"
                )
            }
        }

        // 3. CHART 1: Total Entries Distribution (Bar Chart by Time Slot)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_daily_entries_bar")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Entry Traffic by Time of Day",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Total arrival volume across hour intervals",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Bar Chart Component
                    HourlyEntriesBarChart(
                        data = analytics.hourlyBuckets,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        }

        // 4. CHART 2: Currently Active Visitors Inside Premises (Donut Ring Chart)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_active_visitors_donut")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF16A34A).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Active Visitors Inside Premises",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${analytics.currentlyActiveInside} visitors currently inside gate",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "LIVE",
                                color = Color(0xFF166534),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (analytics.currentlyActiveInside == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No active visitors currently inside",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Donut Chart Canvas
                            ActiveVisitorsDonutChart(
                                data = analytics.activeByType,
                                totalActive = analytics.currentlyActiveInside,
                                modifier = Modifier
                                    .size(130.dp)
                                    .padding(8.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Color-coded legend
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val colors = listOf(
                                    Color(0xFF2563EB),
                                    Color(0xFF16A34A),
                                    Color(0xFFD97706),
                                    Color(0xFF7C3AED),
                                    Color(0xFFDC2626)
                                )
                                analytics.activeByType.entries.forEachIndexed { index, entry ->
                                    val chipColor = colors[index % colors.size]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(chipColor)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = entry.key,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${entry.value}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = chipColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. CHART 3: Average Visit Duration by Visitor Type (Horizontal Comparison Chart)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chart_average_visit_duration")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Average Visit Duration",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Dwell time calculated across completed visits",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3E8FF)
                        ) {
                            Text(
                                text = "Avg ${formatDurationMinutes(analytics.averageDurationMinutes)}",
                                color = Color(0xFF7E22CE),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (analytics.averageDurationByType.isEmpty()) {
                        Text(
                            text = "No completed exit logs recorded yet to calculate average duration.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        // Duration Range Summary Badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DurationStatBadge(
                                label = "Fastest Turnover",
                                value = formatDurationMinutes(analytics.shortestDurationMinutes),
                                color = Color(0xFF16A34A),
                                modifier = Modifier.weight(1f)
                            )
                            DurationStatBadge(
                                label = "Longest Visit",
                                value = formatDurationMinutes(analytics.longestDurationMinutes),
                                color = Color(0xFFD97706),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category duration bars
                        val maxDuration = max(
                            1L,
                            analytics.averageDurationByType.values.maxOrNull() ?: 1L
                        )

                        analytics.averageDurationByType.entries.forEach { (type, durationMins) ->
                            val progress = (durationMins.toFloat() / maxDuration.toFloat()).coerceIn(0.05f, 1f)
                            val barColor = when (type) {
                                "Delivery" -> Color(0xFF2563EB)
                                "Cab / Ride" -> Color(0xFFD97706)
                                "Service / Repair" -> Color(0xFF7E22CE)
                                "Daily Help" -> Color(0xFF059669)
                                else -> Color(0xFF475569)
                            }

                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatDurationMinutes(durationMins),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = barColor,
                                    trackColor = barColor.copy(alpha = 0.15f),
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Background Overstay Security Worker & Threshold Controller
        item {
            OverdueSecurityWorkerCard(
                visitors = visitors,
                onShowSnackbar = onShowSnackbar
            )
        }

        // 7. Navigation Footer Link to Chronological Recent Logs
        item {
            OutlinedButton(
                onClick = onViewRecentLogs,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_recent_logs_from_dashboard")
            ) {
                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("View Full Visitor Logs Ordered by Entry Time")
            }
        }
    }
}

/**
 * KPI Metric summary display card.
 */
@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false,
    testTag: String = ""
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

/**
 * Hourly / Time slot Bar Chart drawn with Jetpack Compose Canvas.
 */
@Composable
fun HourlyEntriesBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxCount = max(1, data.maxOfOrNull { it.second } ?: 1)
    val barColor = MaterialTheme.colorScheme.primary
    val peakColor = Color(0xFF2563EB)
    val lightTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    Column(modifier = modifier) {
        // Canvas Bars
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val totalBars = data.size
                if (totalBars == 0) return@Canvas

                val slotWidth = canvasWidth / totalBars
                val barWidth = slotWidth * 0.5f

                data.forEachIndexed { index, pair ->
                    val count = pair.second
                    val barHeight = (count.toFloat() / maxCount.toFloat()) * (canvasHeight - 20.dp.toPx())
                    val left = index * slotWidth + (slotWidth - barWidth) / 2
                    val top = canvasHeight - barHeight

                    // Background track bar
                    drawRoundRect(
                        color = lightTrackColor,
                        topLeft = Offset(left, 0f),
                        size = Size(barWidth, canvasHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                    )

                    // Active value bar with rounded top
                    if (count > 0) {
                        val isPeak = count == maxCount
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    if (isPeak) Color(0xFF3B82F6) else barColor,
                                    if (isPeak) Color(0xFF1D4ED8) else barColor.copy(alpha = 0.8f)
                                )
                            ),
                            topLeft = Offset(left, top),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Labels Row beneath bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach { (label, count) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$count",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (count > 0) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Donut Ring Chart showing active visitors partition by visitor type.
 */
@Composable
fun ActiveVisitorsDonutChart(
    data: Map<String, Int>,
    totalActive: Int,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color(0xFF2563EB),
        Color(0xFF16A34A),
        Color(0xFFD97706),
        Color(0xFF7C3AED),
        Color(0xFFDC2626)
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2
            val centerOffset = Offset(size.width / 2, size.height / 2)

            if (totalActive <= 0) {
                drawCircle(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )
                return@Canvas
            }

            var startAngle = -90f
            var colorIndex = 0

            data.forEach { (_, count) ->
                val sweepAngle = (count.toFloat() / totalActive.toFloat()) * 360f
                drawArc(
                    color = colors[colorIndex % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle - 2f, // slight gap between segments
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweepAngle
                colorIndex++
            }
        }

        // Center Text with total active
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$totalActive",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Inside",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DurationStatBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Interactive card displaying the background WorkManager security worker status,
 * configurable stay threshold selector, active overstay warnings, and a manual test trigger.
 */
@Composable
fun OverdueSecurityWorkerCard(
    visitors: List<VisitorEntry>,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var thresholdMinutes by remember {
        mutableIntStateOf(OverdueVisitorWorker.getThresholdMinutes(context))
    }
    var isRunningAudit by remember { mutableStateOf(false) }
    var lastAuditResult by remember { mutableStateOf<String?>(null) }

    val thresholdOptions = listOf(15, 30, 60, 120, 180)

    val now = System.currentTimeMillis()
    val thresholdMs = TimeUnit.MINUTES.toMillis(thresholdMinutes.toLong())

    // Active visitors inside exceeding configured threshold
    val overdueVisitors = remember(visitors, thresholdMinutes) {
        visitors.filter { it.status == "INSIDE" && (now - it.timestamp) >= thresholdMs }
            .map { v ->
                val dwellMins = TimeUnit.MILLISECONDS.toMinutes(now - v.timestamp)
                v to dwellMins
            }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("overdue_security_worker_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEA580C).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Overstay Security Worker",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Background WorkManager • 15m periodic sync",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (overdueVisitors.isNotEmpty()) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = if (overdueVisitors.isNotEmpty()) "${overdueVisitors.size} OVERSTAY" else "SECURE",
                        color = if (overdueVisitors.isNotEmpty()) Color(0xFFB91C1C) else Color(0xFF15803D),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Pre-configured Threshold Selector
            Text(
                text = "Pre-Configured Maximum Stay Threshold:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                thresholdOptions.forEach { minutes ->
                    val isSelected = thresholdMinutes == minutes
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            thresholdMinutes = minutes
                            OverdueVisitorWorker.setThresholdMinutes(context, minutes)
                            onShowSnackbar("Overstay threshold set to ${minutes}m limit")
                        },
                        label = {
                            Text(
                                text = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("threshold_chip_${minutes}m")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Overdue Status & Visitor List
            if (overdueVisitors.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "All visitors currently inside are within the $thresholdMinutes-minute security threshold.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, Color(0xFFFDBA74)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "⚠️ Overstay Warning (${overdueVisitors.size} active visitors exceed ${thresholdMinutes}m):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC2410C)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        overdueVisitors.forEach { (visitor, dwellMins) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${visitor.visitorName} (${visitor.visitorType}) - Flat ${visitor.flatNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = "${dwellMins}m inside",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC2410C)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Button: Trigger Immediate Background Audit
            FilledTonalButton(
                onClick = {
                    coroutineScope.launch {
                        isRunningAudit = true
                        val flagged = OverdueVisitorManager.runImmediateAudit(context)
                        isRunningAudit = false
                        val msg = if (flagged.isNotEmpty()) {
                            "⚠️ Overdue scan complete: Sent notifications for ${flagged.size} visitors!"
                        } else {
                            "✅ Overdue scan complete: All visitors within limits."
                        }
                        lastAuditResult = msg
                        onShowSnackbar(msg)
                    }
                },
                enabled = !isRunningAudit,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trigger_overstay_worker_btn")
            ) {
                if (isRunningAudit) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning active visitors...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Run Background Worker Scan Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (lastAuditResult != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = lastAuditResult!!,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
