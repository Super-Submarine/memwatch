package dev.supersubmarine.memwatch.shizuku

import android.content.Context
import androidx.annotation.Keep
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Runs inside the Shizuku server process (uid `shell` via ADB, or root), so `dumpsys`,
 * `am force-stop` and friends work exactly as they would from an `adb shell`.
 */
@Keep
class ShellUserService() : IShellService.Stub() {
    @Keep
    @Suppress("unused")
    constructor(context: Context) : this()

    override fun destroy() {
        exitProcess(0)
    }

    override fun run(command: String, timeoutMillis: Int): String {
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        var output = ""
        val collector = Thread { output = process.inputStream.bufferedReader().use { it.readText() } }
        collector.start()
        if (!process.waitFor(timeoutMillis.toLong(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly()
            collector.join(1_000)
            throw IllegalStateException("Command timed out after ${timeoutMillis}ms")
        }
        collector.join()
        return output
    }
}
