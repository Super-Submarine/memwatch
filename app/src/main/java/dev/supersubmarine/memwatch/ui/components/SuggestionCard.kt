package dev.supersubmarine.memwatch.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Task card: icon tile, what you get, and one primary action that hands off to Android
 * Settings. Mirrors the "suggested clean-up" pattern in Files by Google.
 */
@Composable
fun SuggestionCard(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    val container = if (emphasized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val onContainer = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    val onContainerMuted = if (emphasized) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val tile = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
    val onTile = if (emphasized) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = container) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(44.dp).background(tile, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = onTile, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, style = MaterialTheme.typography.labelMedium, color = onContainerMuted)
                    Spacer(Modifier.height(2.dp))
                    Text(title, style = MaterialTheme.typography.titleLarge, color = onContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = onContainerMuted)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(actionLabel)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}
