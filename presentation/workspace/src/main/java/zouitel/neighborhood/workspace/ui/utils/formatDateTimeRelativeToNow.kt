package zouitel.neighborhood.workspace.ui.utils

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale

fun formatDateTimeRelativeToNow(dateTimeMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(dateTimeMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDateTime()
    val date = dateTime.toLocalDate()
    val today = LocalDate.now()

    return when {
        date == today -> "Today ${formatTime(dateTime)}"
        date == today.minusDays(1) -> "Yesterday ${formatTime(dateTime)}"
        date == today.plusDays(1) -> "Tomorrow ${formatTime(dateTime)}"
        isSameWeek(date, today) -> DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()).format(date) + " " + formatTime(dateTime)
        else -> DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).format(date) + " " + formatTime(dateTime)
    }
}

private fun isSameWeek(date1: LocalDate, date2: LocalDate): Boolean {
    val weekFields = WeekFields.of(Locale.getDefault())
    val week1 = date1.get(weekFields.weekOfWeekBasedYear())
    val week2 = date2.get(weekFields.weekOfWeekBasedYear())
    return week1 == week2
}

private fun formatTime(dateTime: LocalDateTime): String {
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault())
        .format(dateTime)
}

fun isPassed(data: Long): Boolean {
    val givenDateTime = Instant.ofEpochMilli(data).atZone(ZoneId.systemDefault()).toLocalDateTime()
    val now = LocalDateTime.now()
    return givenDateTime.isBefore(now)
}