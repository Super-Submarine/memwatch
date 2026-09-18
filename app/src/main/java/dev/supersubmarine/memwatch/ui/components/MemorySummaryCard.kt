package dev.supersubmarine.memwatch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.MemoryPressure
import dev.supersubmarine.memwatch.data.MemorySnapshot
import dev.supersubmarine.memwatch.ui.formatBytes
import dev.supersubmarine.memwatch.ui.formatBytesSigned
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme
import kotlin.math.abs

private const val DELTA_NOISE_FLOOR_BYTES = 16L * 1024 * 1024

@Composable
fun MemorySummaryCard(memory: MemorySnapshot?, previous: MemorySnapshot?, modifier: Modifier = Modifier) {
    val semantic = MemWatchTheme.semantic
    val pressure = memory?.pressure ?: MemoryPressure.NORMAL
    val usedColor = when (pressure) {
        MemoryPressure.NORMAL -> semantic.ink
        MemoryPressure.HIGH -> semantic.warn
        MemoryPressure.CRITICAL -> semantic.critical
    }
    val used by animateFloatAsState(memory?.usedFraction ?: 0f, tween(600), label = "used")
    val cache by animateFloatAsState(memory?.let { it.reclaimableBytes.toFloat() / it.totalBytes } ?: 0f, tween(600), label = "cache")

    Surface(modifier.fillMaxWidth().animateContentSize(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Phone memory", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.weight(1f))
                PressureBadge(pressure)
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = memory?.let { formatBytes(it.usedBytes) } ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = memory?.let { "of ${formatBytes(it.totalBytes)} in use" } ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                val delta = if (memory != null && previous != null) memory.usedBytes - previous.usedBytes else 0L
                AnimatedVisibility(abs(delta) >= DELTA_NOISE_FLOOR_BYTES, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = formatBytesSigned(delta),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (delta < 0) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            SegmentedBar(
                segments = listOf(BarSegment(used, usedColor), BarSegment(cache, semantic.cache)),
                track = semantic.trackSubtle,
                modifier = Modifier.fillMaxWidth().height(14.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Legend(usedColor, "In use", memory?.let { formatBytes(it.usedBytes) })
                Legend(semantic.cache, "Cache", memory?.let { formatBytes(it.reclaimableBytes) })
                Legend(semantic.trackSubtle, "Free", memory?.let { formatBytes(it.freeBytes) }, outlined = true)
            }
            Spacer(Modifier.height(16.dp))
            MemoryDetails(memory = memory, pressure = pressure)
        }
    }
}

@Composable
private fun Legend(color: androidx.compose.ui.graphics.Color, label: String, value: String?, outlined: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val dot = if (outlined) Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape) else Modifier.background(color, CircleShape)
        Spacer(Modifier.size(8.dp).then(dot))
        Column {
            Text(value ?: "—", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PressureBadge(pressure: MemoryPressure) {
    val semantic = MemWatchTheme.semantic
    val (label, color) = when (pressure) {
        MemoryPressure.NORMAL -> "Healthy" to semantic.ok
        MemoryPressure.HIGH -> "Getting tight" to semantic.warn
        MemoryPressure.CRITICAL -> "Low memory" to semantic.critical
    }
    Row(
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape).padding(start = 8.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Spacer(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}
