package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val reminderTime: Long,        // epoch millis
    val repeatType: String = "NONE", // NONE, DAILY, WEEKLY, MONTHLY
    val repeatInterval: Int = 0,   // how many days/weeks/months
    val entityType: String,        // TASK, MEETING, EVENT, CUSTOM
    val entityId: Long = 0,
    val isActive: Boolean = true,
    val alarmRequestCode: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
