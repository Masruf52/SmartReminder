package com.example.data.repository

import android.content.Context
import com.example.alarm.AlarmScheduler
import com.example.data.local.AppDatabase
import com.example.data.model.Category
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

class ReminderRepository(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val reminderDao = db.reminderDao()
    private val categoryDao = db.categoryDao()
    private val alarmScheduler = AlarmScheduler(context)

    val allReminders: Flow<List<Reminder>> = reminderDao.getAllReminders()
    val pendingReminders: Flow<List<Reminder>> = reminderDao.getPendingReminders()
    val completedReminders: Flow<List<Reminder>> = reminderDao.getCompletedReminders()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    val totalCount: Flow<Int> = reminderDao.getTotalCount()
    val completedCount: Flow<Int> = reminderDao.getCompletedCount()

    fun searchReminders(query: String): Flow<List<Reminder>> {
        return reminderDao.searchReminders(query)
    }

    fun getRemindersForDateRange(startMs: Long, endMs: Long): Flow<List<Reminder>> {
        return reminderDao.getRemindersForDateRange(startMs, endMs)
    }

    suspend fun getReminderById(id: Long): Reminder? {
        return reminderDao.getReminderById(id)
    }

    suspend fun saveReminder(reminder: Reminder): Long {
        val id = reminderDao.insertReminder(reminder)
        val savedReminder = reminder.copy(id = id)
        if (!savedReminder.isCompleted) {
            alarmScheduler.schedule(savedReminder)
        } else {
            alarmScheduler.cancel(id)
        }
        return id
    }

    suspend fun updateReminder(reminder: Reminder) {
        reminderDao.updateReminder(reminder)
        if (!reminder.isCompleted) {
            alarmScheduler.schedule(reminder)
        } else {
            alarmScheduler.cancel(reminder.id)
        }
    }

    suspend fun toggleReminderCompletion(reminder: Reminder) {
        val updated = reminder.copy(
            isCompleted = !reminder.isCompleted,
            updatedAt = System.currentTimeMillis()
        )
        updateReminder(updated)
    }

    suspend fun deleteReminder(reminder: Reminder) {
        alarmScheduler.cancel(reminder.id)
        reminderDao.deleteReminder(reminder)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category)
    }

    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)
    }
}
