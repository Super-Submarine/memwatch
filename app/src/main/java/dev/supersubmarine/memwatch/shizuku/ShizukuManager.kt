package dev.supersubmarine.memwatch.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import dev.supersubmarine.memwatch.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku

/**
 * Tracks Shizuku availability and owns the single [IShellService] bound inside the Shizuku
 * process. Everything here is main-thread safe; [run] suspends off the main thread.
 */
class ShizukuManager(private val context: Context) {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<ShizukuState> = _state

    private var service: IShellService? = null
    private var binding: CompletableDeferred<IShellService>? = null

    private val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context, ShellUserService::class.java))
        .daemon(false)
        .processNameSuffix("shell")
        .debuggable(BuildConfig.DEBUG)
        .version(BuildConfig.VERSION_CODE)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val bound = IShellService.Stub.asInterface(binder)
            service = bound
            binding?.complete(bound)
            binding = null
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            binding?.cancel()
            binding = null
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky { refresh() }
        Shizuku.addBinderDeadListener {
            service = null
            binding?.cancel()
            binding = null
            refresh()
        }
        Shizuku.addRequestPermissionResultListener { code, result ->
            if (code == PERMISSION_REQUEST_CODE) {
                _state.value = if (result == PackageManager.PERMISSION_GRANTED) ShizukuState.READY else ShizukuState.PERMISSION_DENIED
            }
        }
    }

    fun refresh() {
        _state.value = initialState()
    }

    /** Shows Shizuku's own consent dialog; the result lands in [state]. */
    fun requestPermission() {
        if (!Shizuku.pingBinder() || Shizuku.isPreV11()) return refresh()
        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            _state.value = ShizukuState.READY
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            _state.value = ShizukuState.PERMISSION_DENIED
        } else {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
        }
    }

    val isRootMode: Boolean get() = Shizuku.pingBinder() && Shizuku.getUid() == 0

    /** Runs a shell command in the Shizuku process. Throws if Shizuku isn't ready or the command fails. */
    suspend fun run(command: String, timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS): String {
        val bound = service ?: bind()
        return withContext(Dispatchers.IO) { bound.run(command, timeoutMillis) }
    }

    private suspend fun bind(): IShellService {
        check(_state.value == ShizukuState.READY) { "Shizuku is not ready" }
        val pending = binding ?: CompletableDeferred<IShellService>().also {
            binding = it
            Shizuku.bindUserService(serviceArgs, connection)
        }
        return withTimeout(BIND_TIMEOUT_MILLIS) { pending.await() }
    }

    fun shutdown() {
        runCatching { Shizuku.unbindUserService(serviceArgs, connection, true) }
        service = null
    }

    private fun initialState(): ShizukuState = when {
        Shizuku.pingBinder() -> when {
            Shizuku.isPreV11() -> ShizukuState.NOT_RUNNING
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuState.READY
            Shizuku.shouldShowRequestPermissionRationale() -> ShizukuState.PERMISSION_DENIED
            else -> ShizukuState.PERMISSION_NEEDED
        }
        isInstalled() -> ShizukuState.NOT_RUNNING
        else -> ShizukuState.NOT_INSTALLED
    }

    private fun isInstalled(): Boolean =
        runCatching { context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0) }.isSuccess

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        const val SETUP_URL = "https://shizuku.rikka.app/guide/setup/"
        const val DOWNLOAD_URL = "https://shizuku.rikka.app/download/"
        private const val PERMISSION_REQUEST_CODE = 0x5a
        private const val DEFAULT_TIMEOUT_MILLIS = 20_000
        private const val BIND_TIMEOUT_MILLIS = 15_000L
    }
}
