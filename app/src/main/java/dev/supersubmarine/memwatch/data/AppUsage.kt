package dev.supersubmarine.memwatch.data

import androidx.compose.ui.graphics.ImageBitmap

/** On-device storage a package occupies, from StorageStatsManager (requires usage access). */
data class AppStorage(
    val appBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
) {
    val totalBytes: Long get() = appBytes + dataBytes + cacheBytes
}

data class AppUsage(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?,
    val isSystemApp: Boolean,
    val lastTimeUsedMillis: Long,
    val totalForegroundMillis: Long,
    val lastBackgroundServiceMillis: Long,
    val versionName: String?,
    val storage: AppStorage?,
) {
    val lastActivityMillis: Long get() = maxOf(lastTimeUsedMillis, lastBackgroundServiceMillis)

    val ranInBackgroundRecently: Boolean
        get() = lastBackgroundServiceMillis > 0 && lastBackgroundServiceMillis >= lastTimeUsedMillis

    val storageBytes: Long get() = storage?.totalBytes ?: 0L
}

enum class AppSort { STORAGE, RECENT }
