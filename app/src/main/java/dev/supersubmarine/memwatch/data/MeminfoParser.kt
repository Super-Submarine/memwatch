package dev.supersubmarine.memwatch.data

/**
 * Parses the compact form of `dumpsys meminfo -c`. Process rows look like
 * `proc,<oom>,<process name>,<pid>,<pss kB>,<swap kB|N/A>,<a|e>` (AOSP ActivityManagerService).
 *
 * Since Android 10 the compact dump emits two passes of identical shape — RSS first, then PSS —
 * each made of `oom,` headers with their `proc,` rows and closed by `cat,` category totals.
 * Only the last pass is PSS.
 */
object MeminfoParser {
    fun parse(output: String): List<ProcessMemory> =
        lastPass(output).mapNotNull(::parseRow)

    private fun lastPass(output: String): List<String> {
        val passes = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        for (line in output.lineSequence()) {
            when {
                line.startsWith("proc,") -> current += line
                line.startsWith("cat,") && current.isNotEmpty() -> {
                    passes += current
                    current = mutableListOf()
                }
            }
        }
        if (current.isNotEmpty()) passes += current
        return passes.lastOrNull().orEmpty()
    }

    private fun parseRow(line: String): ProcessMemory? {
        val cols = line.split(',')
        if (cols.size < 7) return null
        val pid = cols[cols.size - 4].toIntOrNull() ?: return null
        val pssKb = cols[cols.size - 3].toLongOrNull() ?: return null
        // Process names can't contain commas, but be defensive about extra columns.
        val name = cols.subList(2, cols.size - 4).joinToString(",")
        return ProcessMemory(
            name = name,
            pid = pid,
            pssBytes = pssKb * 1024L,
            tier = OomTier.fromCompactLabel(cols[1]),
            hasActivities = cols.last() == "a",
        )
    }

    /** `ps -A -o PID,UID` rows → pid to uid. */
    fun parsePidUids(output: String): Map<Int, Int> =
        output.lineSequence()
            .map { it.trim().split(Regex("\\s+")) }
            .filter { it.size >= 2 }
            .mapNotNull { cols -> cols[0].toIntOrNull()?.let { pid -> cols[1].toIntOrNull()?.let { uid -> pid to uid } } }
            .toMap()
}
