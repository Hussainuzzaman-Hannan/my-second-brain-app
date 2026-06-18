package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val eventType: String,
    val personName: String = "",
    val eventDate: Long,
    val isYearlyRecurring: Boolean = true,
    val isWeeklyRecurring: Boolean = false,   // ← নতুন
    val weeklyDay: String? = null,            // ← নতুন (WeekDay enum name)
    val startTime: String = "",               // ← নতুন (HH:mm)
    val endTime: String = "",                 // ← নতুন (HH:mm)
    val reminderDaysBefore: Int = 1,
    val colorHex: String = "#FF6B6B",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)