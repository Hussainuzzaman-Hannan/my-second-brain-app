package com.mysecondrain.data.local.dao

import androidx.room.*
import com.mysecondrain.data.local.entity.MeetingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: MeetingEntity): Long

    @Update
    suspend fun updateMeeting(meeting: MeetingEntity)

    @Query("UPDATE meetings SET isDeleted = 1, updatedAt = :time WHERE id = :id")
    suspend fun softDeleteMeeting(id: Long, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM meetings WHERE isDeleted = 0 ORDER BY meetingDate ASC, meetingTime ASC")
    fun getAllMeetings(): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :id AND isDeleted = 0")
    fun getMeetingById(id: Long): Flow<MeetingEntity?>

    @Query("""
        SELECT * FROM meetings 
        WHERE isDeleted = 0 AND meetingDate >= :now 
        ORDER BY meetingDate ASC, meetingTime ASC 
        LIMIT :limit
    """)
    fun getUpcomingMeetings(now: Long, limit: Int = 5): Flow<List<MeetingEntity>>

    @Query("""
        SELECT * FROM meetings 
        WHERE isDeleted = 0 AND meetingDate >= :startOfDay AND meetingDate < :endOfDay
        ORDER BY meetingTime ASC
    """)
    fun getMeetingsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MeetingEntity>>

    @Query("""
        SELECT * FROM meetings 
        WHERE isDeleted = 0 AND (title LIKE :query OR personName LIKE :query OR location LIKE :query)
        ORDER BY meetingDate DESC
    """)
    fun searchMeetings(query: String): Flow<List<MeetingEntity>>
}
