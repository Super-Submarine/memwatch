package dev.supersubmarine.memwatch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = AccentDark,
    onPrimary = Gray950,
    primaryContainer = AccentContainerDark,
    onPrimaryContainer = Color(0xFFDBEAFE),
    background = Gray950,
    onBackground = Gray50,
    surface = Gray950,
    onSurface = Gray50,
    surfaceVariant = Gray800,
    onSurfaceVariant = Gray400,
    surfaceContainer = Gray900,
    surfaceContainerHigh = Gray850,
    surfaceContainerHighest = Gray800,
    surfaceContainerLow = Gray900,
    outline = Gray700,
    outlineVariant = Gray800,
    error = RedDark,
    onError = Gray950,
)

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentContainerLight,
    onPrimaryContainer = Color(0xFF1E3A5F),
    background = Gray50,
    onBackground = Gray900,
    surface = Gray50,
    onSurface = Gray900,
    surfaceVariant = Gray100,
    onSurfaceVariant = Gray500,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Gray100,
    surfaceContainerHighest = Gray200,
    surfaceContainerLow = Color.White,
    outline = Gray200,
    outlineVariant = Gray100,
    error = RedLight,
    onError = Color.White,
)

private val MemWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MemWatchTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSemanticColors provides if (darkTheme) DarkSemantic else LightSemantic) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = MemWatchTypography,
            shapes = MemWatchShapes,
            content = content,
        )
    }
}

object MemWatchTheme {
    val semantic: SemanticColors
        @Composable get() = LocalSemanticColors.current
}
