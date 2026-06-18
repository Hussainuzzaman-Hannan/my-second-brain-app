package com.mysecondrain.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mysecondrain.data.local.dao.*
import com.mysecondrain.data.local.entity.*

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        MeetingEntity::class,
        EventEntity::class,
        NoteEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun meetingDao(): MeetingDao
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "my_second_brain.db"
    }
}

// Pre-populate default categories via DatabaseCallback
class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        // Default categories inserted via DatabaseModule using prepopulate
    }
}
