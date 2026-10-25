package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.Reminder

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_REMINDERS_ID = "smart_reminders_channel"
        const val CHANNEL_ALARMS_ID = "smart_alarms_channel"
        const val ACTION_SNOOZE = "com.example.smartreminder.ACTION_SNOOZE"
        const val ACTION_MARK_COMPLETE = "com.example.smartreminder.ACTION_MARK_COMPLETE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val remindersChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Smart Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard reminder notifications"
                enableVibration(true)
            }

            val defaultRingtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmsChannel = NotificationChannel(
                CHANNEL_ALARMS_ID,
                "Smart Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm notifications"
                enableVibration(true)
                setSound(defaultRingtoneUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(alarmsChannel)
        }
    }

    fun showReminderNotification(reminder: Reminder) {
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_REMINDER_ID", reminder.id)
            if (reminder.reminderType == "ALARM" || reminder.reminderType == "BOTH") {
                putExtra("SHOW_ALARM_FULLSCREEN", true)
            }
        }

        val pendingMainIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze intent
        val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 1).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete intent
        val completeIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminder.id * 10 + 2).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (reminder.reminderType == "ALARM" || reminder.reminderType == "BOTH") {
            CHANNEL_ALARMS_ID
        } else {
            CHANNEL_REMINDERS_ID
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(reminder.title)
            .setContentText(if (reminder.description.isNotBlank()) reminder.description else "Category: ${reminder.category} • ${reminder.reminderTime}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${reminder.description}\nCategory: ${reminder.category} | Priority: ${reminder.priority}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingMainIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 10m", snoozePendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Complete", completePendingIntent)

        if (reminder.reminderType == "ALARM" || reminder.reminderType == "BOTH") {
            builder.setFullScreenIntent(pendingMainIntent, true)
        }

        notificationManager.notify(reminder.id.toInt(), builder.build())
    }

    fun cancelNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.toInt())
    }
}
