package kz.kbtu.releaseflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shapes: a surface is rounded at 20, a button / dialog / sheet at 24; the smaller steps are
 * for what sits inside a surface (an input, a tint block, a monogram).
 */
val ReleaseFlowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** The Material colour scheme is derived from the palette so stock components (Checkbox, Slider, Dialog) match. */
private fun schemeOf(p: ReleaseFlowColors): ColorScheme {
    val base = if (p.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.accent,
        onPrimary = p.onAccent,
        primaryContainer = p.accentSoft,
        onPrimaryContainer = p.accent,
        secondary = p.accent,
        onSecondary = p.onAccent,
        secondaryContainer = p.surfaceSunken,
        onSecondaryContainer = p.textPrimary,
        tertiary = p.warning,
        onTertiary = p.onAccent,
        error = p.danger,
        onError = p.onAccent,
        errorContainer = p.tint(p.danger),
        onErrorContainer = p.danger,
        background = p.background,
        onBackground = p.textPrimary,
        surface = p.background,
        onSurface = p.textPrimary,
        surfaceVariant = p.surfaceSunken,
        onSurfaceVariant = p.textSecondary,
        surfaceContainerLowest = p.background,
        surfaceContainerLow = p.surface,
        surfaceContainer = p.surface,
        surfaceContainerHigh = p.surface,
        surfaceContainerHighest = p.surfaceSunken,
        outline = p.borderStrong,
        outlineVariant = p.border,
        surfaceTint = Color.Transparent,
        scrim = Color(0xFF000000),
    )
}

/**
 * App theme: Prosper palette + Inter type scale + flat shapes. Dark is the designed theme;
 * light follows the system setting with the same palette on a white canvas.
 */
@Composable
fun ReleaseFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalReleaseFlowColors provides palette) {
        MaterialTheme(
            colorScheme = schemeOf(palette),
            typography = Typography,
            shapes = ReleaseFlowShapes,
            content = content,
        )
    }
}

/** Access point for the semantic colours: `ReleaseFlowTheme.colors.accent`. */
object ReleaseFlowTheme {
    val colors: ReleaseFlowColors
        @Composable
        @ReadOnlyComposable
        get() = LocalReleaseFlowColors.current
}
