package kz.kbtu.furnistore.ui.theme

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
    primary = Oak40,
    onPrimary = Linen,
    primaryContainer = OakContainerLight,
    onPrimaryContainer = Oak40,
    secondary = Sage40,
    onSecondary = Linen,
    secondaryContainer = SageContainerLight,
    onSecondaryContainer = Sage40,
    tertiary = Clay40,
    onTertiary = Linen,
    tertiaryContainer = ClayContainerLight,
    onTertiaryContainer = Clay40,
    background = Linen,
    surface = Linen,
)

private val DarkColors = darkColorScheme(
    primary = Oak80,
    onPrimary = Charcoal,
    primaryContainer = OakContainerDark,
    onPrimaryContainer = Oak80,
    secondary = Sage80,
    onSecondary = Charcoal,
    secondaryContainer = SageContainerDark,
    onSecondaryContainer = Sage80,
    tertiary = Clay80,
    onTertiary = Charcoal,
    tertiaryContainer = ClayContainerDark,
    onTertiaryContainer = Clay80,
)

/**
 * Material 3 theme for the app. Dynamic (wallpaper-based) colors are off by default
 * so the store keeps its own brand palette on every device; pass `dynamicColor = true`
 * to try Material You on Android 12+.
 */
@Composable
fun FurniStoreTheme(
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
