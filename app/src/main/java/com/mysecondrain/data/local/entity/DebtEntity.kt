package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val personName: String,
    val debtType: String,          // I_OWE, OWES_ME
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val reason: String = "",
    val debtDate: Long,
    val dueDate: Long? = null,
    val status: String = "PENDING",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)