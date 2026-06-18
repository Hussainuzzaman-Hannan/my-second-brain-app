package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val eventType: String,  // BIRTHDAY, ANNIVERSARY, SCHOOL_EVENT, FAMILY_EVENT, CLASS, OTHER
    val personName: String = "",
    val eventDate: Long,    // epoch millis - stores date (day/month), year used for display
    val isYearlyRecurring: Boolean = true,
    val reminderDaysBefore: Int = 1,
    val colorHex: String = "#FF6B6B",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
