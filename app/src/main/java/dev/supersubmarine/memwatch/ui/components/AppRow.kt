package dev.supersubmarine.memwatch.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppUsage
import dev.supersubmarine.memwatch.ui.formatBytes
import dev.supersubmarine.memwatch.ui.formatDuration
import dev.supersubmarine.memwatch.ui.formatRelativeTime
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme

/**
 * One app in the ranked list: real icon, name, activity line, measured storage on the right and
 * a thin bar scaled against the largest app so relative size is visible at a glance.
 */
@Composable
fun AppRow(
    app: AppUsage,
    now: Long,
    largestBytes: Long,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = MemWatchTheme.semantic
    val fraction = if (largestBytes > 0) app.storageBytes.toFloat() / largestBytes else 0f
    Surface(modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick), shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.icon, app.label, size = 40.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = app.storage?.let { formatBytes(it.totalBytes) } ?: "—",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activityLine(app, now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (app.ranInBackgroundRecently) {
                        Spacer(Modifier.width(8.dp))
                        IconLabel(icon = Icons.Rounded.Sync, text = "Background", tint = semantic.warn)
                    }
                }
                MemoryBar(
                    fraction = fraction,
                    color = semantic.ink,
                    track = semantic.trackSubtle,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(4.dp),
                )
            }
        }
    }
}

private fun activityLine(app: AppUsage, now: Long): String = buildString {
    if (app.lastTimeUsedMillis > 0) append("Opened ${formatRelativeTime(app.lastTimeUsedMillis, now)}") else append("Not opened recently")
    if (app.totalForegroundMillis > 0) append(" · ${formatDuration(app.totalForegroundMillis)} on screen")
    if (app.isSystemApp) append(" · System")
}

@Composable
fun AppIcon(icon: ImageBitmap?, label: String, size: androidx.compose.ui.unit.Dp) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun RowDivider() {
    Spacer(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 72.dp, end = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
