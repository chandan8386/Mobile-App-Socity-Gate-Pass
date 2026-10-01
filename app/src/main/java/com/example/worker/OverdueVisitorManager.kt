package com.example.worker

import android.content.Context
import androidx.work.*
import com.example.data.database.SocietyDatabase
import com.example.data.entity.VisitorEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Controller to schedule background WorkManager audits and run manual on-demand checks.
 */
object OverdueVisitorManager {

    private const val PERIODIC_WORK_TAG = "overdue_visitor_periodic_work"

    /**
     * Enqueues a periodic background worker with WorkManager.
     */
    fun schedulePeriodicAudit(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<OverdueVisitorWorker>(
            15, TimeUnit.MINUTES, // Periodic interval
            5, TimeUnit.MINUTES   // Flex interval
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /**
     * Executes an immediate audit of visitors currently inside the premises against the configured threshold.
     * Inserts in-app alerts, triggers push notifications, and returns list of overdue visitors.
     */
    suspend fun runImmediateAudit(context: Context): List<Pair<VisitorEntry, Long>> = withContext(Dispatchers.IO) {
        val dao = SocietyDatabase.getInstance(context).societyDao()
        val insideVisitors = dao.getInsideVisitorsList()
        val thresholdMinutes = OverdueVisitorWorker.getThresholdMinutes(context)
        val thresholdMs = TimeUnit.MINUTES.toMillis(thresholdMinutes.toLong())
        val now = System.currentTimeMillis()

        val overdueList = mutableListOf<Pair<VisitorEntry, Long>>()

        insideVisitors.forEach { visitor ->
            val dwellMs = now - visitor.timestamp
            if (dwellMs >= thresholdMs) {
                val dwellMinutes = TimeUnit.MILLISECONDS.toMinutes(dwellMs)
                overdueList.add(visitor to dwellMinutes)

                // Insert Notification
                val alertTitle = "⚠️ Visitor Overstay Alert: ${visitor.visitorName}"
                val alertMessage = "${visitor.visitorName} (${visitor.visitorType}) has been inside flat ${visitor.flatNumber} (${visitor.tower}) for $dwellMinutes mins, exceeding the ${thresholdMinutes}m limit."

                dao.insertNotification(
                    com.example.data.entity.GateNotification(
                        title = alertTitle,
                        message = alertMessage,
                        type = "OVERSTAY_ALERT",
                        timestamp = now,
                        isRead = false,
                        flatNumber = visitor.flatNumber,
                        priority = "HIGH"
                    )
                )
            }
        }

        // Also trigger OneTimeWorkRequest to exercise the WorkManager pipeline
        val oneTimeRequest = OneTimeWorkRequestBuilder<OverdueVisitorWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueue(oneTimeRequest)

        overdueList
    }
}
