package dev.supersubmarine.memwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.supersubmarine.memwatch.data.AppMemory
import dev.supersubmarine.memwatch.data.AppMemoryRepository
import dev.supersubmarine.memwatch.data.AppMemorySnapshot
import dev.supersubmarine.memwatch.data.MemoryRepository
import dev.supersubmarine.memwatch.data.MemorySnapshot
import dev.supersubmarine.memwatch.data.SystemActions
import dev.supersubmarine.memwatch.shizuku.ShizukuManager
import dev.supersubmarine.memwatch.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val memory: MemorySnapshot? = null,
    val previousMemory: MemorySnapshot? = null,
    val shizuku: ShizukuState = ShizukuState.NOT_INSTALLED,
    val apps: AppMemorySnapshot? = null,
    val appsError: String? = null,
    val isLoadingApps: Boolean = false,
    val isRefreshing: Boolean = false,
    val hasSystemMemoryScreen: Boolean = false,
    val stoppingKey: String? = null,
    val isClearingBackground: Boolean = false,
    val selectedKey: String? = null,
    val message: String? = null,
) {
    val isReady: Boolean get() = shizuku == ShizukuState.READY
    val selectedApp: AppMemory? get() = apps?.apps?.firstOrNull { it.key == selectedKey }
    val isBusy: Boolean get() = stoppingKey != null || isClearingBackground
}

class MemWatchViewModel(application: Application) : AndroidViewModel(application) {
    private val memoryRepository = MemoryRepository(application)
    private val shizuku = ShizukuManager(application)
    private val appMemoryRepository = AppMemoryRepository(application, shizuku)
    private val systemActions = SystemActions(application)

    private val _state = MutableStateFlow(UiState(hasSystemMemoryScreen = systemActions.canOpenAppMemoryUsage()))
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            shizuku.state
                .onEach { shizukuState -> _state.update { it.copy(shizuku = shizukuState) } }
                .filter { it == ShizukuState.READY }
                .collect { refreshApps() }
        }
        viewModelScope.launch { refreshMemory() }
    }

    fun refresh(minVisibleMillis: Long = 0L) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val started = System.currentTimeMillis()
            shizuku.refresh()
            refreshMemory()
            if (_state.value.isReady) refreshApps()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < minVisibleMillis) delay(minVisibleMillis - elapsed)
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    suspend fun refreshMemory() {
        val snapshot = withContext(Dispatchers.IO) { memoryRepository.snapshot() }
        _state.update { it.copy(memory = snapshot, previousMemory = it.memory ?: snapshot) }
    }

    /** One `dumpsys meminfo` round-trip; skipped while another is in flight. */
    suspend fun refreshApps() {
        if (!_state.value.isReady || _state.value.isLoadingApps) return
        _state.update { it.copy(isLoadingApps = true) }
        val result = runCatching { appMemoryRepository.snapshot() }
        _state.update {
            it.copy(
                apps = result.getOrNull() ?: it.apps,
                appsError = result.exceptionOrNull()?.let(::describe),
                isLoadingApps = false,
            )
        }
    }

    fun select(key: String?) = _state.update { it.copy(selectedKey = key) }

    fun requestShizuku() {
        when (_state.value.shizuku) {
            ShizukuState.NOT_INSTALLED -> systemActions.openUrl(ShizukuManager.DOWNLOAD_URL)
            ShizukuState.NOT_RUNNING, ShizukuState.PERMISSION_DENIED -> openShizukuApp()
            ShizukuState.PERMISSION_NEEDED -> shizuku.requestPermission()
            ShizukuState.READY -> Unit
        }
    }

    fun openShizukuApp() {
        if (!systemActions.openShizukuApp()) systemActions.openUrl(ShizukuManager.SETUP_URL)
    }

    fun openShizukuGuide() {
        systemActions.openUrl(ShizukuManager.SETUP_URL)
    }

    fun forceStop(app: AppMemory) {
        val packageName = app.packageName ?: return
        if (_state.value.isBusy) return
        viewModelScope.launch {
            _state.update { it.copy(stoppingKey = app.key) }
            val result = runCatching { appMemoryRepository.forceStop(packageName) }
            delay(SETTLE_MILLIS)
            refreshMemory()
            refreshApps()
            _state.update {
                it.copy(
                    stoppingKey = null,
                    selectedKey = null,
                    message = result.fold(
                        onSuccess = { "Stopped ${app.label} — it was using ${formatBytes(app.pssBytes)}" },
                        onFailure = { error -> "Couldn't stop ${app.label}: ${describe(error)}" },
                    ),
                )
            }
        }
    }

    fun clearBackground() {
        if (_state.value.isBusy) return
        val before = _state.value.apps ?: return
        viewModelScope.launch {
            _state.update { it.copy(isClearingBackground = true) }
            val result = runCatching { appMemoryRepository.killBackground() }
            delay(SETTLE_MILLIS)
            refreshMemory()
            refreshApps()
            val after = _state.value.apps ?: before
            val freed = (before.totalPssBytes - after.totalPssBytes).coerceAtLeast(0)
            val gone = before.apps.count { app -> after.apps.none { it.key == app.key } }
            _state.update {
                it.copy(
                    isClearingBackground = false,
                    message = result.fold(
                        onSuccess = { if (gone > 0) "Cleared $gone cached ${if (gone == 1) "app" else "apps"} · ${formatBytes(freed)} freed" else "Nothing was cached — Android had already cleaned up" },
                        onFailure = { error -> "Couldn't clear background apps: ${describe(error)}" },
                    ),
                )
            }
        }
    }

    fun openAppDetails(packageName: String) {
        if (!systemActions.openAppDetails(packageName)) {
            _state.update { it.copy(message = "Couldn't open system settings for this app.") }
        }
    }

    fun openAppMemoryUsage() {
        if (!systemActions.openAppMemoryUsage()) {
            _state.update { it.copy(message = "Developer options are hidden. Tap Build number 7 times in Settings > About phone to enable them.") }
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    override fun onCleared() {
        shizuku.shutdown()
    }

    private fun describe(error: Throwable): String = error.message ?: error.javaClass.simpleName

    private companion object {
        const val SETTLE_MILLIS = 700L
    }
}
