package dev.supersubmarine.memwatch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.MemoryPressure
import dev.supersubmarine.memwatch.data.MemorySnapshot
import dev.supersubmarine.memwatch.ui.formatBytes
import dev.supersubmarine.memwatch.ui.formatBytesSigned
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DELTA_NOISE_FLOOR_BYTES = 16L * 1024 * 1024

@Composable
fun MemoryHero(memory: MemorySnapshot?, previous: MemorySnapshot?, modifier: Modifier = Modifier) {
    val semantic = MemWatchTheme.semantic
    val pressure = memory?.pressure ?: MemoryPressure.NORMAL
    val barColor = when (pressure) {
        MemoryPressure.NORMAL -> MaterialTheme.colorScheme.primary
        MemoryPressure.HIGH -> semantic.warn
        MemoryPressure.CRITICAL -> semantic.critical
    }
    val usedFraction by animateFloatAsState(
        targetValue = memory?.usedFraction ?: 0f,
        animationSpec = tween(durationMillis = 600),
        label = "usedFraction",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "In use",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                PressureBadge(pressure)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = memory?.let { formatBytes(it.usedBytes) } ?: "—",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = memory?.let { "of ${formatBytes(it.totalBytes)}" } ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = memory?.let { "${(it.usedFraction * 100).roundToInt()}%" } ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            MemoryBar(
                fraction = usedFraction,
                color = barColor,
                track = semantic.trackSubtle,
                modifier = Modifier.fillMaxWidth().height(12.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = pressureCopy(pressure),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                val delta = if (memory != null && previous != null) memory.usedBytes - previous.usedBytes else 0L
                AnimatedVisibility(visible = abs(delta) >= DELTA_NOISE_FLOOR_BYTES, enter = fadeIn(), exit = fadeOut()) {
                    Text(
                        text = formatBytesSigned(delta),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (delta < 0) semantic.ok else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(Modifier.size(8.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.SemiBold)
    }
}

private fun pressureCopy(pressure: MemoryPressure): String = when (pressure) {
    MemoryPressure.NORMAL -> "Android keeps recent apps cached and drops them the moment something needs the space."
    MemoryPressure.HIGH -> "Less than a fifth is free. Apps may reload from scratch when you switch back to them."
    MemoryPressure.CRITICAL -> "The system is below its low-memory threshold and is closing background apps."
}

@Composable
fun MemoryBar(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val width = (size.width * fraction.coerceIn(0f, 1f)).coerceAtLeast(size.height)
        drawRoundRect(color = color, topLeft = Offset.Zero, size = Size(width, size.height), cornerRadius = radius)
    }
}
