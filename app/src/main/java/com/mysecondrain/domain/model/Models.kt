package com.mysecondrain.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

// ─── Task ──────────────────────────────────────────────────────────────────────

enum class Priority(val label: String, val weight: Int) {
    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4)
}

enum class TaskStatus { PENDING, IN_PROGRESS, COMPLETED }

enum class ReminderOffset(val label: String) {
    NONE("No reminder"),
    MIN_15("15 minutes before"),
    MIN_30("30 minutes before"),
    HR_1("1 hour before"),
    DAY_1("1 day before"),
    CUSTOM("Custom")
}

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: Category? = null,
    val priority: Priority = Priority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val dueDate: LocalDate? = null,
    val dueDateTime: LocalDateTime? = null,
    val reminderOffset: ReminderOffset = ReminderOffset.NONE,
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

// ─── Category ──────────────────────────────────────────────────────────────────

data class Category(
    val id: Long = 0,
    val name: String,
    val iconName: String = "folder",
    val colorHex: String = "#6200EE",
    val isDefault: Boolean = false
)

// ─── Meeting ──────────────────────────────────────────────────────────────────

data class Meeting(
    val id: Long = 0,
    val title: String,
    val personName: String = "",
    val location: String = "",
    val dateTime: LocalDateTime,
    val notes: String = "",
    val reminderTime: LocalDateTime? = null,
    val isCompleted: Boolean = false
)

// ─── Event ────────────────────────────────────────────────────────────────────

enum class EventType(val label: String) {
    BIRTHDAY("Birthday"),
    ANNIVERSARY("Anniversary"),
    SCHOOL_EVENT("School Event"),
    FAMILY_EVENT("Family Event"),
    CLASS("Class"),
    OTHER("Other")
}

data class Event(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val eventType: EventType,
    val personName: String = "",
    val eventDate: LocalDate,
    val isYearlyRecurring: Boolean = true,
    val reminderDaysBefore: Int = 1,
    val colorHex: String = "#FF6B6B"
)

// ─── Note ─────────────────────────────────────────────────────────────────────

enum class NoteType(val label: String) {
    PERSONAL("Personal"),
    TEACHING("Teaching"),
    FAMILY("Family"),
    QUICK("Quick Thought")
}

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val category: Category? = null,
    val noteType: NoteType = NoteType.PERSONAL,
    val isPinned: Boolean = false,
    val colorHex: String = "#FFFFFF",
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

// ─── Dashboard Summary ────────────────────────────────────────────────────────

data class DashboardSummary(
    val todayTasks: List<Task> = emptyList(),
    val upcomingMeetings: List<Meeting> = emptyList(),
    val upcomingReminders: List<Event> = emptyList(),
    val pendingTaskCount: Int = 0,
    val completedTaskCount: Int = 0,
    val todayEvents: List<Event> = emptyList()
)
