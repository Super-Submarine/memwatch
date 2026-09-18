package dev.supersubmarine.memwatch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.MemoryPressure
import dev.supersubmarine.memwatch.data.MemorySnapshot
import dev.supersubmarine.memwatch.ui.formatBytes

/** Progressive disclosure under the summary bar: what the numbers mean, plus the raw figures. */
@Composable
fun MemoryDetails(memory: MemorySnapshot?, pressure: MemoryPressure, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(modifier) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = pressureCopy(pressure),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Hide details" else "Show details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevron),
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Body("Why is 1–2 GB in use with nothing open? The kernel, system services and your launcher live there permanently.")
                Body("Above that sits recently used apps kept warm so they reopen instantly. Android drops them the moment something needs the space — idle free RAM is wasted RAM.")
                Body("Cache is file data the kernel can reclaim instantly. Swap is compressed memory (zRAM); if it keeps growing, memory is genuinely tight.")
                Spacer(Modifier.height(4.dp))
                if (memory != null) {
                    DetailLine("Available to apps", formatBytes(memory.availableBytes))
                    DetailLine("Low-memory threshold", formatBytes(memory.lowMemoryThresholdBytes))
                    DetailLine("Swap in use", if (memory.swapTotalBytes == 0L) "None" else "${formatBytes(memory.swapUsedBytes)} of ${formatBytes(memory.swapTotalBytes)}")
                    DetailLine("MemWatch itself (PSS)", formatBytes(memory.selfPssBytes))
                }
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun pressureCopy(pressure: MemoryPressure): String = when (pressure) {
    MemoryPressure.NORMAL -> "Normal for Android — cached apps are dropped the moment something needs the space."
    MemoryPressure.HIGH -> "Less than a fifth is free. Apps may reload from scratch when you switch back."
    MemoryPressure.CRITICAL -> "Below the low-memory threshold — Android is closing background apps."
}
