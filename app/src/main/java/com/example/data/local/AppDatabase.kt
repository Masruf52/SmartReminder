package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.Category
import com.example.data.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Reminder::class, Category::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_reminder_db"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDefaultCategories(database.categoryDao())
                        populateSampleReminders(database.reminderDao())
                    }
                }
            }

            private suspend fun populateDefaultCategories(categoryDao: CategoryDao) {
                val defaultCategories = listOf(
                    Category(name = "Work", iconName = "work", colorHex = "#6750A4", isDefault = true),
                    Category(name = "Personal", iconName = "person", colorHex = "#005AC1", isDefault = true),
                    Category(name = "Urgent", iconName = "priority_high", colorHex = "#B3261E", isDefault = true),
                    Category(name = "Study", iconName = "book", colorHex = "#00505F", isDefault = true),
                    Category(name = "Shopping", iconName = "shopping_cart", colorHex = "#8A4CFC", isDefault = true),
                    Category(name = "Medicine", iconName = "favorite", colorHex = "#BA1A1A", isDefault = true),
                    Category(name = "Bills", iconName = "receipt_long", colorHex = "#006A7C", isDefault = true),
                    Category(name = "Birthday", iconName = "cake", colorHex = "#D2BBFF", isDefault = true),
                    Category(name = "Meeting", iconName = "groups", colorHex = "#4D44E3", isDefault = true),
                    Category(name = "Travel", iconName = "flight", colorHex = "#004E5C", isDefault = true)
                )
                categoryDao.insertCategories(defaultCategories)
            }

            private suspend fun populateSampleReminders(reminderDao: ReminderDao) {
                val now = System.currentTimeMillis()
                val samples = listOf(
                    Reminder(
                        title = "Prepare Q3 Strategy Presentation",
                        description = "Review key deliverables and revenue statistics with the team.",
                        category = "Work",
                        priority = "High",
                        reminderDate = now,
                        reminderTime = "10:00",
                        reminderType = "BOTH",
                        repeatType = "WEEKLY",
                        isCompleted = false
                    ),
                    Reminder(
                        title = "Pick up Groceries & Organic Produce",
                        description = "Fresh vegetables, almond milk, and whole grain bread.",
                        category = "Shopping",
                        priority = "Medium",
                        reminderDate = now,
                        reminderTime = "17:30",
                        reminderType = "NOTIFICATION",
                        repeatType = "NEVER",
                        isCompleted = false
                    ),
                    Reminder(
                        title = "Take Morning Vitamins & Calcium",
                        description = "Take after breakfast with water.",
                        category = "Medicine",
                        priority = "High",
                        reminderDate = now,
                        reminderTime = "08:30",
                        reminderType = "NOTIFICATION",
                        repeatType = "DAILY",
                        isCompleted = true
                    ),
                    Reminder(
                        title = "Electricity Bill Payment Due",
                        description = "Pay via online banking before end of day.",
                        category = "Bills",
                        priority = "High",
                        reminderDate = now + 86400000L, // Tomorrow
                        reminderTime = "14:00",
                        reminderType = "ALARM",
                        repeatType = "MONTHLY",
                        isCompleted = false
                    )
                )
                samples.forEach { reminderDao.insertReminder(it) }
            }
        }
    }
}
