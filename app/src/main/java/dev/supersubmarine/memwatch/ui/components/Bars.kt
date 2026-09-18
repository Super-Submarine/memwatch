package dev.supersubmarine.memwatch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun MemoryBar(fraction: Float, color: Color, track: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val f = fraction.coerceIn(0f, 1f)
        if (f > 0f) {
            val width = (size.width * f).coerceAtLeast(size.height)
            drawRoundRect(color = color, topLeft = Offset.Zero, size = Size(width, size.height), cornerRadius = radius)
        }
    }
}

data class BarSegment(val fraction: Float, val color: Color)

/** Stacked capacity bar with a hairline gap between segments; leftover shows the track. */
@Composable
fun SegmentedBar(segments: List<BarSegment>, track: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = track, cornerRadius = radius)
        val gap = 2.dp.toPx()
        var x = 0f
        segments.forEach { segment ->
            val width = size.width * segment.fraction.coerceIn(0f, 1f) - gap
            if (width > 0f) {
                drawRoundRect(
                    color = segment.color,
                    topLeft = Offset(x, 0f),
                    size = Size(width, size.height),
                    cornerRadius = CornerRadius(size.height / 3, size.height / 3),
                )
            }
            x += size.width * segment.fraction.coerceIn(0f, 1f)
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Spacer(Modifier.size(8.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
