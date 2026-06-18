package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val categoryId: Long = 0,
    val noteType: String = "PERSONAL",  // PERSONAL, TEACHING, FAMILY, QUICK
    val isPinned: Boolean = false,
    val colorHex: String = "#FFFFFF",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
