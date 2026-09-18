package dev.supersubmarine.memwatch.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Primitives: cool grays. Light mode layers white cards on a tinted canvas; dark mode layers
// progressively lighter surfaces on a near-black canvas (never pure black).
internal val Gray50 = Color(0xFFF2F3F8)
internal val Gray100 = Color(0xFFE9EAF1)
internal val Gray200 = Color(0xFFDCDEE6)
internal val Gray400 = Color(0xFF8F9099)
internal val Gray500 = Color(0xFF666773)
internal val Gray700 = Color(0xFF3B3C45)
internal val Gray800 = Color(0xFF2B2C33)
internal val Gray850 = Color(0xFF1F2026)
internal val Gray900 = Color(0xFF17181D)
internal val Gray950 = Color(0xFF101114)

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
    /** Fill for the primary capacity bar when memory is healthy — ink, not brand colour. */
    val ink: Color,
    /** Reclaimable cache segment of the capacity bar. */
    val cache: Color,
)

internal val LightSemantic = SemanticColors(
    ok = GreenLight, warn = AmberLight, critical = RedLight, trackSubtle = Gray100, ink = Gray900, cache = Gray400,
)
internal val DarkSemantic = SemanticColors(
    ok = GreenDark, warn = AmberDark, critical = RedDark, trackSubtle = Gray700, ink = Gray50, cache = Gray500,
)

val LocalSemanticColors = staticCompositionLocalOf { DarkSemantic }
