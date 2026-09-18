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
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.data.AppMemory
import dev.supersubmarine.memwatch.data.OomTier
import dev.supersubmarine.memwatch.ui.formatBytes
import dev.supersubmarine.memwatch.ui.theme.MemWatchTheme

/**
 * One app in the RAM ranking: icon, name, what state it's in, its live RAM on the right and a
 * bar scaled against the biggest app so relative size reads at a glance. Reclaimable (cached)
 * apps get a muted bar — they cost nothing to keep.
 */
@Composable
fun AppRow(
    app: AppMemory,
    largestBytes: Long,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = MemWatchTheme.semantic
    val fraction = if (largestBytes > 0) app.pssBytes.toFloat() / largestBytes else 0f
    Surface(modifier.fillMaxWidth().clip(shape).clickable(onClick = onClick), shape = shape, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.icon, size = 40.dp)
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
                        text = formatBytes(app.pssBytes),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                Text(
                    text = stateLine(app),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (app.tier == OomTier.SERVICE || app.tier == OomTier.PERSISTENT) semantic.warn else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MemoryBar(
                    fraction = fraction,
                    color = if (app.isReclaimable) semantic.cache else semantic.ink,
                    track = semantic.trackSubtle,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp).height(4.dp),
                )
            }
        }
    }
}

private fun stateLine(app: AppMemory): String = buildString {
    append(app.tier.label)
    if (app.processCount > 1) append(" · ${app.processCount} processes")
    if (app.isSystemApp && app.packageName != null && app.tier != OomTier.SYSTEM) append(" · System")
}

@Composable
fun AppIcon(icon: ImageBitmap?, size: androidx.compose.ui.unit.Dp) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(size / 2))
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
