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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

private val steps = listOf("Build", "Test", "Review", "Release")
private val DotSize = 24.dp

/**
 * Four-step progress line Build → Test → Review → Release. A hairline joins the dots; the part
 * already travelled is green, the current step is the accent (or red when the stage is a problem).
 * Uses `ReleaseStage.step` — the UI never decides on its own which step a stage belongs to.
 */
@Composable
fun TrackTimeline(stage: ReleaseStage, modifier: Modifier = Modifier) {
    val colors = ReleaseFlowTheme.colors
    val current = stage.step
    val allDone = stage == ReleaseStage.Live
    val count = steps.size

    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                // Centres of the dots sit at (i + 0.5) / count of the width; the line runs between them.
                val y = DotSize.toPx() / 2
                val slot = size.width / count
                val first = slot / 2
                val last = size.width - slot / 2
                val reached = if (allDone) last else first + slot * current
                drawLine(colors.borderStrong, Offset(first, y), Offset(last, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                if (reached > first) {
                    drawLine(colors.success, Offset(first, y), Offset(reached, y), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                }
            },
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, label ->
            val state = when {
                allDone || index < current -> StepState.Done
                index == current && stage.isProblem -> StepState.Problem
                index == current -> StepState.Current
                else -> StepState.Pending
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                StepDot(state = state, number = index + 1)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = when (state) {
                        StepState.Pending -> colors.textTertiary
                        StepState.Problem -> colors.danger
                        else -> colors.textSecondary
                    },
                )
            }
        }
    }
}

private enum class StepState { Done, Current, Problem, Pending }

@Composable
private fun StepDot(state: StepState, number: Int) {
    val colors = ReleaseFlowTheme.colors
    val background = when (state) {
        StepState.Done -> colors.success
        StepState.Current -> colors.accent
        StepState.Problem -> colors.danger
        StepState.Pending -> colors.surfaceSunken
    }
    Box(
        modifier = Modifier
            .size(DotSize)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            StepState.Done -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(14.dp),
            )
            StepState.Problem -> Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(14.dp),
            )
            StepState.Current -> Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onAccent,
            )
            StepState.Pending -> Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
        }
    }
}
