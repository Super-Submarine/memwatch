package dev.supersubmarine.memwatch.data

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.os.storage.StorageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AppUsageRepository(private val context: Context) {
    private val usageStatsManager = context.getSystemService(UsageStatsManager::class.java)
    private val storageStatsManager = context.getSystemService(StorageStatsManager::class.java)
    private val appOps = context.getSystemService(AppOpsManager::class.java)
    private val packageManager: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    fun hasUsageAccess(): Boolean {
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Every app the user can open (plus anything that ran in the window), with its measured
     * storage footprint and recent activity. Both need usage access; without it Android throws.
     */
    fun apps(window: Long = TimeUnit.HOURS.toMillis(24)): List<AppUsage> {
        val end = System.currentTimeMillis()
        val begin = end - window
        val stats: Map<String, UsageStats> = usageStatsManager.queryAndAggregateUsageStats(begin, end)
        return installedApplications()
            .asSequence()
            .filter { it.packageName != context.packageName }
            .filter { info -> isUserFacing(info) || (stats[info.packageName]?.lastTimeUsed ?: 0L) >= begin }
            .map { info -> toAppUsage(info, stats[info.packageName]) }
            .toList()
    }

    private fun installedApplications(): List<ApplicationInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }

    private fun isUserFacing(info: ApplicationInfo): Boolean {
        if (!info.enabled) return false
        val system = info.flags and ApplicationInfo.FLAG_SYSTEM != 0
        val updatedSystem = info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != 0
        return !system || updatedSystem || packageManager.getLaunchIntentForPackage(info.packageName) != null
    }

    private fun toAppUsage(appInfo: ApplicationInfo, stats: UsageStats?): AppUsage {
        val packageName = appInfo.packageName
        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
        val icon = iconCache[packageName] ?: runCatching {
            packageManager.getApplicationIcon(appInfo).toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
        }.getOrNull()?.also { iconCache[packageName] = it }
        val serviceMillis = if (stats != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) stats.lastTimeForegroundServiceUsed else 0L
        return AppUsage(
            packageName = packageName,
            label = packageManager.getApplicationLabel(appInfo).toString(),
            icon = icon,
            isSystemApp = appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            lastTimeUsedMillis = stats?.lastTimeUsed ?: 0L,
            totalForegroundMillis = stats?.totalTimeInForeground ?: 0L,
            lastBackgroundServiceMillis = serviceMillis,
            versionName = versionName,
            storage = storageFor(packageName),
        )
    }

    private fun storageFor(packageName: String): AppStorage? = runCatching {
        val stats = storageStatsManager.queryStatsForPackage(StorageManager.UUID_DEFAULT, packageName, Process.myUserHandle())
        AppStorage(appBytes = stats.appBytes, dataBytes = stats.dataBytes, cacheBytes = stats.cacheBytes)
    }.getOrNull()

    private companion object {
        const val ICON_SIZE_PX = 144
    }
}
