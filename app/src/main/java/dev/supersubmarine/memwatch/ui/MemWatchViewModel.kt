package dev.supersubmarine.memwatch.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.supersubmarine.memwatch.data.AppSort
import dev.supersubmarine.memwatch.data.AppUsage
import dev.supersubmarine.memwatch.data.AppUsageRepository
import dev.supersubmarine.memwatch.data.MemoryRepository
import dev.supersubmarine.memwatch.data.MemorySnapshot
import dev.supersubmarine.memwatch.data.SystemActions
import dev.supersubmarine.memwatch.data.TrimResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val memory: MemorySnapshot? = null,
    val previousMemory: MemorySnapshot? = null,
    val apps: List<AppUsage> = emptyList(),
    val sort: AppSort = AppSort.STORAGE,
    val usageAccessGranted: Boolean = false,
    val appsLoaded: Boolean = false,
    val isRefreshing: Boolean = false,
    val isTrimming: Boolean = false,
    val canTrimOtherApps: Boolean = false,
    val hasSystemMemoryScreen: Boolean = false,
    val lastTrim: TrimResult? = null,
    val selectedPackage: String? = null,
    val message: String? = null,
) {
    val selectedApp: AppUsage? get() = apps.firstOrNull { it.packageName == selectedPackage }
    val trimmableApps: List<AppUsage> get() = apps.filterNot { it.isSystemApp }

    val sortedApps: List<AppUsage>
        get() = when (sort) {
            AppSort.STORAGE -> apps.sortedByDescending { it.storageBytes }
            AppSort.RECENT -> apps.filter { it.lastActivityMillis > 0 }.sortedByDescending { it.lastActivityMillis }
        }

    val largestStorageBytes: Long get() = apps.maxOfOrNull { it.storageBytes } ?: 0L
    val totalAppStorageBytes: Long get() = apps.sumOf { it.storageBytes }
}

class MemWatchViewModel(application: Application) : AndroidViewModel(application) {
    private val memoryRepository = MemoryRepository(application)
    private val appUsageRepository = AppUsageRepository(application)
    private val systemActions = SystemActions(application, memoryRepository)

    private val _state = MutableStateFlow(
        UiState(
            canTrimOtherApps = systemActions.canTrimOtherApps,
            hasSystemMemoryScreen = systemActions.canOpenAppMemoryUsage(),
        ),
    )
    val state: StateFlow<UiState> = _state

    init {
        refresh()
    }

    fun refresh(minVisibleMillis: Long = 0L) {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val started = System.currentTimeMillis()
            val granted = withContext(Dispatchers.IO) { appUsageRepository.hasUsageAccess() }
            val apps = if (granted) withContext(Dispatchers.IO) { appUsageRepository.apps() } else emptyList()
            refreshMemory()
            val elapsed = System.currentTimeMillis() - started
            if (elapsed < minVisibleMillis) delay(minVisibleMillis - elapsed)
            _state.update {
                it.copy(
                    apps = apps,
                    usageAccessGranted = granted,
                    appsLoaded = true,
                    isRefreshing = false,
                )
            }
        }
    }

    suspend fun refreshMemory() {
        val snapshot = withContext(Dispatchers.IO) { memoryRepository.snapshot() }
        _state.update { it.copy(memory = snapshot, previousMemory = it.memory ?: snapshot) }
    }

    fun select(packageName: String?) = _state.update { it.copy(selectedPackage = packageName) }

    fun setSort(sort: AppSort) = _state.update { it.copy(sort = sort) }

    fun openAppDetails(packageName: String) {
        if (!systemActions.openAppDetails(packageName)) {
            _state.update { it.copy(message = "Couldn't open system settings for this app.") }
        }
    }

    fun openUsageAccessSettings() {
        if (!systemActions.openUsageAccessSettings()) {
            _state.update { it.copy(message = "Usage access settings aren't available on this device.") }
        }
    }

    fun openAppMemoryUsage() {
        if (!systemActions.openAppMemoryUsage()) {
            _state.update { it.copy(message = "Developer options are hidden. Tap Build number 7 times in Settings > About phone to enable them.") }
        }
    }

    fun trimBackground(packages: List<String>) {
        if (!systemActions.canTrimOtherApps || packages.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(isTrimming = true) }
            val result = withContext(Dispatchers.IO) { systemActions.trimBackground(packages) }
            refreshMemory()
            _state.update { it.copy(isTrimming = false, lastTrim = result) }
        }
    }

    fun dismissTrimResult() = _state.update { it.copy(lastTrim = null) }

    fun consumeMessage() = _state.update { it.copy(message = null) }
}
