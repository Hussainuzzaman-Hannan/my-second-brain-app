package com.mysecondrain.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.mysecondrain.domain.repository.TaskRepository
import com.mysecondrain.notification.ReminderReceiver
import com.mysecondrain.notification.ReminderScheduler
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import java.util.concurrent.TimeUnit

// ─── Daily Summary Worker ─────────────────────────────────────────────────────

@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            reminderScheduler.scheduleDailyReminder(8, 0)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "daily_summary_worker"

        fun enqueue(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(
                24, TimeUnit.HOURS
            ).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

// ─── Reminder Check Worker ────────────────────────────────────────────────────

@HiltWorker
class ReminderCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val taskRepository: TaskRepository,
    private val reminderScheduler: ReminderScheduler
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val now       = System.currentTimeMillis()
            val in1Hour   = now + (60 * 60 * 1000)

            taskRepository.getUpcomingTasks(now, in1Hour)
                .first()
                .forEach { task ->
                    val triggerMillis = task.dueDateTime
                        ?.atZone(ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli() ?: return@forEach

                    reminderScheduler.scheduleReminder(
                        requestCode   = task.id.toInt(),
                        triggerMillis = triggerMillis,
                        title         = "Task Due Soon ⏰",
                        message       = task.title,
                        type          = ReminderReceiver.TYPE_TASK
                    )
                }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "reminder_check_worker"

        fun enqueue(workManager: WorkManager) {
            val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}