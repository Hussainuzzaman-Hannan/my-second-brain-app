package com.mysecondrain.di

import android.content.Context
import androidx.room.Room
import com.mysecondrain.data.local.dao.*
import com.mysecondrain.data.local.database.AppDatabase
import com.mysecondrain.data.local.entity.CategoryEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val defaultCategories = listOf(
        CategoryEntity(name = "Teaching",  iconName = "school",                  colorHex = "#1565C0", isDefault = true),
        CategoryEntity(name = "Family",    iconName = "family_home",             colorHex = "#2E7D32", isDefault = true),
        CategoryEntity(name = "Personal",  iconName = "person",                  colorHex = "#6A1B9A", isDefault = true),
        CategoryEntity(name = "Meeting",   iconName = "groups",                  colorHex = "#E65100", isDefault = true),
        CategoryEntity(name = "Colleague", iconName = "handshake",               colorHex = "#00695C", isDefault = true),
        CategoryEntity(name = "Relative",  iconName = "diversity_3",             colorHex = "#F57F17", isDefault = true),
        CategoryEntity(name = "Finance",   iconName = "account_balance_wallet",  colorHex = "#880E4F", isDefault = true),
        CategoryEntity(name = "Health",    iconName = "favorite",                colorHex = "#C62828", isDefault = true),
        CategoryEntity(name = "Shopping",  iconName = "shopping_cart",           colorHex = "#4E342E", isDefault = true),
        CategoryEntity(name = "Other",     iconName = "more_horiz",              colorHex = "#546E7A", isDefault = true)
    )

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)   // ← এটা যোগ করা হয়েছে
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.beginTransaction()
                    try {
                        defaultCategories.forEach { cat ->
                            db.execSQL(
                                "INSERT INTO categories (name, iconName, colorHex, isDefault, createdAt) VALUES (?, ?, ?, ?, ?)",
                                arrayOf(
                                    cat.name,
                                    cat.iconName,
                                    cat.colorHex,
                                    if (cat.isDefault) 1 else 0,
                                    System.currentTimeMillis()
                                )
                            )
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                }
            })
            .build()

    @Provides fun provideTaskDao(db: AppDatabase): TaskDao         = db.taskDao()
    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideMeetingDao(db: AppDatabase): MeetingDao   = db.meetingDao()
    @Provides fun provideEventDao(db: AppDatabase): EventDao       = db.eventDao()
    @Provides fun provideNoteDao(db: AppDatabase): NoteDao         = db.noteDao()
    @Provides fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
    @Provides fun provideDebtDao(db: AppDatabase): DebtDao         = db.debtDao()
}