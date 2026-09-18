package dev.supersubmarine.memwatch.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primitives
internal val Gray50 = Color(0xFFF7F7F8)
internal val Gray100 = Color(0xFFEFEFF1)
internal val Gray200 = Color(0xFFE2E2E6)
internal val Gray400 = Color(0xFF9A9AA3)
internal val Gray500 = Color(0xFF6E6E78)
internal val Gray700 = Color(0xFF3A3A42)
internal val Gray800 = Color(0xFF26262C)
internal val Gray850 = Color(0xFF1C1C21)
internal val Gray900 = Color(0xFF141417)
internal val Gray950 = Color(0xFF0E0E10)

internal val Accent = Color(0xFF3B82F6)
internal val AccentDark = Color(0xFF60A5FA)
internal val AccentContainerLight = Color(0xFFDBEAFE)
internal val AccentContainerDark = Color(0xFF1E3A5F)

internal val GreenLight = Color(0xFF15803D)
internal val GreenDark = Color(0xFF4ADE80)
internal val AmberLight = Color(0xFFB45309)
internal val AmberDark = Color(0xFFFBBF24)
internal val RedLight = Color(0xFFB91C1C)
internal val RedDark = Color(0xFFF87171)

@Immutable
data class SemanticColors(
    val ok: Color,
    val warn: Color,
    val critical: Color,
    val trackSubtle: Color,
)

internal val LightSemantic = SemanticColors(ok = GreenLight, warn = AmberLight, critical = RedLight, trackSubtle = Gray200)
internal val DarkSemantic = SemanticColors(ok = GreenDark, warn = AmberDark, critical = RedDark, trackSubtle = Gray700)

val LocalSemanticColors = staticCompositionLocalOf { DarkSemantic }
