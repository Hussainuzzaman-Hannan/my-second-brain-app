package com.mysecondrain.domain.repository

import com.mysecondrain.domain.model.*
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<Task>>
    fun getPendingTasks(): Flow<List<Task>>
    fun getCompletedTasks(): Flow<List<Task>>
    fun getTodayTasks(): Flow<List<Task>>
    fun getTasksByCategory(categoryId: Long): Flow<List<Task>>
    fun searchTasks(query: String): Flow<List<Task>>
    fun getPendingTaskCount(): Flow<Int>
    fun getCompletedTaskCount(): Flow<Int>
    fun getTotalTaskCount(): Flow<Int>
    fun getTaskById(id: Long): Flow<Task?>
    fun getUpcomingTasks(start: Long, end: Long): Flow<List<Task>>
    suspend fun addTask(task: Task): Long
    suspend fun updateTask(task: Task)
    suspend fun deleteTask(id: Long)
    suspend fun toggleTaskCompletion(id: Long, completed: Boolean)
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
}

interface MeetingRepository {
    fun getAllMeetings(): Flow<List<Meeting>>
    fun getUpcomingMeetings(limit: Int = 5): Flow<List<Meeting>>
    fun getMeetingById(id: Long): Flow<Meeting?>
    fun searchMeetings(query: String): Flow<List<Meeting>>
    fun getMeetingsForDay(date: Long): Flow<List<Meeting>>
    suspend fun addMeeting(meeting: Meeting): Long
    suspend fun updateMeeting(meeting: Meeting)
    suspend fun deleteMeeting(id: Long)
}

interface EventRepository {
    fun getAllEvents(): Flow<List<Event>>
    fun getEventsByType(type: EventType): Flow<List<Event>>
    fun getEventById(id: Long): Flow<Event?>
    fun searchEvents(query: String): Flow<List<Event>>
    fun getEventsForDay(date: Long): Flow<List<Event>>
    suspend fun addEvent(event: Event): Long
    suspend fun updateEvent(event: Event)
    suspend fun deleteEvent(id: Long)
}

interface NoteRepository {
    fun getAllNotes(): Flow<List<Note>>
    fun getNotesByType(type: NoteType): Flow<List<Note>>
    fun getNoteById(id: Long): Flow<Note?>
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun addNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(id: Long)
    suspend fun togglePin(id: Long, pinned: Boolean)
}
