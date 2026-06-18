package com.mysecondrain.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mysecondrain.worker.DailySummaryWorker
import com.mysecondrain.worker.ReminderCheckWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            scheduleWorkers(context)
        }
    }

    private fun scheduleWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Daily summary — every 24 hours
        val dailyWork = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            24, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            DailySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )

        // Reminder check — every 15 minutes
        val reminderWork = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
            15, TimeUnit.MINUTES
        ).build()
        workManager.enqueueUniquePeriodicWork(
            ReminderCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            reminderWork
        )
    }
}