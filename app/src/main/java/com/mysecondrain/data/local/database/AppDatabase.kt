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
    version = 2,          // ← 1 থেকে 2 করা হয়েছে
    exportSchema = false   // ← true থেকে false করা হয়েছে
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

        // Version 1 → 2 Migration
        // EventEntity তে নতুন 4টা column যোগ করা হয়েছে
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE events ADD COLUMN isWeeklyRecurring INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE events ADD COLUMN weeklyDay TEXT"
                )
                database.execSQL(
                    "ALTER TABLE events ADD COLUMN startTime TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE events ADD COLUMN endTime TEXT NOT NULL DEFAULT ''"
                )
            }
        }
    }
}

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
    }
}