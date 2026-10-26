package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.ReleaseStage

private val steps = listOf("Build", "Test", "Review", "Release")

/**
 * Four-step progress line Build → Test → Review → Release.
 * Steps before the current one are done, the current one is highlighted (or red when the
 * stage is a problem), the rest are inactive. Uses `ReleaseStage.step`.
 */
@Composable
fun TrackTimeline(stage: ReleaseStage, modifier: Modifier = Modifier) {
    val current = stage.step
    val done = stage == ReleaseStage.Live

    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        steps.forEachIndexed { index, label ->
            val state = when {
                done || index < current -> StepState.Done
                index == current && stage.isProblem -> StepState.Problem
                index == current -> StepState.Current
                else -> StepState.Pending
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                StepDot(state = state, number = index + 1)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = if (state == StepState.Pending) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private enum class StepState { Done, Current, Problem, Pending }

@Composable
private fun StepDot(state: StepState, number: Int) {
    val scheme = MaterialTheme.colorScheme
    val background = when (state) {
        StepState.Done -> scheme.primary
        StepState.Current -> scheme.tertiary
        StepState.Problem -> scheme.error
        StepState.Pending -> scheme.surfaceVariant
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StepState.Done -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp),
            )
            StepState.Problem -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = scheme.onError,
                modifier = Modifier.size(16.dp),
            )
            StepState.Current -> Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onTertiary,
            )
            StepState.Pending -> Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
        }
    }
}
