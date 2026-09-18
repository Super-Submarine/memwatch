package dev.supersubmarine.memwatch.data

data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val lowMemoryThresholdBytes: Long,
    val lowMemory: Boolean,
    val cachedBytes: Long,
    val swapTotalBytes: Long,
    val swapUsedBytes: Long,
    val capturedAtMillis: Long,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0)
    val usedFraction: Float get() = if (totalBytes == 0L) 0f else usedBytes.toFloat() / totalBytes
    val availableFraction: Float get() = 1f - usedFraction

    val pressure: MemoryPressure
        get() = when {
            lowMemory || availableBytes < lowMemoryThresholdBytes -> MemoryPressure.CRITICAL
            availableFraction < 0.2f -> MemoryPressure.HIGH
            else -> MemoryPressure.NORMAL
        }
}

enum class MemoryPressure { NORMAL, HIGH, CRITICAL }
