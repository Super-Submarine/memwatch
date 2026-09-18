package dev.supersubmarine.memwatch.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.TimeUnit

class AppUsageRepository(private val context: Context) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val appOps = context.getSystemService(AppOpsManager::class.java)
    private val packageManager: PackageManager = context.packageManager

    fun hasUsageAccess(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun recentApps(window: Long = TimeUnit.HOURS.toMillis(24)): List<AppUsage> {
        val end = System.currentTimeMillis()
        val begin = end - window
        val stats: Map<String, UsageStats> = usageStatsManager.queryAndAggregateUsageStats(begin, end)
        return stats.values
            .asSequence()
            .filter { it.packageName != context.packageName }
            .mapNotNull { toAppUsage(it) }
            .filter { it.lastActivityMillis >= begin }
            .sortedByDescending { it.lastActivityMillis }
            .toList()
    }

    private fun toAppUsage(stats: UsageStats): AppUsage? {
        val appInfo = runCatching { packageManager.getApplicationInfo(stats.packageName, 0) }.getOrNull() ?: return null
        val versionName = runCatching { packageManager.getPackageInfo(stats.packageName, 0).versionName }.getOrNull()
        val icon = runCatching {
            packageManager.getApplicationIcon(appInfo).toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
        }.getOrNull()
        val serviceMillis = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) stats.lastTimeForegroundServiceUsed else 0L
        return AppUsage(
            packageName = stats.packageName,
            label = packageManager.getApplicationLabel(appInfo).toString(),
            icon = icon,
            isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            lastTimeUsedMillis = stats.lastTimeUsed,
            totalForegroundMillis = stats.totalTimeInForeground,
            lastBackgroundServiceMillis = serviceMillis,
            versionName = versionName,
        )
    }

    private companion object {
        const val ICON_SIZE_PX = 144
    }
}
