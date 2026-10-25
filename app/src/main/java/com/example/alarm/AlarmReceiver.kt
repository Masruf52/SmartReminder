package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_ALARM = "com.example.smartreminder.ACTION_TRIGGER_ALARM"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId == -1L) return

        val db = AppDatabase.getDatabase(context)
        val reminderDao = db.reminderDao()
        val notificationHelper = NotificationHelper(context)
        val scheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val reminder = reminderDao.getReminderById(reminderId) ?: return@launch

            when (intent.action) {
                ACTION_TRIGGER_ALARM -> {
                    notificationHelper.showReminderNotification(reminder)
                }
                NotificationHelper.ACTION_SNOOZE -> {
                    notificationHelper.cancelNotification(reminderId)
                    // Snooze for 10 minutes
                    val snoozedDate = System.currentTimeMillis() + (10 * 60 * 1000)
                    val snoozedReminder = reminder.copy(
                        reminderDate = snoozedDate,
                        updatedAt = System.currentTimeMillis()
                    )
                    reminderDao.updateReminder(snoozedReminder)
                    scheduler.schedule(snoozedReminder)
                }
                NotificationHelper.ACTION_MARK_COMPLETE -> {
                    notificationHelper.cancelNotification(reminderId)
                    val completedReminder = reminder.copy(
                        isCompleted = true,
                        updatedAt = System.currentTimeMillis()
                    )
                    reminderDao.updateReminder(completedReminder)
                }
            }
        }
    }
}
