package dev.supersubmarine.memwatch.data

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import java.io.File

class MemoryRepository(context: Context) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    fun snapshot(): MemorySnapshot {
        val info = ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo)
        val meminfo = readProcMeminfo()
        val swapTotal = meminfo["SwapTotal"] ?: 0L
        val swapFree = meminfo["SwapFree"] ?: 0L
        val self = Debug.MemoryInfo().also(Debug::getMemoryInfo)
        return MemorySnapshot(
            totalBytes = info.totalMem,
            availableBytes = info.availMem,
            lowMemoryThresholdBytes = info.threshold,
            lowMemory = info.lowMemory,
            cachedBytes = (meminfo["Cached"] ?: 0L) + (meminfo["Buffers"] ?: 0L),
            swapTotalBytes = swapTotal,
            swapUsedBytes = (swapTotal - swapFree).coerceAtLeast(0),
            selfPssBytes = self.totalPss * 1024L,
            capturedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun readProcMeminfo(): Map<String, Long> {
        val result = HashMap<String, Long>()
        val file = File("/proc/meminfo")
        if (!file.canRead()) return result
        runCatching {
            file.forEachLine { line ->
                val colon = line.indexOf(':')
                if (colon <= 0) return@forEachLine
                val key = line.substring(0, colon)
                val kb = line.substring(colon + 1).trim().substringBefore(' ').toLongOrNull()
                if (kb != null) result[key] = kb * 1024
            }
        }
        return result
    }
}
