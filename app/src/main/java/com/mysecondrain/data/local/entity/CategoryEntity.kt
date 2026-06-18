package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val iconName: String = "folder",   // Material icon name
    val colorHex: String = "#6200EE",  // Hex color string
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
