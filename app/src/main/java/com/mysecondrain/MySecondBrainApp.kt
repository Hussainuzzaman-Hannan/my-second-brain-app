package com.mysecondrain

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.mysecondrain.util.LocaleHelper
import com.mysecondrain.worker.DailySummaryWorker
import com.mysecondrain.worker.ReminderCheckWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MySecondBrainApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleWorkers()
    }

    override fun attachBaseContext(base: Context) {
        // Load saved language preference
        val prefs    = base.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "English") ?: "English"
        val context  = LocaleHelper.setLocale(base, language)
        super.attachBaseContext(context)
    }

    private fun scheduleWorkers() {
        val workManager = WorkManager.getInstance(this)

        val dailyWork = PeriodicWorkRequestBuilder<DailySummaryWorker>(
            24, TimeUnit.HOURS
        ).build()
        workManager.enqueueUniquePeriodicWork(
            DailySummaryWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            dailyWork
        )

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