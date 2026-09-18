package dev.supersubmarine.memwatch.data

import androidx.compose.ui.graphics.ImageBitmap

data class AppUsage(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystemApp: Boolean,
    val lastTimeUsedMillis: Long,
    val totalForegroundMillis: Long,
    val lastBackgroundServiceMillis: Long,
    val versionName: String?,
) {
    val lastActivityMillis: Long get() = maxOf(lastTimeUsedMillis, lastBackgroundServiceMillis)

    val ranInBackgroundRecently: Boolean
        get() = lastBackgroundServiceMillis > 0 && lastBackgroundServiceMillis >= lastTimeUsedMillis
}
