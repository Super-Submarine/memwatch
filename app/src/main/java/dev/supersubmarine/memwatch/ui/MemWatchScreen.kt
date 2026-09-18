package dev.supersubmarine.memwatch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppMemory
import dev.supersubmarine.memwatch.data.AppMemorySnapshot
import dev.supersubmarine.memwatch.shizuku.ShizukuState
import dev.supersubmarine.memwatch.ui.components.AppDetailSheet
import dev.supersubmarine.memwatch.ui.components.AppRow
import dev.supersubmarine.memwatch.ui.components.EmptyRows
import dev.supersubmarine.memwatch.ui.components.InfoCard
import dev.supersubmarine.memwatch.ui.components.MemorySummaryCard
import dev.supersubmarine.memwatch.ui.components.RowDivider
import dev.supersubmarine.memwatch.ui.components.SectionHeader
import dev.supersubmarine.memwatch.ui.components.ShizukuSetupCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemWatchScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onSelect: (String?) -> Unit,
    onSetupShizuku: () -> Unit,
    onOpenShizukuGuide: () -> Unit,
    onOpenAppMemoryUsage: () -> Unit,
    onForceStop: (AppMemory) -> Unit,
    onOpenAppInfo: (String) -> Unit,
    onClearBackground: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        onMessageShown()
    }

    val reclaimable = state.apps?.apps?.filter { it.isReclaimable && it.packageName != null }.orEmpty()
    val showClearBar = state.isReady && reclaimable.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Memory", style = MaterialTheme.typography.titleLarge) },
                actions = { RefreshAction(isRefreshing = state.isRefreshing, onClick = onRefresh) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            AnimatedVisibility(visible = showClearBar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Button(
                            onClick = onClearBackground,
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            if (state.isClearingBackground) {
                                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(12.dp))
                                Text("Clearing…")
                            } else {
                                val count = reclaimable.size
                                Text("Clear $count cached ${if (count == 1) "app" else "apps"} · ${formatBytes(reclaimable.sumOf { it.pssBytes })}")
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                item(key = "summary") { MemorySummaryCard(memory = state.memory, previous = state.previousMemory) }

                item(key = "apps-header") {
                    SectionHeader(
                        title = "RAM by app",
                        trailing = state.apps?.takeIf { state.isReady }?.let { "${it.apps.size} running · ${formatBytes(it.totalPssBytes)}" },
                        modifier = Modifier.padding(top = GAP),
                    )
                }

                appsSection(state, onSelect, onSetupShizuku, onOpenShizukuGuide, onRefresh)

                item(key = "android-ranking") {
                    AndroidRankingLink(hasSystemMemoryScreen = state.hasSystemMemoryScreen, onClick = onOpenAppMemoryUsage, modifier = Modifier.padding(top = GAP))
                }
            }
        }
    }

    state.selectedApp?.let { app ->
        AppDetailSheet(
            app = app,
            isStopping = state.stoppingKey == app.key,
            onDismiss = { onSelect(null) },
            onForceStop = { onForceStop(app) },
            onOpenAppInfo = { app.packageName?.let(onOpenAppInfo) },
        )
    }
}

private fun LazyListScope.appsSection(
    state: UiState,
    onSelect: (String?) -> Unit,
    onSetupShizuku: () -> Unit,
    onOpenShizukuGuide: () -> Unit,
    onRetry: () -> Unit,
) {
    val snapshot: AppMemorySnapshot? = state.apps
    when {
        state.shizuku != ShizukuState.READY -> item(key = "shizuku-setup") {
            ShizukuSetupCard(state = state.shizuku, onAction = onSetupShizuku, onOpenGuide = onOpenShizukuGuide, modifier = Modifier.padding(top = GAP))
        }
        snapshot == null && state.appsError != null -> item(key = "apps-error") {
            InfoCard(
                modifier = Modifier.padding(top = GAP),
                title = "Couldn't read app memory",
                body = "Shizuku answered, but the reading failed: ${state.appsError}. If Shizuku was just restarted, try again.",
                actionLabel = "Try again",
                onAction = onRetry,
            )
        }
        snapshot == null -> item(key = "apps-loading") { EmptyRows(Modifier.padding(top = GAP)) }
        else -> {
            itemsIndexed(snapshot.apps, key = { _, app -> app.key }) { index, app ->
                Column(if (index == 0) Modifier.padding(top = GAP) else Modifier) {
                    AppRow(
                        app = app,
                        largestBytes = snapshot.largestPssBytes,
                        shape = groupShape(index, snapshot.apps.lastIndex),
                        onClick = { onSelect(app.key) },
                    )
                    if (index < snapshot.apps.lastIndex) RowDivider()
                }
            }
            item(key = "apps-footer") {
                Text(
                    text = "Measured by Android (PSS) ${formatRelativeTime(snapshot.capturedAtMillis, System.currentTimeMillis())} · refreshes every 12 s" +
                        (state.appsError?.let { " · last refresh failed: $it" } ?: ""),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp),
                )
            }
        }
    }
}

/** Android's own averaged per-app RAM screen — a second opinion, not the main event. */
@Composable
private fun AndroidRankingLink(hasSystemMemoryScreen: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (hasSystemMemoryScreen) "Android also keeps a 3 h – 1 day average per app." else "Android keeps its own averages under Developer options.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onClick) {
            Text(if (hasSystemMemoryScreen) "Open" else "Developer options")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

private val GAP = 12.dp

/** Rows share one visual container: only the first and last rows get outer corners. */
@Composable
private fun groupShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val radius = 20.dp
    val zero = 0.dp
    return when {
        lastIndex == 0 -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius, bottomStart = zero, bottomEnd = zero)
        index == lastIndex -> RoundedCornerShape(topStart = zero, topEnd = zero, bottomStart = radius, bottomEnd = radius)
        else -> RoundedCornerShape(zero)
    }
}

@Composable
private fun RefreshAction(isRefreshing: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "refresh")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 900, easing = LinearEasing), RepeatMode.Restart),
        label = "refreshAngle",
    )
    IconButton(onClick = onClick, enabled = !isRefreshing) {
        Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = "Refresh",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.rotate(if (isRefreshing) angle else 0f),
        )
    }
}
