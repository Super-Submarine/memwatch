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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppUsage
import dev.supersubmarine.memwatch.ui.formatDuration
import dev.supersubmarine.memwatch.ui.formatRelativeTime
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme

@Composable
fun AppRow(app: AppUsage, now: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val semantic = MemWatchTheme.semantic
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.icon, app.label, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val secondary = buildString {
                if (app.lastTimeUsedMillis > 0) append("Opened ${formatRelativeTime(app.lastTimeUsedMillis, now)}") else append("Not opened recently")
                if (app.totalForegroundMillis > 0) append(" · ${formatDuration(app.totalForegroundMillis)} on screen")
            }
            Text(
                text = secondary,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.ranInBackgroundRecently) {
                IconLabel(
                    icon = Icons.Rounded.Sync,
                    text = "Ran in background ${formatRelativeTime(app.lastBackgroundServiceMillis, now)}",
                    tint = semantic.warn,
                )
            }
        }
        if (app.isSystemApp) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "System",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
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
            .padding(start = 56.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}
