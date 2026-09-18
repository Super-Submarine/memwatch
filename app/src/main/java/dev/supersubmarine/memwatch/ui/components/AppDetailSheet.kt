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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppMemory
import dev.supersubmarine.memwatch.ui.formatBytes
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme

/**
 * Everything about one app's RAM plus the two things you can do about it: force stop it right
 * here (through Shizuku) or open Android's App info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailSheet(
    app: AppMemory,
    isStopping: Boolean,
    onDismiss: () -> Unit,
    onForceStop: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val semantic = MemWatchTheme.semantic
    var confirmStop by remember { mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.icon, size = 48.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    app.packageName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    app.versionName?.let {
                        Text(
                            text = "Version $it",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("RAM right now", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = formatBytes(app.pssBytes),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = if (app.isReclaimable) {
                            "${app.tier.label}. Android keeps it warm so it reopens fast and will drop it the moment something else needs the memory."
                        } else {
                            "${app.tier.label}. Android counts this against your free memory."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    app.processes.sortedByDescending { it.pssBytes }.forEach { process ->
                        DetailRow(
                            label = process.name.removePrefix(app.packageName.orEmpty()).ifEmpty { "main" }.removePrefix(":"),
                            value = formatBytes(process.pssBytes),
                            mono = true,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = when {
                    app.packageName == null -> "These processes belong to Android itself, not to an app, so there is nothing to stop."
                    app.isSystemApp -> "This is part of the system. Force stopping it frees its RAM now, but Android usually restarts it and some features may hiccup until it does."
                    else -> "Force stop ends every process of this app until you open it again — the same as the button in Android's App info."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (app.isSystemApp && app.packageName != null) semantic.warn else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            if (app.packageName != null) {
                Button(
                    onClick = { if (app.isSystemApp) confirmStop = true else onForceStop() },
                    enabled = !isStopping,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = if (app.isSystemApp) ButtonDefaults.buttonColors(containerColor = semantic.warn) else ButtonDefaults.buttonColors(),
                ) {
                    if (isStopping) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(12.dp))
                        Text("Stopping…")
                    } else {
                        Text("Force stop")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Open App info")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop a system app?") },
            text = { Text("${app.label} is part of Android. Stopping it frees ${formatBytes(app.pssBytes)} for a moment, but the system will most likely bring it straight back.") },
            confirmButton = {
                TextButton(onClick = { confirmStop = false; onForceStop() }) { Text("Force stop") }
            },
            dismissButton = { TextButton(onClick = { confirmStop = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = if (mono) MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
