package dev.supersubmarine.memwatch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppUsage
import dev.supersubmarine.memwatch.ui.formatDuration
import dev.supersubmarine.memwatch.ui.formatRelativeTime
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
    app: AppUsage,
    canTrim: Boolean,
    isTrimming: Boolean,
    onDismiss: () -> Unit,
    onForceStop: () -> Unit,
    onTrim: () -> Unit,
) {
    val semantic = MemWatchTheme.semantic
    val now = System.currentTimeMillis()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.icon, app.label, size = 48.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = app.packageName + (app.versionName?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DetailRow("Last opened", formatRelativeTime(app.lastTimeUsedMillis, now))
                    DetailRow("On screen, last 24 h", if (app.totalForegroundMillis > 0) formatDuration(app.totalForegroundMillis) else "Not used")
                    DetailRow(
                        label = "Background service",
                        value = if (app.lastBackgroundServiceMillis > 0) formatRelativeTime(app.lastBackgroundServiceMillis, now) else "None recorded",
                        valueColor = if (app.ranInBackgroundRecently) semantic.warn else null,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = if (app.isSystemApp) {
                    "This is part of the system. Force stopping it can break phone features until it restarts on its own."
                } else {
                    "Force stop ends every process and background service of this app until you open it again. Android only allows that from its own Settings screen — tap Force stop there."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (app.isSystemApp) semantic.warn else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            Button(onClick = onForceStop, modifier = Modifier.fillMaxWidth()) {
                Text("Open app info to force stop")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            if (canTrim && !app.isSystemApp) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onTrim, enabled = !isTrimming, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isTrimming) "Trimming…" else "Trim background process")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface,
        )
    }
}
