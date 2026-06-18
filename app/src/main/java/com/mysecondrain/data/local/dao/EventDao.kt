package com.mysecondrain.data.local.dao

import androidx.room.*
import com.mysecondrain.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("UPDATE events SET isDeleted = 1, updatedAt = :time WHERE id = :id")
    suspend fun softDeleteEvent(id: Long, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM events WHERE isDeleted = 0 ORDER BY eventDate ASC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id AND isDeleted = 0")
    fun getEventById(id: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE isDeleted = 0 AND eventType = :type ORDER BY eventDate ASC")
    fun getEventsByType(type: String): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events 
        WHERE isDeleted = 0 AND (title LIKE :query OR personName LIKE :query OR description LIKE :query)
    """)
    fun searchEvents(query: String): Flow<List<EventEntity>>

    @Query("""
        SELECT * FROM events 
        WHERE isDeleted = 0 AND eventDate >= :startOfDay AND eventDate < :endOfDay
    """)
    fun getEventsForDay(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>>
}
