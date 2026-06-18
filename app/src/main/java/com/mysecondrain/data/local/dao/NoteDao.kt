package com.mysecondrain.data.local.dao

import androidx.room.*
import com.mysecondrain.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :time WHERE id = :id")
    suspend fun softDeleteNote(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isPinned = :pinned, updatedAt = :time WHERE id = :id")
    suspend fun togglePin(id: Long, pinned: Boolean, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id AND isDeleted = 0")
    fun getNoteById(id: Long): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND noteType = :type ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByType(type: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByCategory(categoryId: Long): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE isDeleted = 0 AND (title LIKE :query OR content LIKE :query)
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotes(query: String): Flow<List<NoteEntity>>
}
