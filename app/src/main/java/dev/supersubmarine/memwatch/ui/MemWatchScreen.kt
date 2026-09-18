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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
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
import dev.supersubmarine.memwatch.ui.components.AppDetailSheet
import dev.supersubmarine.memwatch.ui.components.AppRow
import dev.supersubmarine.memwatch.ui.components.EmptyRows
import dev.supersubmarine.memwatch.ui.components.ExplainerCard
import dev.supersubmarine.memwatch.ui.components.InfoCard
import dev.supersubmarine.memwatch.ui.components.MemoryHero
import dev.supersubmarine.memwatch.ui.components.RowDivider
import dev.supersubmarine.memwatch.ui.components.SectionHeader
import dev.supersubmarine.memwatch.ui.components.StatTiles
import dev.supersubmarine.memwatch.ui.components.TrimResultBanner
import dev.supersubmarine.memwatch.ui.components.UsageAccessCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemWatchScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onSelect: (String?) -> Unit,
    onGrantUsageAccess: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "hero") { MemoryHero(memory = state.memory, previous = state.previousMemory) }
                item(key = "tiles") { StatTiles(memory = state.memory) }
                item(key = "explainer") { ExplainerCard() }
                item(key = "trim") {
                    AnimatedVisibility(
                        visible = state.lastTrim != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        state.lastTrim?.let { TrimResultBanner(result = it, onDismiss = onDismissTrim) }
                    }
                }

                item(key = "apps-header") {
                    SectionHeader(
                        title = "Recently active apps",
                        trailing = if (state.usageAccessGranted && state.appsLoaded) "${state.apps.size} in the last 24 h" else null,
                    )
                }

                when {
                    !state.appsLoaded -> item(key = "apps-loading") { EmptyRows() }
                    !state.usageAccessGranted -> item(key = "apps-permission") { UsageAccessCard(onGrant = onGrantUsageAccess) }
                    state.apps.isEmpty() -> item(key = "apps-empty") {
                        InfoCard(
                            title = "No app activity yet",
                            body = "Android starts recording activity once usage access is on. Use your phone normally and pull down to refresh.",
                            actionLabel = null,
                            onAction = null,
                        )
                    }
                    else -> itemsIndexed(state.apps, key = { _, app -> app.packageName }) { index, app ->
                        Column {
                            AppRow(app = app, now = now, onClick = { onSelect(app.packageName) })
                            if (index < state.apps.lastIndex) RowDivider()
                        }
                    }
                }

                item(key = "footer") {
                    Spacer(Modifier.height(8.dp))
                    InfoCard(
                        title = "Per-app memory is hidden from apps",
                        body = if (state.canTrimOtherApps) {
                            "Since Android 8, only the system can see how much RAM each app holds. Developer options shows it under Running services."
                        } else {
                            "Since Android 8, only the system can see how much RAM each app holds, and Android 14 stopped apps from trimming each other. Developer options shows the real per-app numbers under Running services."
                        },
                        actionLabel = "Open Developer options",
                        onAction = onOpenDeveloperOptions,
                    )
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
