package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.entity.VisitorEntry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Utility functions for generating, escaping, saving, and sharing CSV audit logs.
 */
object CsvExportUtil {

    private val csvDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    /**
     * Escapes a string to conform with RFC 4180 CSV standards.
     */
    fun escapeCsv(value: String?): String {
        if (value == null) return "\"\""
        var str = value
        if (str.contains("\"") || str.contains(",") || str.contains("\n") || str.contains("\r")) {
            str = str.replace("\"", "\"\"")
            return "\"$str\""
        }
        return str
    }

    /**
     * Converts a list of VisitorEntry records into standard CSV string content.
     */
    fun generateVisitorCsv(visitors: List<VisitorEntry>): String {
        val sb = StringBuilder()

        // CSV Header
        val headers = listOf(
            "Log ID",
            "Entry Timestamp",
            "Entry Time",
            "Exit Timestamp",
            "Exit Time",
            "Dwell Duration (Mins)",
            "Visitor Name",
            "Phone Number",
            "Visitor Type",
            "Company / Service",
            "Tower",
            "Flat Number",
            "Host Resident",
            "Vehicle Number",
            "Gate Passcode",
            "QR Pass Token",
            "Status",
            "Remarks"
        )
        sb.append(headers.joinToString(","))
        sb.append("\r\n")

        // Rows
        visitors.forEach { v ->
            val entryDateStr = csvDateFormat.format(Date(v.timestamp))
            val exitDateStr = if (v.exitTimestamp != null) csvDateFormat.format(Date(v.exitTimestamp)) else "N/A"
            val durationMins = if (v.exitTimestamp != null && v.exitTimestamp > v.timestamp) {
                TimeUnit.MILLISECONDS.toMinutes(v.exitTimestamp - v.timestamp).toString()
            } else {
                "N/A"
            }

            val row = listOf(
                v.id.toString(),
                v.timestamp.toString(),
                escapeCsv(entryDateStr),
                v.exitTimestamp?.toString() ?: "",
                escapeCsv(exitDateStr),
                durationMins,
                escapeCsv(v.visitorName),
                escapeCsv(v.visitorPhoneNumber),
                escapeCsv(v.visitorType),
                escapeCsv(v.visitorCompany),
                escapeCsv(v.tower),
                escapeCsv(v.flatNumber),
                escapeCsv(v.hostResidentName),
                escapeCsv(v.vehicleNumber.ifBlank { "Walking" }),
                escapeCsv(v.passCode),
                escapeCsv(v.effectiveQrToken),
                escapeCsv(v.status),
                escapeCsv(v.remarks)
            )
            sb.append(row.joinToString(","))
            sb.append("\r\n")
        }

        return sb.toString()
    }

    /**
     * Generates a default timestamped file name for visitor CSV exports.
     */
    fun generateFileName(): String {
        val dateStr = fileTimestampFormat.format(Date())
        return "visitor_logs_audit_$dateStr.csv"
    }

    /**
     * Writes CSV string content to a file in the app's cache directory.
     */
    fun writeCsvToFile(context: Context, csvContent: String, fileName: String = generateFileName()): File {
        val exportDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val file = File(exportDir, fileName)
        FileOutputStream(file).use { out ->
            out.write(csvContent.toByteArray(Charsets.UTF_8))
        }
        return file
    }

    /**
     * Launches Android system share sheet for exporting or saving the CSV file.
     */
    fun shareCsvFile(context: Context, file: File, title: String = "Export Visitor Audit Logs") {
        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Society Gate Visitor Audit Logs (${file.name})")
                putExtra(Intent.EXTRA_TEXT, "Attached is the visitor access and security log audit CSV report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
