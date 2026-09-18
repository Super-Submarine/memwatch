package dev.supersubmarine.memwatch.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import dev.supersubmarine.memwatch.shizuku.ShizukuManager

/** Hand-offs into Android Settings, the browser and the Shizuku app. Each returns whether it launched. */
class SystemActions(private val context: Context) {
    fun openAppDetails(packageName: String): Boolean = launch(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )

    fun openDeveloperOptions(): Boolean = launch(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS))

    /**
     * Android's own "Memory used by apps" screen (per-app RAM averaged by the system). The action
     * is declared by AOSP Settings but has no SDK constant; fall back to Developer options.
     */
    fun openAppMemoryUsage(): Boolean = launch(Intent(ACTION_APP_MEMORY_USAGE)) || openDeveloperOptions()

    fun canOpenAppMemoryUsage(): Boolean =
        Intent(ACTION_APP_MEMORY_USAGE).resolveActivity(context.packageManager) != null

    fun openShizukuApp(): Boolean =
        context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE)?.let(::launch) ?: false

    fun openUrl(url: String): Boolean = launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    private fun launch(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private companion object {
        const val ACTION_APP_MEMORY_USAGE = "android.settings.APP_MEMORY_USAGE"
    }
}
