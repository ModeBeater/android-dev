package kz.kbtu.releaseflow.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Indigo40,
    onPrimary = Snow,
    primaryContainer = IndigoContainerLight,
    onPrimaryContainer = Indigo40,
    secondary = Teal40,
    onSecondary = Snow,
    secondaryContainer = TealContainerLight,
    onSecondaryContainer = Teal40,
    tertiary = Amber40,
    onTertiary = Snow,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = Amber40,
    background = Snow,
    surface = Snow,
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    onPrimary = Ink,
    primaryContainer = IndigoContainerDark,
    onPrimaryContainer = Indigo80,
    secondary = Teal80,
    onSecondary = Ink,
    secondaryContainer = TealContainerDark,
    onSecondaryContainer = Teal80,
    tertiary = Amber80,
    onTertiary = Ink,
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = Amber80,
)

/**
 * Material 3 theme. Dynamic (wallpaper) colors are off by default so the tool looks the
 * same on every device; pass `dynamicColor = true` to try Material You on Android 12+.
 */
@Composable
fun ReleaseFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
