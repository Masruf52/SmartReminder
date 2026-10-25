package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.navigation.Screen
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit
) {
    val selectedDateMs by viewModel.selectedDateMs.collectAsStateWithLifecycle()
    val agendaReminders by viewModel.calendarReminders.collectAsStateWithLifecycle()

    val cal = remember(selectedDateMs) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMs }
    }

    val monthYearStr = remember(selectedDateMs) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(selectedDateMs))
    }

    val selectedDayFormatted = remember(selectedDateMs) {
        SimpleDateFormat("EEEE, MMMM dd", Locale.US).format(Date(selectedDateMs))
    }

    val currentMonthDays = remember(selectedDateMs) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedDateMs
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0-based Sunday
        val daysList = mutableListOf<CalendarDay>()

        // Padding for previous month
        for (i in 0 until firstDayOfWeek) {
            daysList.add(CalendarDay(dayNumber = 0, isCurrentMonth = false, dateMs = 0L))
        }

        // Days of current month
        for (day in 1..maxDays) {
            val dateCal = Calendar.getInstance().apply {
                timeInMillis = calendar.timeInMillis
                set(Calendar.DAY_OF_MONTH, day)
            }
            daysList.add(CalendarDay(dayNumber = day, isCurrentMonth = true, dateMs = dateCal.timeInMillis))
        }
        daysList
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
            .testTag("calendar_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = monthYearStr,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row {
                IconButton(
                    onClick = {
                        val prev = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMs
                            add(Calendar.MONTH, -1)
                        }
                        viewModel.setSelectedDate(prev.timeInMillis)
                    },
                    modifier = Modifier.testTag("prev_month_button")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }
                IconButton(
                    onClick = {
                        val next = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMs
                            add(Calendar.MONTH, 1)
                        }
                        viewModel.setSelectedDate(next.timeInMillis)
                    },
                    modifier = Modifier.testTag("next_month_button")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Month Calendar Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Day Names Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(currentMonthDays.size) { index ->
                        val dayItem = currentMonthDays[index]
                        if (dayItem.isCurrentMonth) {
                            val isSelected = isSameDay(dayItem.dateMs, selectedDateMs)
                            val isToday = isSameDay(dayItem.dateMs, System.currentTimeMillis())

                            val bgColor = when {
                                isSelected -> MaterialTheme.colorScheme.primary
                                isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else -> Color.Transparent
                            }

                            val textColor = when {
                                isSelected -> Color.White
                                isToday -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(bgColor)
                                    .clickable { viewModel.setSelectedDate(dayItem.dateMs) }
                                    .testTag("day_${dayItem.dayNumber}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${dayItem.dayNumber}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(Color.White)
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Daily Agenda Header
        Text(
            text = selectedDayFormatted.uppercase(Locale.US),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Daily Agenda Reminders List
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (agendaReminders.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "No reminders scheduled for this day.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(agendaReminders, key = { it.id }) { reminder ->
                    ReminderItemCard(
                        reminder = reminder,
                        onToggle = { viewModel.toggleCompletion(reminder) },
                        onEdit = { onNavigate(Screen.AddEditReminder.createRoute(reminder.id)) },
                        onDelete = { viewModel.deleteReminder(reminder) }
                    )
                }
            }
        }
    }
}

private data class CalendarDay(
    val dayNumber: Int,
    val isCurrentMonth: Boolean,
    val dateMs: Long
)

private fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
