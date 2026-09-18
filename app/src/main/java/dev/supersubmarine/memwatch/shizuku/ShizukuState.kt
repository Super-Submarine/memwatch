package dev.supersubmarine.memwatch.shizuku

/** Where the user is on the road to shell-level access. Each step has its own UI. */
enum class ShizukuState {
    /** Shizuku (or Sui) is not on the device at all. */
    NOT_INSTALLED,

    /** Installed, but its service hasn't been started (needs Wireless debugging or root once per boot). */
    NOT_RUNNING,

    /** Service is up; MemWatch hasn't been allowed to use it yet. */
    PERMISSION_NEEDED,

    /** The user said no with "don't ask again"; only Shizuku's own app can undo it. */
    PERMISSION_DENIED,

    /** Bound and ready to run commands. */
    READY,
}
