package kz.kbtu.releaseflow.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic colours of the app, the same house palette as Prosper Diagnostics / Prosper Business:
 * a black canvas, flat lighter surfaces with **no outlines**, one purple accent and four status hues.
 * Dark is the primary theme; light is the same palette on a white canvas.
 *
 * Screens read them through `ReleaseFlowTheme.colors` instead of hard-coding Material roles.
 */
@Immutable
data class ReleaseFlowColors(
    val isDark: Boolean,
    /** Page canvas. */
    val background: Color,
    /** A card: a lighter block on the canvas, never a bordered box. */
    val surface: Color,
    /** A quieter block inside a card (inputs, secondary buttons, track of a progress bar). */
    val surfaceSunken: Color,
    /** Hairline separators inside a grouped card — nothing else. */
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSoft: Color,
    val onAccent: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
) {
    /** Translucent fill for pills and badges of the given colour. */
    fun tint(color: Color): Color = color.copy(alpha = if (isDark) 0.18f else 0.10f)
}

val DarkPalette = ReleaseFlowColors(
    isDark = true,
    background = Color(0xFF000000),
    surface = Color(0xFF1A1A1A),
    surfaceSunken = Color(0xFF262628),
    border = Color(0xFF262628),
    borderStrong = Color(0xFF2B2B2E),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF7B7F85),
    textTertiary = Color(0xFF5C6066),
    accent = Color(0xFF8C52FF),
    accentSoft = Color(0x337331F5),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF09AA5C),
    warning = Color(0xFFFAAE17),
    danger = Color(0xFFD52B3B),
    info = Color(0xFF5B8CFF),
)

val LightPalette = ReleaseFlowColors(
    isDark = false,
    background = Color(0xFFF7F7F7),
    surface = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF5F5F5),
    border = Color(0xFFEAEDED),
    borderStrong = Color(0xFFD8DCDC),
    textPrimary = Color(0xFF191B1D),
    textSecondary = Color(0xFF7B7F85),
    textTertiary = Color(0xFFA0A4AA),
    accent = Color(0xFF6A3DC3),
    accentSoft = Color(0x1A7331F5),
    onAccent = Color(0xFFFFFFFF),
    success = Color(0xFF09AA5C),
    // Brand yellow and red are tuned for a black canvas; on white they are darkened so text in them stays readable.
    warning = Color(0xFFB57A00),
    danger = Color(0xFFFF3347),
    info = Color(0xFF3D6FC3),
)

val LocalReleaseFlowColors = staticCompositionLocalOf { DarkPalette }
