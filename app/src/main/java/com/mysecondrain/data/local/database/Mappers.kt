package com.mysecondrain.data.local.database

import com.mysecondrain.data.local.entity.*
import com.mysecondrain.domain.model.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

// ─── Time helpers ─────────────────────────────────────────────────────────────

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()

fun LocalDate.toEpochMilli(): Long =
    this.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun LocalDateTime.toEpochMilli(): Long =
    this.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

// ─── Task ─────────────────────────────────────────────────────────────────────

fun TaskEntity.toDomain(category: Category? = null) = Task(
    id = id,
    title = title,
    description = description,
    category = category,
    priority = Priority.valueOf(priority),
    status = TaskStatus.valueOf(status),
    dueDate = dueDate?.toLocalDate(),
    dueDateTime = dueTime?.toLocalDateTime(),
    reminderOffset = ReminderOffset.valueOf(reminderOffset),
    isCompleted = isCompleted,
    completedAt = completedAt?.toLocalDateTime(),
    createdAt = createdAt.toLocalDateTime()
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    description = description,
    categoryId = category?.id ?: 0,
    priority = priority.name,
    status = status.name,
    dueDate = dueDate?.toEpochMilli(),
    dueTime = dueDateTime?.toEpochMilli(),
    reminderOffset = reminderOffset.name,
    isCompleted = isCompleted,
    completedAt = completedAt?.toEpochMilli(),
    updatedAt = System.currentTimeMillis()
)

// ─── Category ─────────────────────────────────────────────────────────────────

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    iconName = iconName,
    colorHex = colorHex,
    isDefault = isDefault
)

// ─── Meeting ──────────────────────────────────────────────────────────────────

fun MeetingEntity.toDomain() = Meeting(
    id = id,
    title = title,
    personName = personName,
    location = location,
    dateTime = meetingDate.toLocalDateTime(),
    notes = notes,
    reminderTime = reminderTime?.toLocalDateTime(),
    isCompleted = isCompleted
)

fun Meeting.toEntity() = MeetingEntity(
    id = id,
    title = title,
    personName = personName,
    location = location,
    meetingDate = dateTime.toEpochMilli(),
    meetingTime = dateTime.toEpochMilli(),
    notes = notes,
    reminderTime = reminderTime?.toEpochMilli(),
    isCompleted = isCompleted,
    updatedAt = System.currentTimeMillis()
)

// ─── Event ────────────────────────────────────────────────────────────────────

fun EventEntity.toDomain() = Event(
    id = id,
    title = title,
    description = description,
    eventType = EventType.valueOf(eventType),
    personName = personName,
    eventDate = eventDate.toLocalDate(),
    isYearlyRecurring = isYearlyRecurring,
    reminderDaysBefore = reminderDaysBefore,
    colorHex = colorHex
)

fun Event.toEntity() = EventEntity(
    id = id,
    title = title,
    description = description,
    eventType = eventType.name,
    personName = personName,
    eventDate = eventDate.toEpochMilli(),
    isYearlyRecurring = isYearlyRecurring,
    reminderDaysBefore = reminderDaysBefore,
    colorHex = colorHex,
    updatedAt = System.currentTimeMillis()
)

// ─── Note ─────────────────────────────────────────────────────────────────────

fun NoteEntity.toDomain(category: Category? = null) = Note(
    id = id,
    title = title,
    content = content,
    category = category,
    noteType = NoteType.valueOf(noteType),
    isPinned = isPinned,
    colorHex = colorHex,
    createdAt = createdAt.toLocalDateTime(),
    updatedAt = updatedAt.toLocalDateTime()
)

fun Note.toEntity() = NoteEntity(
    id = id,
    title = title,
    content = content,
    categoryId = category?.id ?: 0,
    noteType = noteType.name,
    isPinned = isPinned,
    colorHex = colorHex,
    updatedAt = System.currentTimeMillis()
)
