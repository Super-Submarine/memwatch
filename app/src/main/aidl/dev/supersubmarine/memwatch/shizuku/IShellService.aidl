package dev.supersubmarine.memwatch.shizuku;

interface IShellService {
    void destroy() = 16777114;

    /** Runs [command] with `sh -c` as the Shizuku uid and returns combined stdout/stderr. */
    String run(String command, int timeoutMillis) = 1;
}
