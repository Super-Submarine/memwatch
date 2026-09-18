package dev.supersubmarine.memwatch.data

/** How Android ranks a process for reclaim; mirrors the `oom` buckets in `dumpsys meminfo`. */
enum class OomTier(val label: String) {
    NATIVE("Native"),
    SYSTEM("System"),
    PERSISTENT("Always running"),
    FOREGROUND("In use"),
    VISIBLE("Visible"),
    PERCEPTIBLE("Playing or tracking"),
    SERVICE("Background service"),
    HOME("Home screen"),
    PREVIOUS("Last app"),
    CACHED("Cached — reclaimable"),
    ;

    /** Cached and previous apps are held only as a convenience; Android drops them on demand. */
    val isReclaimable: Boolean get() = this == CACHED || this == PREVIOUS

    companion object {
        fun fromCompactLabel(label: String): OomTier = when (label) {
            "native" -> NATIVE
            "sys" -> SYSTEM
            "pers", "persvc" -> PERSISTENT
            "fore" -> FOREGROUND
            "vis" -> VISIBLE
            "percept", "perceptm", "perceptl", "backup", "heavy" -> PERCEPTIBLE
            "servicea", "serviceb" -> SERVICE
            "home" -> HOME
            "prev" -> PREVIOUS
            "cached" -> CACHED
            else -> SERVICE
        }
    }
}

/** One live process as reported by `dumpsys meminfo -c`. */
data class ProcessMemory(
    val name: String,
    val pid: Int,
    val pssBytes: Long,
    val tier: OomTier,
    val hasActivities: Boolean,
)
