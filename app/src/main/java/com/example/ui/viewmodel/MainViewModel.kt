package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Category
import com.example.data.model.Reminder
import com.example.data.repository.ReminderRepository
import com.example.ocr.OcrParser
import com.example.ocr.ParsedReminderResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository(application)

    val allReminders: StateFlow<List<Reminder>> = repository.allReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingReminders: StateFlow<List<Reminder>> = repository.pendingReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedReminders: StateFlow<List<Reminder>> = repository.completedReminders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = repository.totalCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val completedCount: StateFlow<Int> = repository.completedCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    // Selected Date for Calendar Screen
    private val _selectedDateMs = MutableStateFlow(System.currentTimeMillis())
    val selectedDateMs: StateFlow<Long> = _selectedDateMs.asStateFlow()

    // OCR extracted result for preview before saving
    private val _parsedResult = MutableStateFlow<ParsedReminderResult?>(null)
    val parsedResult: StateFlow<ParsedReminderResult?> = _parsedResult.asStateFlow()

    // Dark Theme preference state
    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Filtered Reminders List
    val filteredReminders: StateFlow<List<Reminder>> = combine(
        allReminders,
        searchQuery,
        selectedCategoryFilter
    ) { reminders, query, categoryFilter ->
        reminders.filter { reminder ->
            val matchesQuery = query.isBlank() ||
                reminder.title.contains(query, ignoreCase = true) ||
                reminder.description.contains(query, ignoreCase = true) ||
                reminder.category.contains(query, ignoreCase = true)
            val matchesCategory = categoryFilter == null || reminder.category == categoryFilter
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Reminders for Selected Calendar Date
    val calendarReminders: StateFlow<List<Reminder>> = _selectedDateMs.flatMapLatest { dateMs ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val endMs = cal.timeInMillis - 1
        repository.getRemindersForDateRange(startMs, endMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Set or toggle category filter for filtering reminders by custom category (e.g. "Work", "Personal", "Urgent")
     */
    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    /**
     * Clear category filter
     */
    fun clearCategoryFilter() {
        _selectedCategoryFilter.value = null
    }

    /**
     * Helper to get flow of reminders filtered by a specific category name
     */
    fun getRemindersByCategory(categoryName: String): StateFlow<List<Reminder>> {
        return combine(allReminders) { (reminders) ->
            reminders.filter { it.category.equals(categoryName, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSelectedDate(dateMs: Long) {
        _selectedDateMs.value = dateMs
    }

    fun toggleDarkTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
    }

    fun processOcrInput(rawText: String) {
        val parsed = OcrParser.parseText(rawText)
        _parsedResult.value = parsed
    }

    fun clearParsedResult() {
        _parsedResult.value = null
    }

    /**
     * Create a new reminder with a specified custom or standard category
     */
    fun createReminder(
        title: String,
        description: String = "",
        category: String = "Work",
        priority: String = "Medium",
        reminderDate: Long = System.currentTimeMillis(),
        reminderTime: String = "09:00",
        reminderType: String = "NOTIFICATION",
        repeatType: String = "NEVER",
        attachmentType: String? = null,
        onComplete: () -> Unit = {}
    ) {
        val reminder = Reminder(
            title = title,
            description = description,
            category = category,
            priority = priority,
            reminderDate = reminderDate,
            reminderTime = reminderTime,
            reminderType = reminderType,
            repeatType = repeatType,
            attachmentType = attachmentType,
            isCompleted = false
        )
        saveReminder(reminder, onComplete)
    }

    /**
     * Save or insert a reminder
     */
    fun saveReminder(reminder: Reminder, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveReminder(reminder)
            onComplete()
        }
    }

    /**
     * Edit or update an existing reminder
     */
    fun updateReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder)
        }
    }

    fun toggleCompletion(reminder: Reminder) {
        viewModelScope.launch {
            repository.toggleReminderCompletion(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    /**
     * Create/add a custom category (e.g. "Work", "Personal", "Urgent" or custom name)
     */
    fun addCustomCategory(name: String, iconName: String = "folder", colorHex: String = "#6750A4") {
        if (name.isBlank()) return
        viewModelScope.launch {
            val category = Category(name = name.trim(), iconName = iconName, colorHex = colorHex, isDefault = false)
            repository.insertCategory(category)
        }
    }

    /**
     * Edit/update an existing custom category
     */
    fun editCustomCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    /**
     * Delete a custom category
     */
    fun deleteCustomCategory(category: Category) {
        viewModelScope.launch {
            repository.deleteCategory(category)
        }
    }
}
