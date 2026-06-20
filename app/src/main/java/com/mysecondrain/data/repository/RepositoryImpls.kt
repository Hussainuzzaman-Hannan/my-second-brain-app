package com.mysecondrain.data.repository

import com.mysecondrain.data.local.dao.*
import com.mysecondrain.data.local.database.*
import com.mysecondrain.data.local.entity.DebtPaymentEntity
import com.mysecondrain.domain.model.*
import com.mysecondrain.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// ─── Task Repository ──────────────────────────────────────────────────────────

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao
) : TaskRepository {

    private suspend fun categoryFor(id: Long): Category? =
        if (id == 0L) null else categoryDao.getCategoryById(id)?.toDomain()

    override fun getAllTasks(): Flow<List<Task>> =
        taskDao.getAllTasks().map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun getPendingTasks(): Flow<List<Task>> =
        taskDao.getPendingTasks().map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun getCompletedTasks(): Flow<List<Task>> =
        taskDao.getCompletedTasks().map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun getTodayTasks(): Flow<List<Task>> {
        val today = LocalDate.now()
        val start = today.toEpochMilli()
        val end = today.plusDays(1).toEpochMilli()
        return taskDao.getTodayTasksLimited(start, end).map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }
    }

    override fun getTasksByCategory(categoryId: Long): Flow<List<Task>> =
        taskDao.getTasksByCategory(categoryId).map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun searchTasks(query: String): Flow<List<Task>> =
        taskDao.searchTasks("%$query%").map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun getPendingTaskCount(): Flow<Int> = taskDao.getPendingTaskCount()
    override fun getCompletedTaskCount(): Flow<Int> = taskDao.getCompletedTaskCount()
    override fun getTotalTaskCount(): Flow<Int> = taskDao.getTotalTaskCount()

    override fun getTaskById(id: Long): Flow<Task?> =
        taskDao.getTaskById(id).map { it?.toDomain(it.let { t -> categoryFor(t.categoryId) }) }

    override fun getUpcomingTasks(start: Long, end: Long): Flow<List<Task>> =
        taskDao.getTasksBetween(start, end).map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override suspend fun addTask(task: Task): Long =
        taskDao.insertTask(task.toEntity())

    override suspend fun updateTask(task: Task) =
        taskDao.updateTask(task.toEntity())

    override suspend fun deleteTask(id: Long) =
        taskDao.softDeleteTask(id)

    override suspend fun toggleTaskCompletion(id: Long, completed: Boolean) {
        val completedAt = if (completed) System.currentTimeMillis() else null
        val status = if (completed) TaskStatus.COMPLETED.name else TaskStatus.PENDING.name
        taskDao.updateTaskCompletion(id, completed, completedAt, status = status)
    }
}

// ─── Category Repository ──────────────────────────────────────────────────────

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    override suspend fun addCategory(category: Category): Long =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category.toEntity())

    override suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category.toEntity())
}

// ─── Meeting Repository ───────────────────────────────────────────────────────

@Singleton
class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: MeetingDao
) : MeetingRepository {

    override fun getAllMeetings(): Flow<List<Meeting>> =
        meetingDao.getAllMeetings().map { list -> list.map { it.toDomain() } }

    override fun getUpcomingMeetings(limit: Int): Flow<List<Meeting>> =
        meetingDao.getUpcomingMeetings(System.currentTimeMillis(), limit)
            .map { list -> list.map { it.toDomain() } }

    override fun getMeetingById(id: Long): Flow<Meeting?> =
        meetingDao.getMeetingById(id).map { it?.toDomain() }

    override fun searchMeetings(query: String): Flow<List<Meeting>> =
        meetingDao.searchMeetings("%$query%").map { list -> list.map { it.toDomain() } }

    override fun getMeetingsForDay(date: Long): Flow<List<Meeting>> {
        val endOfDay = date + 86_400_000L
        return meetingDao.getMeetingsForDay(date, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addMeeting(meeting: Meeting): Long =
        meetingDao.insertMeeting(meeting.toEntity())

    override suspend fun updateMeeting(meeting: Meeting) =
        meetingDao.updateMeeting(meeting.toEntity())

    override suspend fun deleteMeeting(id: Long) =
        meetingDao.softDeleteMeeting(id)
}

// ─── Event Repository ─────────────────────────────────────────────────────────

@Singleton
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao
) : EventRepository {

    override fun getAllEvents(): Flow<List<Event>> =
        eventDao.getAllEvents().map { list -> list.map { it.toDomain() } }

    override fun getEventsByType(type: EventType): Flow<List<Event>> =
        eventDao.getEventsByType(type.name).map { list -> list.map { it.toDomain() } }

    override fun getEventById(id: Long): Flow<Event?> =
        eventDao.getEventById(id).map { it?.toDomain() }

    override fun searchEvents(query: String): Flow<List<Event>> =
        eventDao.searchEvents("%$query%").map { list -> list.map { it.toDomain() } }

    override fun getEventsForDay(date: Long): Flow<List<Event>> {
        val endOfDay = date + 86_400_000L
        return eventDao.getEventsForDay(date, endOfDay).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addEvent(event: Event): Long =
        eventDao.insertEvent(event.toEntity())

    override suspend fun updateEvent(event: Event) =
        eventDao.updateEvent(event.toEntity())

    override suspend fun deleteEvent(id: Long) =
        eventDao.softDeleteEvent(id)
}

// ─── Note Repository ──────────────────────────────────────────────────────────

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) : NoteRepository {

    private suspend fun categoryFor(id: Long): Category? =
        if (id == 0L) null else categoryDao.getCategoryById(id)?.toDomain()

    override fun getAllNotes(): Flow<List<Note>> =
        noteDao.getAllNotes().map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun getNotesByType(type: NoteType): Flow<List<Note>> =
        noteDao.getNotesByType(type.name).map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override fun getNoteById(id: Long): Flow<Note?> =
        noteDao.getNoteById(id).map { it?.toDomain(it.let { n -> categoryFor(n.categoryId) }) }

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes("%$query%").map { list ->
            list.map { it.toDomain(categoryFor(it.categoryId)) }
        }

    override suspend fun addNote(note: Note): Long =
        noteDao.insertNote(note.toEntity())

    override suspend fun updateNote(note: Note) =
        noteDao.updateNote(note.toEntity())

    override suspend fun deleteNote(id: Long) =
        noteDao.softDeleteNote(id)

    override suspend fun togglePin(id: Long, pinned: Boolean) =
        noteDao.togglePin(id, pinned)
}

// ─── Debt Repository ──────────────────────────────────────────────────────────

@Singleton
class DebtRepositoryImpl @Inject constructor(
    private val debtDao: DebtDao
) : DebtRepository {

    override fun getAllDebts(): Flow<List<Debt>> =
        debtDao.getAllDebts().map { list -> list.map { it.toDomain() } }

    override fun getDebtsIOwe(): Flow<List<Debt>> =
        debtDao.getDebtsIOwe().map { list -> list.map { it.toDomain() } }

    override fun getDebtsOwedToMe(): Flow<List<Debt>> =
        debtDao.getDebtsOwedToMe().map { list -> list.map { it.toDomain() } }

    override fun getDebtById(id: Long): Flow<Debt?> =
        debtDao.getDebtById(id).map { it?.toDomain() }

    override fun searchDebts(query: String): Flow<List<Debt>> =
        debtDao.searchDebts(query).map { list -> list.map { it.toDomain() } }

    override fun getTotalIOwe(): Flow<Double> = debtDao.getTotalIOwe()

    override fun getTotalOwedToMe(): Flow<Double> = debtDao.getTotalOwedToMe()

    override fun getPaymentsForDebt(debtId: Long): Flow<List<DebtPayment>> =
        debtDao.getPaymentsForDebt(debtId).map { list -> list.map { it.toDomain() } }

    override suspend fun addDebt(debt: Debt): Long =
        debtDao.insertDebt(debt.toEntity())

    override suspend fun updateDebt(debt: Debt) =
        debtDao.updateDebt(debt.toEntity())

    override suspend fun deleteDebt(id: Long) =
        debtDao.deleteDebt(id)

    override suspend fun addPayment(debtId: Long, amount: Double, note: String) {
        debtDao.insertPayment(
            DebtPaymentEntity(
                debtId      = debtId,
                amount      = amount,
                note        = note,
                paymentDate = LocalDate.now().toEpochMilli()
            )
        )
        val debt = debtDao.getDebtById(debtId).firstOrNull() ?: return
        val newPaidAmount = debt.paidAmount + amount
        val newStatus = when {
            newPaidAmount >= debt.totalAmount -> "PAID"
            newPaidAmount > 0                  -> "PARTIALLY_PAID"
            else                                -> "PENDING"
        }
        debtDao.addPaymentToDebt(debtId, amount, newStatus)
    }
}