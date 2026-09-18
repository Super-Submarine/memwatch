package dev.supersubmarine.memwatch.data

data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val lowMemoryThresholdBytes: Long,
    val lowMemory: Boolean,
    val cachedBytes: Long,
    val swapTotalBytes: Long,
    val swapUsedBytes: Long,
    /** Proportional set size of MemWatch's own process — the only process an app may measure. */
    val selfPssBytes: Long,
    val capturedAtMillis: Long,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)
    val usedFraction: Float get() = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes
    val availableFraction: Float get() = 1f - usedFraction

    /** Page cache the kernel can drop instantly; it sits inside [availableBytes]. */
    val reclaimableBytes: Long get() = cachedBytes.coerceIn(0, availableBytes)
    val freeBytes: Long get() = (availableBytes - reclaimableBytes).coerceAtLeast(0)

    val pressure: MemoryPressure
        get() = when {
            lowMemory || availableBytes < lowMemoryThresholdBytes -> MemoryPressure.CRITICAL
            availableFraction < 0.2f -> MemoryPressure.HIGH
            else -> MemoryPressure.NORMAL
        }
}

enum class MemoryPressure { NORMAL, HIGH, CRITICAL }
