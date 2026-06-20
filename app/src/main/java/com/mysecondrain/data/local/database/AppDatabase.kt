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
        ReminderEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class
    ],
    version = 3,           // ← 2 থেকে 3 করা হয়েছে
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun meetingDao(): MeetingDao
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun debtDao(): DebtDao

    companion object {
        const val DATABASE_NAME = "my_second_brain.db"

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS debts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        personName TEXT NOT NULL,
                        debtType TEXT NOT NULL,
                        totalAmount REAL NOT NULL,
                        paidAmount REAL NOT NULL DEFAULT 0.0,
                        reason TEXT NOT NULL DEFAULT '',
                        debtDate INTEGER NOT NULL,
                        dueDate INTEGER,
                        status TEXT NOT NULL DEFAULT 'PENDING',
                        notes TEXT NOT NULL DEFAULT '',
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL DEFAULT 0
                    )
                """)
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS debt_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        debtId INTEGER NOT NULL,
                        amount REAL NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        paymentDate INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}

class DatabaseCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
    }
}