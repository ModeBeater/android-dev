package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.ui.theme.LiveGreen
import kz.kbtu.releaseflow.ui.theme.LiveGreenContainer
import kz.kbtu.releaseflow.ui.theme.LiveGreenContainerDark

/** Container/content color pair for a stage — one place that decides what each stage looks like. */
@Composable
fun stageColors(stage: ReleaseStage): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    return when (stage) {
        ReleaseStage.Draft -> scheme.surfaceVariant to scheme.onSurfaceVariant
        is ReleaseStage.Building, is ReleaseStage.Testing -> scheme.secondaryContainer to scheme.onSecondaryContainer
        ReleaseStage.Submitted, ReleaseStage.InReview -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        is ReleaseStage.Rejected, is ReleaseStage.Halted -> scheme.errorContainer to scheme.onErrorContainer
        ReleaseStage.Approved, is ReleaseStage.RollingOut -> scheme.primaryContainer to scheme.onPrimaryContainer
        ReleaseStage.Live -> (if (dark) LiveGreenContainerDark else LiveGreenContainer) to (if (dark) LiveGreenContainer else LiveGreen)
    }
}

/** Colored pill with the stage label — exhaustive `when` over the sealed class via [stageColors]. */
@Composable
fun StageBadge(stage: ReleaseStage, modifier: Modifier = Modifier) {
    val (container, content) = stageColors(stage)
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = stage.label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

/** Tiny "iOS" / "Android" tag. */
@Composable
fun PlatformChip(platform: Platform, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (platform) {
        Platform.IOS -> scheme.surfaceVariant to scheme.onSurfaceVariant
        Platform.ANDROID -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Surface(modifier = modifier, shape = MaterialTheme.shapes.extraSmall, color = container, contentColor = content) {
        Text(
            text = platform.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

/** Round app icon placeholder with the app's monogram — a `Box` stacking background and text. */
@Composable
fun AppMonogram(monogram: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = monogram,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}
