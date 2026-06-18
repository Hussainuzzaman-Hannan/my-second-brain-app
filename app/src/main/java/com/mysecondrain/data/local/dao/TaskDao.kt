package com.mysecondrain.data.local.dao

import androidx.room.*
import com.mysecondrain.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE tasks SET isDeleted = 1, updatedAt = :time WHERE id = :id")
    suspend fun softDeleteTask(id: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isCompleted = :completed, completedAt = :completedAt, updatedAt = :updatedAt, status = :status WHERE id = :id")
    suspend fun updateTaskCompletion(
        id: Long,
        completed: Boolean,
        completedAt: Long?,
        updatedAt: Long = System.currentTimeMillis(),
        status: String
    )

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("""
        SELECT * FROM tasks 
        WHERE isDeleted = 0 AND dueDate >= :startOfDay AND dueDate < :endOfDay 
        ORDER BY dueTime ASC
    """)
    fun getTasksForDay(startOfDay: Long, endOfDay: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isCompleted = 0 ORDER BY dueDate ASC, priority DESC")
    fun getPendingTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND isCompleted = 1 ORDER BY completedAt DESC")
    fun getCompletedTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY dueDate ASC")
    fun getTasksByCategory(categoryId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND priority = :priority ORDER BY dueDate ASC")
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE isDeleted = 0 AND (title LIKE :query OR description LIKE :query)
        ORDER BY createdAt DESC
    """)
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND isCompleted = 0")
    fun getPendingTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND isCompleted = 1")
    fun getCompletedTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0")
    fun getTotalTaskCount(): Flow<Int>

    @Query("""
        SELECT * FROM tasks 
        WHERE isDeleted = 0 
        AND dueDate >= :todayStart 
        AND dueDate < :tomorrowStart 
        ORDER BY dueTime ASC 
        LIMIT :limit
    """)
    fun getTodayTasksLimited(todayStart: Long, tomorrowStart: Long, limit: Int = 5): Flow<List<TaskEntity>>

    @Query("""
        SELECT * FROM tasks 
        WHERE isDeleted = 0 
        AND isCompleted = 0 
        AND dueDate BETWEEN :start AND :end
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getTasksBetween(start: Long, end: Long): Flow<List<TaskEntity>>
}
