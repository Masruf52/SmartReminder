package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, reminderDate ASC, reminderTime ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE reminderDate >= :startMs AND reminderDate <= :endMs ORDER BY isCompleted ASC, reminderTime ASC")
    fun getRemindersForDateRange(startMs: Long, endMs: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY reminderDate ASC, reminderTime ASC")
    fun getPendingReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') ORDER BY reminderDate ASC")
    fun searchReminders(query: String): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)

    @Query("SELECT COUNT(*) FROM reminders")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reminders WHERE isCompleted = 1")
    fun getCompletedCount(): Flow<Int>
}
