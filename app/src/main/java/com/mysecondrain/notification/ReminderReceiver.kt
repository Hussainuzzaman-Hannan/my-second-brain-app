package com.mysecondrain.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mysecondrain.MainActivity

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title   = intent.getStringExtra(EXTRA_TITLE)   ?: "Reminder"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have a reminder"
        val type    = intent.getStringExtra(EXTRA_TYPE)    ?: TYPE_TASK

        createNotificationChannel(context)

        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when (type) {
            TYPE_MEETING -> CHANNEL_MEETINGS
            TYPE_EVENT   -> CHANNEL_EVENTS
            else         -> CHANNEL_TASKS
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        notificationManager.notify(notificationId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            listOf(
                NotificationChannel(
                    CHANNEL_TASKS, "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_MEETINGS, "Meeting Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ),
                NotificationChannel(
                    CHANNEL_EVENTS, "Event Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ),
                NotificationChannel(
                    CHANNEL_DAILY, "Daily Summary",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            ).forEach { manager.createNotificationChannel(it) }
        }
    }

    companion object {
        const val EXTRA_TITLE           = "extra_title"
        const val EXTRA_MESSAGE         = "extra_message"
        const val EXTRA_TYPE            = "extra_type"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val TYPE_TASK             = "task"
        const val TYPE_MEETING          = "meeting"
        const val TYPE_EVENT            = "event"
        const val CHANNEL_TASKS         = "channel_tasks"
        const val CHANNEL_MEETINGS      = "channel_meetings"
        const val CHANNEL_EVENTS        = "channel_events"
        const val CHANNEL_DAILY         = "channel_daily"
    }
}