package dev.supersubmarine.memwatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.supersubmarine.memwatch.shizuku.ShizukuState

/**
 * The road to per-app RAM. Three fixed steps so the user always sees how far along they are;
 * the primary button does whichever step is next.
 */
@Composable
fun ShizukuSetupCard(
    state: ShizukuState,
    onAction: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val done = when (state) {
        ShizukuState.NOT_INSTALLED -> 0
        ShizukuState.NOT_RUNNING -> 1
        ShizukuState.PERMISSION_NEEDED, ShizukuState.PERMISSION_DENIED -> 2
        ShizukuState.READY -> 3
    }
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(20.dp)) {
            Text("One-time setup", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text("See RAM per app", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Android hides other apps' memory from every app. Shizuku lends MemWatch the same access as a USB-debugging PC — no root — so it can read Android's own per-app RAM numbers and force stop apps for you.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Step(1, done, "Install Shizuku", "Free, from the Play Store or GitHub.")
                Step(2, done, "Start it once", "In Shizuku: Start via Wireless debugging → pair. Needed again after a reboot.")
                Step(
                    3,
                    done,
                    "Allow MemWatch",
                    if (state == ShizukuState.PERMISSION_DENIED) "You chose “don't ask again” — allow MemWatch under Authorized apps in Shizuku." else "Approve the request from Shizuku.",
                )
            }
            Spacer(Modifier.height(20.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(
                    when (state) {
                        ShizukuState.NOT_INSTALLED -> "Get Shizuku"
                        ShizukuState.NOT_RUNNING -> "Open Shizuku to start it"
                        ShizukuState.PERMISSION_NEEDED -> "Allow MemWatch"
                        ShizukuState.PERMISSION_DENIED -> "Open Shizuku to allow"
                        ShizukuState.READY -> "Done"
                    },
                )
            }
            TextButton(onClick = onOpenGuide, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text("How Shizuku setup works")
            }
        }
    }
}

@Composable
private fun Step(index: Int, done: Int, title: String, body: String) {
    val isDone = index <= done
    val isCurrent = index == done + 1
    val badge = when {
        isDone -> MaterialTheme.colorScheme.primary
        isCurrent -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val onBadge = when {
        isDone -> MaterialTheme.colorScheme.onPrimary
        isCurrent -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(24.dp).background(badge, CircleShape), contentAlignment = Alignment.Center) {
            if (isDone) {
                Icon(Icons.Rounded.Check, contentDescription = "Done", tint = onBadge, modifier = Modifier.size(14.dp))
            } else {
                Text(index.toString(), style = MaterialTheme.typography.labelMedium, color = onBadge)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (isCurrent || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isCurrent) {
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
