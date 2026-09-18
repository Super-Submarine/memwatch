package dev.supersubmarine.memwatch.ui

import java.util.Locale
import java.util.concurrent.TimeUnit

private const val KIB = 1024.0
private const val MIB = KIB * 1024
private const val GIB = MIB * 1024

fun formatBytes(bytes: Long): String = when {
    bytes >= GIB * 0.9995 -> String.format(Locale.getDefault(), "%.1f GB", bytes / GIB)
    bytes >= MIB -> String.format(Locale.getDefault(), "%.0f MB", bytes / MIB)
    bytes >= KIB -> String.format(Locale.getDefault(), "%.0f KB", bytes / KIB)
    else -> "$bytes B"
}

fun formatBytesSigned(bytes: Long): String = (if (bytes >= 0) "+" else "−") + formatBytes(kotlin.math.abs(bytes))

fun formatRelativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    if (epochMillis <= 0) return "not recently"
    val diff = (now - epochMillis).coerceAtLeast(0)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours h ago"
        days == 1L -> "yesterday"
        else -> "$days days ago"
    }
}

fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    return when {
        millis < TimeUnit.MINUTES.toMillis(1) -> "under a minute"
        hours < 1 -> "$minutes min"
        else -> "${hours} h ${minutes % 60} min"
    }
}
