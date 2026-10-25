package com.example.ocr

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReminderResult(
    val title: String,
    val description: String,
    val dateMs: Long,
    val timeStr: String,
    val category: String,
    val priority: String,
    val rawText: String,
    val detectedTitleToken: String?,
    val detectedDateToken: String?,
    val detectedTimeToken: String?
)

object OcrParser {

    fun parseText(rawInput: String): ParsedReminderResult {
        val cleanText = rawInput.trim()
        val lines = cleanText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ParsedReminderResult(
                title = "New Reminder",
                description = "",
                dateMs = System.currentTimeMillis(),
                timeStr = "09:00",
                category = "Personal",
                priority = "Medium",
                rawText = rawInput,
                detectedTitleToken = null,
                detectedDateToken = null,
                detectedTimeToken = null
            )
        }

        // 1. Title Extraction
        val titleLine = lines.firstOrNull { line ->
            !line.contains("Total", ignoreCase = true) &&
            !line.contains("Receipt", ignoreCase = true) &&
            !line.contains("Tax", ignoreCase = true) &&
            line.length in 3..60
        } ?: lines.first()

        var extractedTitle = titleLine.take(50)

        // 2. Time Extraction
        var detectedTimeStr = "09:00"
        var timeToken: String? = null
        val timeRegex = Pattern.compile("(?i)\\b([01]?[0-9]|2[0-3]):([0-5][0-9])\\s*(am|pm)?\\b|\\b([1-9]|1[0-2])\\s*(am|pm)\\b")
        val timeMatcher = timeRegex.matcher(cleanText)

        if (timeMatcher.find()) {
            timeToken = timeMatcher.group()
            detectedTimeStr = convertTo24HourTime(timeToken)
        }

        // 3. Date Extraction
        var detectedDateMs = System.currentTimeMillis()
        var dateToken: String? = null

        val dateRegex = Pattern.compile("(?i)\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})|(\\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\\s+\\d{1,2}(?:st|nd|rd|th)?,?\\s*\\d{0,4})|\\b(Today|Tomorrow|Next\\s+(?:Mon|Tue|Wed|Thu|Fri|Sat|Sun)[a-z]*)\\b")
        val dateMatcher = dateRegex.matcher(cleanText)

        if (dateMatcher.find()) {
            dateToken = dateMatcher.group()
            detectedDateMs = parseDateTokenToEpoch(dateToken)
        }

        // 4. Category Classification
        val lower = cleanText.lowercase()
        val category = when {
            lower.contains("doctor") || lower.contains("medicine") || lower.contains("pill") || lower.contains("clinic") || lower.contains("health") -> "Medicine"
            lower.contains("bill") || lower.contains("invoice") || lower.contains("due") || lower.contains("electric") || lower.contains("water") || lower.contains("pay") -> "Bills"
            lower.contains("exam") || lower.contains("study") || lower.contains("homework") || lower.contains("chapter") -> "Study"
            lower.contains("meeting") || lower.contains("sync") || lower.contains("project") || lower.contains("report") || lower.contains("presentation") || lower.contains("client") -> "Work"
            lower.contains("flight") || lower.contains("hotel") || lower.contains("passport") || lower.contains("airline") || lower.contains("ticket") -> "Travel"
            lower.contains("birthday") || lower.contains("anniversary") || lower.contains("party") || lower.contains("cake") -> "Birthday"
            lower.contains("buy") || lower.contains("grocery") || lower.contains("shopping") || lower.contains("supermarket") -> "Shopping"
            else -> "Personal"
        }

        val priority = if (lower.contains("urgent") || lower.contains("due today") || lower.contains("important") || category == "Bills" || category == "Medicine") {
            "High"
        } else {
            "Medium"
        }

        return ParsedReminderResult(
            title = extractedTitle,
            description = cleanText,
            dateMs = detectedDateMs,
            timeStr = detectedTimeStr,
            category = category,
            priority = priority,
            rawText = cleanText,
            detectedTitleToken = extractedTitle,
            detectedDateToken = dateToken,
            detectedTimeToken = timeToken
        )
    }

    private fun convertTo24HourTime(timeStr: String): String {
        return try {
            val lower = timeStr.trim().lowercase()
            if (lower.contains("pm") || lower.contains("am")) {
                val inputFormat = SimpleDateFormat("h:mm a", Locale.US)
                val inputFormatNoMin = SimpleDateFormat("h a", Locale.US)
                val outputFormat = SimpleDateFormat("HH:mm", Locale.US)
                val date = try { inputFormat.parse(timeStr) } catch (e: Exception) { inputFormatNoMin.parse(timeStr) }
                if (date != null) outputFormat.format(date) else "09:00"
            } else {
                val parts = timeStr.split(":")
                val h = parts[0].toInt().coerceIn(0, 23)
                val m = parts.getOrNull(1)?.toInt()?.coerceIn(0, 59) ?: 0
                String.format(Locale.US, "%02d:%02d", h, m)
            }
        } catch (e: Exception) {
            "09:00"
        }
    }

    private fun parseDateTokenToEpoch(token: String): Long {
        val lower = token.trim().lowercase()
        val cal = Calendar.getInstance()

        if (lower.contains("today")) {
            return cal.timeInMillis
        }
        if (lower.contains("tomorrow")) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return cal.timeInMillis
        }

        val formats = listOf(
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("MM-dd-yyyy", Locale.US),
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("MMM dd, yyyy", Locale.US),
            SimpleDateFormat("MMM dd yyyy", Locale.US),
            SimpleDateFormat("MMM dd", Locale.US)
        )

        for (format in formats) {
            try {
                val parsed = format.parse(token)
                if (parsed != null) {
                    val targetCal = Calendar.getInstance().apply { time = parsed }
                    if (targetCal.get(Calendar.YEAR) == 1970) {
                        targetCal.set(Calendar.YEAR, cal.get(Calendar.YEAR))
                    }
                    return targetCal.timeInMillis
                }
            } catch (ignored: Exception) {}
        }

        return cal.timeInMillis
    }
}
