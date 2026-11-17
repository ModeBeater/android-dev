package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * A small pill: tinted fill with the text in the same colour, or `filled` for the one state
 * that must read loudest (Live). Reusable component used by every list and card.
 */
@Composable
fun AppPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    dense: Boolean = false,
    mono: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val colors = ReleaseFlowTheme.colors
    val foreground = if (filled) colors.onAccent else color
    val base = if (mono) MonoText.small else MaterialTheme.typography.labelSmall
    Row(
        modifier = modifier
            .background(if (filled) color else colors.tint(color), CircleShape)
            .padding(horizontal = if (dense) 8.dp else 10.dp, vertical = if (dense) 3.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = label,
            style = base.copy(fontSize = if (dense) 11.sp else 12.sp, lineHeight = 14.sp),
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** One colour per stage — the single place that decides how a stage looks; exhaustive over the sealed class. */
@Composable
fun stageColor(stage: ReleaseStage): Color {
    val colors = ReleaseFlowTheme.colors
    return when (stage) {
        ReleaseStage.Draft -> colors.textSecondary
        is ReleaseStage.Building -> colors.info
        is ReleaseStage.Testing -> colors.accent
        ReleaseStage.Submitted, ReleaseStage.InReview -> colors.warning
        is ReleaseStage.Rejected, is ReleaseStage.Halted -> colors.danger
        ReleaseStage.Approved, is ReleaseStage.RollingOut, ReleaseStage.Live -> colors.success
    }
}

/** Stage pill: tinted for everything in flight, filled green once the build is live. */
@Composable
fun StageBadge(stage: ReleaseStage, modifier: Modifier = Modifier, dense: Boolean = false) {
    AppPill(
        label = stage.label,
        color = stageColor(stage),
        filled = stage == ReleaseStage.Live,
        dense = dense,
        modifier = modifier,
    )
}

/** Platform identity colour: iOS reads in the text colour, Android in the store's green. */
@Composable
fun platformColor(platform: Platform): Color = when (platform) {
    Platform.IOS -> ReleaseFlowTheme.colors.textPrimary
    Platform.ANDROID -> ReleaseFlowTheme.colors.success
}

/** Small coloured dot that marks a platform in dense rows. */
@Composable
fun PlatformDot(platform: Platform, modifier: Modifier = Modifier, size: Int = 8) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(platformColor(platform), CircleShape),
    )
}

/** "● iOS" / "● Android" on a quiet sunken pill. */
@Composable
fun PlatformChip(platform: Platform, modifier: Modifier = Modifier, dense: Boolean = false) {
    val colors = ReleaseFlowTheme.colors
    Row(
        modifier = modifier
            .background(colors.surfaceSunken, CircleShape)
            .padding(horizontal = if (dense) 8.dp else 10.dp, vertical = if (dense) 3.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlatformDot(platform = platform, size = 7)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = platform.label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (dense) 11.sp else 12.sp, lineHeight = 14.sp),
            color = colors.textSecondary,
            maxLines = 1,
        )
    }
}

/** Rounded-square app mark with the app's monogram in the accent colour — the product mark of the list. */
@Composable
fun AppMonogram(monogram: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val colors = ReleaseFlowTheme.colors
    Box(
        modifier = modifier
            .size(if (large) 56.dp else 44.dp)
            .background(colors.accentSoft, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram,
            style = if (large) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            color = colors.accent,
        )
    }
}
