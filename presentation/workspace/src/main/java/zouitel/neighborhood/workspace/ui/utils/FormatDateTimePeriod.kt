package zouitel.neighborhood.workspace.ui.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

fun formatDateTimePeriod(epochMillis: Long): String {
    val now = System.currentTimeMillis()
    return if (epochMillis < now) {
        calculatePastTime(epochMillis, now)
    } else {
        calculateFutureTime(epochMillis, now)
    }
}

private fun calculatePastTime(dateTime: Long, now: Long): String {
    val difference = (now - dateTime).milliseconds
    return getTimePeriodString(difference, "ago")
}

private fun calculateFutureTime(dateTime: Long, now: Long): String {
    val difference = (dateTime - now).milliseconds
    return getTimePeriodString(difference, "from now")
}

private fun getTimePeriodString(difference: Duration, suffix: String): String {
    val days = difference.toLong(DurationUnit.DAYS)
    val hours = difference.inWholeHours % 24
    val minutes = difference.inWholeMinutes % 60

    return when {
        days > 0 -> {
            if (days >= 7 && suffix == "ago") {
                formatDate(System.currentTimeMillis() - difference.inWholeMilliseconds)
            } else {
                formatDuration(days, "day", suffix)
            }
        }
        hours > 0 -> formatDuration(hours, "hour", suffix)
        minutes > 0 -> formatDuration(minutes, "minute", suffix)
        else -> if (suffix == "ago") "just now" else "in a moment"
    }
}

private fun formatDuration(value: Long, unit: String, suffix: String): String {
    val plural = if (value == 1L) "" else "s"
    return "$value $unit$plural $suffix"
}

private fun formatDate(dateTime: Long): String {
    val date = Calendar.getInstance().apply {
        timeInMillis = dateTime
    }.time
    return SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
}