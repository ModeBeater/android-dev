package kz.kbtu.releaseflow.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kz.kbtu.releaseflow.R

/**
 * Inter is a variable font (wght 100–900). `Font(resId, weight)` pins the `wght` axis to the
 * requested weight on API 26+, so every weight below renders as a real instance, not a faked bold.
 */
val InterFamily = FontFamily(
    Font(R.font.inter, FontWeight.Normal),
    Font(R.font.inter, FontWeight.Medium),
    Font(R.font.inter, FontWeight.SemiBold),
    Font(R.font.inter, FontWeight.Bold),
)

/** Versions, build numbers, bundle ids: content that must line up, so it is monospaced. */
val MonoFamily = FontFamily(
    Font(R.font.roboto_mono_regular, FontWeight.Normal),
    Font(R.font.roboto_mono_medium, FontWeight.Medium),
)

private fun inter(size: TextUnit, weight: FontWeight, lineHeight: TextUnit, letterSpacing: TextUnit = 0.sp) = TextStyle(
    fontFamily = InterFamily,
    fontSize = size,
    fontWeight = weight,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

/** Prosper's type scale mapped onto the Material 3 slots the components read. */
val Typography = Typography(
    displaySmall = inter(36.sp, FontWeight.SemiBold, 40.sp),
    headlineMedium = inter(28.sp, FontWeight.SemiBold, 32.sp, (-0.32).sp),
    headlineSmall = inter(24.sp, FontWeight.Bold, 28.sp, (-0.24).sp),
    titleLarge = inter(20.sp, FontWeight.SemiBold, 28.sp, 0.16.sp),
    titleMedium = inter(18.sp, FontWeight.SemiBold, 24.sp, (-0.18).sp),
    titleSmall = inter(16.sp, FontWeight.Medium, 22.sp, (-0.16).sp),
    bodyLarge = inter(16.sp, FontWeight.Normal, 22.sp),
    bodyMedium = inter(14.sp, FontWeight.Normal, 20.sp, (-0.14).sp),
    bodySmall = inter(12.sp, FontWeight.Normal, 16.sp, 0.16.sp),
    labelLarge = inter(16.sp, FontWeight.Medium, 22.sp, (-0.16).sp),
    labelMedium = inter(14.sp, FontWeight.Medium, 20.sp),
    labelSmall = inter(12.sp, FontWeight.Medium, 16.sp, 0.16.sp),
)

/** Big tabular number of a KPI tile. */
val KpiNumberStyle = inter(28.sp, FontWeight.SemiBold, 32.sp, (-0.32).sp).copy(fontFeatureSettings = "tnum")

/** Monospace for ids, versions and build numbers. */
fun monoStyle(size: TextUnit = 12.sp, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = MonoFamily,
    fontSize = size,
    fontWeight = weight,
    lineHeight = size * 1.5,
    fontFeatureSettings = "tnum",
)

/** Ready-made monospace styles (colour is applied by the caller). */
object MonoText {
    val small: TextStyle = monoStyle(12.sp)
    val medium: TextStyle = monoStyle(13.sp, FontWeight.Medium)
    val large: TextStyle = monoStyle(15.sp, FontWeight.Medium)
}
