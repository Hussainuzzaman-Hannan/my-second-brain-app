package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val categoryId: Long = 0,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, URGENT
    val status: String = "PENDING",  // PENDING, IN_PROGRESS, COMPLETED
    val dueDate: Long? = null,       // epoch millis
    val dueTime: Long? = null,       // epoch millis
    val reminderTime: Long? = null,  // epoch millis
    val reminderOffset: String = "NONE", // NONE, 15MIN, 30MIN, 1HR, 1DAY, CUSTOM
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
