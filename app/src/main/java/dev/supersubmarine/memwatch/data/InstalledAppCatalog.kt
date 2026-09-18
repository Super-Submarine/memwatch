package dev.supersubmarine.memwatch.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/** Labels, icons and flags for packages, cached for the life of the process. */
class InstalledAppCatalog(context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private val icons = ConcurrentHashMap<String, ImageBitmap>()

    fun describe(owner: ProcessOwner, processes: List<ProcessMemory>): AppMemory = when (owner) {
        is ProcessOwner.Package -> describePackage(owner.packageName, processes)
        ProcessOwner.Native -> AppMemory(
            key = "native",
            packageName = null,
            label = "Native & kernel processes",
            icon = null,
            isSystemApp = true,
            versionName = null,
            processes = processes,
        )
        is ProcessOwner.Unresolved -> AppMemory(
            key = "proc:${owner.processName}",
            packageName = null,
            label = owner.processName,
            icon = null,
            isSystemApp = true,
            versionName = null,
            processes = processes,
        )
    }

    private fun describePackage(packageName: String, processes: List<ProcessMemory>): AppMemory {
        val info = runCatching { packageManager.getApplicationInfo(packageName, 0) }.getOrNull()
        val versionName = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull()
        return AppMemory(
            key = packageName,
            packageName = packageName,
            label = info?.let { packageManager.getApplicationLabel(it).toString() } ?: packageName,
            icon = info?.let(::iconFor),
            isSystemApp = info == null || info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
            versionName = versionName,
            processes = processes,
        )
    }

    private fun iconFor(info: ApplicationInfo): ImageBitmap? =
        icons[info.packageName] ?: runCatching {
            packageManager.getApplicationIcon(info).toBitmap(ICON_SIZE_PX, ICON_SIZE_PX).asImageBitmap()
        }.getOrNull()?.also { icons[info.packageName] = it }

    private companion object {
        const val ICON_SIZE_PX = 144
    }
}
