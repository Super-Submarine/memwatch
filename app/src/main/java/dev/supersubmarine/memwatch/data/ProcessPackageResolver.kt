package dev.supersubmarine.memwatch.data

import android.content.Context
import android.os.Process

/** Who owns a process: an installed package, or nothing (native daemons, unknown uids). */
sealed interface ProcessOwner {
    data class Package(val packageName: String) : ProcessOwner
    data object Native : ProcessOwner
    data class Unresolved(val processName: String) : ProcessOwner
}

/**
 * Maps a process to its package. Android names app processes `<package>` or `<package>:suffix`,
 * but `android:process` may be anything, so the uid from `ps` is the authority and the name is
 * only used to pick between packages sharing a uid.
 */
class ProcessPackageResolver(context: Context) {
    private val packageManager = context.packageManager

    fun resolve(process: ProcessMemory, uid: Int?): ProcessOwner {
        val baseName = process.name.substringBefore(':')
        if (process.tier == OomTier.NATIVE) {
            return installedPrefix(baseName)?.let { ProcessOwner.Package(it) } ?: ProcessOwner.Native
        }
        val sharingUid = uid?.let { packageManager.getPackagesForUid(it) }.orEmpty().toList()
        val byName = sharingUid.firstOrNull { baseName == it || baseName.startsWith("$it.") }
        val chosen = byName
            ?: sharingUid.singleOrNull()
            ?: installedPrefix(baseName)
            ?: sharingUid.firstOrNull()?.takeIf { uid != null && uid >= Process.FIRST_APPLICATION_UID }
            ?: systemPackageFor(uid, sharingUid)
        return chosen?.let { ProcessOwner.Package(it) } ?: ProcessOwner.Unresolved(process.name)
    }

    private fun installedPrefix(baseName: String): String? {
        var candidate = baseName
        while (candidate.contains('.')) {
            if (runCatching { packageManager.getPackageInfo(candidate, 0) }.isSuccess) return candidate
            candidate = candidate.substringBeforeLast('.')
        }
        return null
    }

    private fun systemPackageFor(uid: Int?, sharingUid: List<String>): String? = when {
        uid == Process.SYSTEM_UID -> "android"
        sharingUid.isNotEmpty() -> sharingUid.first()
        else -> null
    }
}
