package dev.supersubmarine.memwatch.data

import android.content.Context
import dev.supersubmarine.memwatch.shizuku.ShizukuManager

/**
 * Real per-app RAM: asks Android's own accounting (`dumpsys meminfo`) through Shizuku and folds
 * the process rows into apps. Nothing here is estimated — if Shizuku isn't ready, there is no data.
 */
class AppMemoryRepository(context: Context, private val shizuku: ShizukuManager) {
    private val resolver = ProcessPackageResolver(context)
    private val catalog = InstalledAppCatalog(context)

    suspend fun snapshot(): AppMemorySnapshot {
        val output = shizuku.run("dumpsys meminfo -c; echo $SEPARATOR; ps -A -o PID,UID")
        val (meminfo, ps) = output.split(SEPARATOR, limit = 2).let { it[0] to it.getOrElse(1) { "" } }
        val processes = MeminfoParser.parse(meminfo)
        check(processes.isNotEmpty()) { "dumpsys meminfo returned no processes" }
        val pidUids = MeminfoParser.parsePidUids(ps)
        val apps = processes
            .groupBy { process -> resolver.resolve(process, pidUids[process.pid]) }
            .map { (owner, procs) -> catalog.describe(owner, procs) }
            .sortedByDescending { it.pssBytes }
        return AppMemorySnapshot(apps = apps, capturedAtMillis = System.currentTimeMillis())
    }

    /** `am force-stop`: the same thing the Force stop button in Settings does. */
    suspend fun forceStop(packageName: String) {
        shizuku.run("am force-stop ${shellQuote(packageName)}", timeoutMillis = 10_000)
    }

    /** `am kill-all`: drops every cached/background process the way Android itself does under pressure. */
    suspend fun killBackground() {
        shizuku.run("am kill-all", timeoutMillis = 10_000)
    }

    private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

    private companion object {
        const val SEPARATOR = "---MEMWATCH-PS---"
    }
}
