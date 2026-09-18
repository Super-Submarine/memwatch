package dev.supersubmarine.memwatch.data

import androidx.compose.ui.graphics.ImageBitmap

/** Everything one app currently holds in RAM, summed over all of its processes. */
data class AppMemory(
    /** Stable list key: the package name, or a synthetic id for processes that have none. */
    val key: String,
    /** Null for native/kernel processes that belong to no package. */
    val packageName: String?,
    val label: String,
    val icon: ImageBitmap?,
    val isSystemApp: Boolean,
    val versionName: String?,
    val processes: List<ProcessMemory>,
) {
    val pssBytes: Long get() = processes.sumOf { it.pssBytes }
    val processCount: Int get() = processes.size

    /**
     * The most "alive" of this app's processes decides how the whole app is described. Helper
     * processes an app runs outside the framework (e.g. via Shizuku) show up as `native`; they
     * only win when the app has nothing else running.
     */
    val tier: OomTier
        get() = processes.asSequence().map { it.tier }.filter { it != OomTier.NATIVE }.minOrNull()
            ?: OomTier.NATIVE
    val isReclaimable: Boolean get() = tier.isReclaimable
}

/** A full per-app RAM snapshot with the totals needed for headline numbers. */
data class AppMemorySnapshot(
    val apps: List<AppMemory>,
    val capturedAtMillis: Long,
) {
    val totalPssBytes: Long get() = apps.sumOf { it.pssBytes }
    val reclaimableBytes: Long get() = apps.filter { it.isReclaimable }.sumOf { it.pssBytes }
    val largestPssBytes: Long get() = apps.maxOfOrNull { it.pssBytes } ?: 0L
}
