package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.data.database.SocietyDatabase
import com.example.data.entity.GateNotification
import com.example.data.entity.VisitorEntry
import java.util.concurrent.TimeUnit

/**
 * Background worker that scans active visitors inside the premises and dispatches
 * push notifications to residents or managers if a visitor exceeds the configured stay threshold.
 */
class OverdueVisitorWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "visitor_overstay_channel"
        const val CHANNEL_NAME = "Visitor Overstay Security Alerts"
        const val PREFS_NAME = "visitor_security_prefs"
        const val KEY_THRESHOLD_MINUTES = "stay_threshold_minutes"
        const val DEFAULT_THRESHOLD_MINUTES = 60 // 1 hour default

        fun getThresholdMinutes(context: Context): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_THRESHOLD_MINUTES, DEFAULT_THRESHOLD_MINUTES)
        }

        fun setThresholdMinutes(context: Context, minutes: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_THRESHOLD_MINUTES, minutes).apply()
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val dao = SocietyDatabase.getInstance(appContext).societyDao()
            val insideVisitors = dao.getInsideVisitorsList()
            val thresholdMins = getThresholdMinutes(appContext)
            val thresholdMs = TimeUnit.MINUTES.toMillis(thresholdMins.toLong())
            val now = System.currentTimeMillis()

            var overdueCount = 0

            createNotificationChannel()

            insideVisitors.forEach { visitor ->
                val dwellMs = now - visitor.timestamp
                if (dwellMs >= thresholdMs) {
                    overdueCount++
                    val dwellMinutes = TimeUnit.MILLISECONDS.toMinutes(dwellMs)

                    val alertTitle = "⚠️ Visitor Overstay Alert: ${visitor.visitorName}"
                    val alertMessage = "${visitor.visitorName} (${visitor.visitorType}) has been inside flat ${visitor.flatNumber} (${visitor.tower}) for $dwellMinutes mins, exceeding the ${thresholdMins}m limit."

                    // 1. Insert In-App High Priority Notification into Room Database
                    val notification = GateNotification(
                        title = alertTitle,
                        message = alertMessage,
                        type = "OVERSTAY_ALERT",
                        timestamp = now,
                        isRead = false,
                        flatNumber = visitor.flatNumber,
                        priority = "HIGH"
                    )
                    dao.insertNotification(notification)

                    // 2. Dispatch Android System Push Notification
                    sendSystemNotification(
                        notificationId = (visitor.id + 1000).toInt(),
                        title = alertTitle,
                        body = alertMessage,
                        flatNumber = visitor.flatNumber
                    )
                }
            }

            Result.success(workDataOf("overdue_count" to overdueCount))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Dispatches security notifications when visitors remain on premises past their time limit."
                enableVibration(true)
            }
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendSystemNotification(
        notificationId: Int,
        title: String,
        body: String,
        flatNumber: String
    ) {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)

        try {
            val notificationManager = NotificationManagerCompat.from(appContext)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // In Android 13+, POST_NOTIFICATIONS permission might be requested at runtime
            e.printStackTrace()
        }
    }
}
