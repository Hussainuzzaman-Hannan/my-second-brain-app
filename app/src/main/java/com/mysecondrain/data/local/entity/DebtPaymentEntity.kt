package com.mysecondrain.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debt_payments")
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val debtId: Long,
    val amount: Double,
    val note: String = "",
    val paymentDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)