package dev.supersubmarine.memwatch.data

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

data class TrimResult(
    val appsTrimmed: Int,
    val freedBytes: Long,
    val capturedAtMillis: Long,
)

class SystemActions(private val context: Context, private val memoryRepository: MemoryRepository) {
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    /** Android 14+ restricts killBackgroundProcesses to the caller's own packages. */
    val canTrimOtherApps: Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun openAppDetails(packageName: String): Boolean = launch(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )

    fun openUsageAccessSettings(): Boolean =
        launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", context.packageName, null))) ||
            launch(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openDeveloperOptions(): Boolean = launch(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))

    fun trimBackground(packages: List<String>): TrimResult {
        val before = memoryRepository.snapshot().availableBytes
        var trimmed = 0
        packages.forEach { pkg ->
            runCatching { activityManager.killBackgroundProcesses(pkg) }.onSuccess { trimmed++ }
        }
        val after = memoryRepository.snapshot().availableBytes
        return TrimResult(
            appsTrimmed = trimmed,
            freedBytes = (after - before).coerceAtLeast(0),
            capturedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
