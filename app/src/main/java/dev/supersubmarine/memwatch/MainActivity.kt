package dev.supersubmarine.memwatch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.supersubmarine.memwatch.ui.MemWatchScreen
import dev.supersubmarine.memwatch.ui.MemWatchViewModel
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MemWatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refresh()
                while (true) {
                    delay(MEMORY_POLL_MILLIS)
                    viewModel.refreshMemory()
                }
            }
        }

        setContent {
            MemWatchTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                MemWatchScreen(
                    state = state,
                    onRefresh = { viewModel.refresh(minVisibleMillis = 600) },
                    onSelect = viewModel::select,
                    onSort = viewModel::setSort,
                    onGrantUsageAccess = viewModel::openUsageAccessSettings,
                    onOpenAppMemoryUsage = viewModel::openAppMemoryUsage,
                    onForceStop = viewModel::openAppDetails,
                    onTrim = viewModel::trimBackground,
                    onDismissTrim = viewModel::dismissTrimResult,
                    onMessageShown = viewModel::consumeMessage,
                )
            }
        }
    }

    private companion object {
        const val MEMORY_POLL_MILLIS = 4_000L
    }
}
