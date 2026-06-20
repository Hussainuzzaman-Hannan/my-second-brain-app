package com.mysecondrain.data.local.dao

import androidx.room.*
import com.mysecondrain.data.local.entity.DebtEntity
import com.mysecondrain.data.local.entity.DebtPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Query("SELECT * FROM debts WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE isDeleted = 0 AND debtType = 'I_OWE' ORDER BY createdAt DESC")
    fun getDebtsIOwe(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE isDeleted = 0 AND debtType = 'OWES_ME' ORDER BY createdAt DESC")
    fun getDebtsOwedToMe(): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    fun getDebtById(id: Long): Flow<DebtEntity?>

    @Query("""
        SELECT * FROM debts 
        WHERE isDeleted = 0 AND personName LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchDebts(query: String): Flow<List<DebtEntity>>

    @Query("""
        SELECT COALESCE(SUM(totalAmount - paidAmount), 0) FROM debts 
        WHERE isDeleted = 0 AND debtType = 'I_OWE' AND status != 'PAID'
    """)
    fun getTotalIOwe(): Flow<Double>

    @Query("""
        SELECT COALESCE(SUM(totalAmount - paidAmount), 0) FROM debts 
        WHERE isDeleted = 0 AND debtType = 'OWES_ME' AND status != 'PAID'
    """)
    fun getTotalOwedToMe(): Flow<Double>

    @Insert
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Query("UPDATE debts SET isDeleted = 1 WHERE id = :id")
    suspend fun deleteDebt(id: Long)

    // ── Payments ──────────────────────────────────────────────────────────────

    @Query("SELECT * FROM debt_payments WHERE debtId = :debtId ORDER BY paymentDate DESC")
    fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPaymentEntity>>

    @Insert
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deletePayment(id: Long)

    @Query("UPDATE debts SET paidAmount = paidAmount + :amount, status = :status, updatedAt = :now WHERE id = :debtId")
    suspend fun addPaymentToDebt(debtId: Long, amount: Double, status: String, now: Long = System.currentTimeMillis())
}