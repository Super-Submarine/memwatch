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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppSort
import dev.supersubmarine.memwatch.ui.components.AppDetailSheet
import dev.supersubmarine.memwatch.ui.components.AppRow
import dev.supersubmarine.memwatch.ui.components.EmptyRows
import dev.supersubmarine.memwatch.ui.components.InfoCard
import dev.supersubmarine.memwatch.ui.components.MemorySummaryCard
import dev.supersubmarine.memwatch.ui.components.RowDivider
import dev.supersubmarine.memwatch.ui.components.SectionHeader
import dev.supersubmarine.memwatch.ui.components.SortChips
import dev.supersubmarine.memwatch.ui.components.SuggestionCard
import dev.supersubmarine.memwatch.ui.components.TrimResultBanner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemWatchScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onSelect: (String?) -> Unit,
    onSort: (AppSort) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onOpenAppMemoryUsage: () -> Unit,
    onForceStop: (String) -> Unit,
    onTrim: (List<String>) -> Unit,
    onDismissTrim: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbar.showSnackbar(message)
        onMessageShown()
    }

    val showTrimBar = state.canTrimOtherApps && state.usageAccessGranted && state.trimmableApps.isNotEmpty()

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
            AnimatedVisibility(visible = showTrimBar, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Button(
                            onClick = { onTrim(state.trimmableApps.map { it.packageName }) },
                            enabled = !state.isTrimming,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) {
                            val count = state.trimmableApps.size
                            Text(if (state.isTrimming) "Trimming…" else "Trim $count background ${if (count == 1) "app" else "apps"}")
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
            val now = System.currentTimeMillis()
            val apps = state.sortedApps
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            ) {
                item(key = "summary") { MemorySummaryCard(memory = state.memory, previous = state.previousMemory) }
                item(key = "ram-per-app") {
                    SuggestionCard(
                        modifier = Modifier.padding(top = GAP),
                        icon = Icons.Rounded.Memory,
                        eyebrow = "RAM per app",
                        title = "Memory used by apps",
                        body = if (state.hasSystemMemoryScreen) {
                            "Android measures each app's RAM itself and hides it from other apps. Its own ranking — averaged over the last 3 hours to 1 day — is one tap away."
                        } else {
                            "Android measures each app's RAM itself and hides it from other apps. The real numbers live in Developer options under Running services."
                        },
                        actionLabel = if (state.hasSystemMemoryScreen) "Open Android's RAM ranking" else "Open Developer options",
                        onAction = onOpenAppMemoryUsage,
                    )
                }
                item(key = "trim") {
                    AnimatedVisibility(
                        visible = state.lastTrim != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        state.lastTrim?.let { TrimResultBanner(result = it, onDismiss = onDismissTrim, modifier = Modifier.padding(top = GAP)) }
                    }
                }

                item(key = "apps-header") {
                    SectionHeader(
                        title = "Apps",
                        trailing = if (state.usageAccessGranted && state.appsLoaded) "${state.apps.size} apps · ${formatBytes(state.totalAppStorageBytes)} on device" else null,
                        modifier = Modifier.padding(top = GAP),
                    )
                }
                if (state.usageAccessGranted && state.appsLoaded && state.apps.isNotEmpty()) {
                    item(key = "apps-sort") { SortChips(selected = state.sort, onSelect = onSort, modifier = Modifier.padding(top = 4.dp, bottom = GAP)) }
                }

                when {
                    !state.appsLoaded -> item(key = "apps-loading") { EmptyRows(Modifier.padding(top = GAP)) }
                    !state.usageAccessGranted -> item(key = "apps-permission") {
                        SuggestionCard(
                            modifier = Modifier.padding(top = GAP),
                            icon = Icons.Rounded.Apps,
                            eyebrow = "One-time permission",
                            title = "See how much each app takes",
                            body = "Usage access lets MemWatch read each app's measured storage (app, data, cache) and when it last ran. It's read on your phone only — nothing leaves the device.",
                            actionLabel = "Allow usage access",
                            onAction = onGrantUsageAccess,
                            emphasized = true,
                        )
                    }
                    state.apps.isEmpty() -> item(key = "apps-empty") {
                        InfoCard(
                            modifier = Modifier.padding(top = GAP),
                            title = "Nothing to show yet",
                            body = "No user apps were found. Install or open a few apps, then pull down to refresh.",
                            actionLabel = null,
                            onAction = null,
                        )
                    }
                    else -> itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        Column {
                            AppRow(
                                app = app,
                                now = now,
                                largestBytes = state.largestStorageBytes,
                                shape = groupShape(index, apps.lastIndex),
                                onClick = { onSelect(app.packageName) },
                            )
                            if (index < apps.lastIndex) RowDivider()
                        }
                    }
                }
            }
        }
    }

    state.selectedApp?.let { app ->
        AppDetailSheet(
            app = app,
            canTrim = state.canTrimOtherApps,
            isTrimming = state.isTrimming,
            onDismiss = { onSelect(null) },
            onForceStop = { onForceStop(app.packageName) },
            onTrim = { onTrim(listOf(app.packageName)) },
        )
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
