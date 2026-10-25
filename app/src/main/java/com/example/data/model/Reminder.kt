package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Personal",
    val priority: String = "Medium", // High, Medium, Low
    val reminderDate: Long = System.currentTimeMillis(),
    val reminderTime: String = "09:00", // "HH:mm"
    val reminderType: String = "NOTIFICATION", // NOTIFICATION, ALARM, BOTH
    val repeatType: String = "NEVER", // NEVER, DAILY, WEEKLY, MONTHLY, YEARLY, WEEKDAYS, WEEKENDS
    val attachmentPath: String? = null,
    val attachmentType: String? = null, // IMAGE, PDF, VOICE
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
