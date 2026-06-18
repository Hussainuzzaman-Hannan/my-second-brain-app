package com.mysecondrain.data.local.dao

import androidx.room.*
import com.mysecondrain.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE entityType = :type AND entityId = :entityId")
    suspend fun deleteRemindersForEntity(type: String, entityId: Long)

    @Query("SELECT * FROM reminders WHERE isActive = 1 ORDER BY reminderTime ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE entityType = :type AND entityId = :entityId")
    fun getRemindersForEntity(type: String, entityId: Long): Flow<List<ReminderEntity>>

    @Query("UPDATE reminders SET isActive = :active WHERE id = :id")
    suspend fun setReminderActive(id: Long, active: Boolean)
}
