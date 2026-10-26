package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.util.ageText
import kz.kbtu.releaseflow.domain.util.dateTimeText
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.domain.util.relativeDays
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.TrackUi
import kz.kbtu.releaseflow.ui.components.ChecklistProgress
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.PlatformChip
import kz.kbtu.releaseflow.ui.components.RolloutDialog
import kz.kbtu.releaseflow.ui.components.SectionTitle
import kz.kbtu.releaseflow.ui.components.StageBadge
import kz.kbtu.releaseflow.ui.components.SummaryRow
import kz.kbtu.releaseflow.ui.components.TrackTimeline

/**
 * The heart of the app: one release, one card per platform with its timeline, the actions
 * the workflow allows right now (decided by the domain, not the UI), the checklist summary
 * and the full stage history.
 */
@Composable
fun ReleaseScreen(
    state: ReleaseUiState,
    releaseId: Int,
    onAction: (Platform, TrackAction, Int?) -> Unit,
    onOpenChecklist: () -> Unit,
    onOpenApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val release = state.release(releaseId)
    if (release == null) {
        EmptyState(icon = Icons.Default.Warning, title = "Release not found", subtitle = "Release #$releaseId does not exist.", modifier = modifier)
        return
    }

    // Which platform's rollout dialog is open ("" = none) — survives rotation.
    var rolloutFor by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${release.appName} ${release.version}",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = onOpenApp) { Text("App") }
                    }
                    if (release.notes.isNotBlank()) {
                        Text(text = release.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryRow(label = "Created", value = release.createdAt.dayText())
                    SummaryRow(
                        label = "Planned",
                        value = release.plannedAt?.let { "${it.dayText()} (${it.relativeDays(state.now)})" } ?: "—",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChecklistProgress(done = release.checklistDone, total = release.checklistTotal)
                    TextButton(onClick = onOpenChecklist, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Open submission checklist")
                    }
                }
            }
        }

        items(release.tracks.size) { index ->
            val track = release.tracks[index]
            TrackCard(
                track = track,
                isProcessing = state.isProcessing(track),
                now = state.now,
                onAction = { action ->
                    if (action == TrackAction.IncreaseRollout) rolloutFor = track.platform.name
                    else onAction(track.platform, action, null)
                },
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("History")
                    release.tracks.forEach { track ->
                        track.history.asReversed().forEach { event ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                PlatformChip(platform = track.platform)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = event.stage.label, style = MaterialTheme.typography.bodyLarge)
                                    val detail = when (val stage = event.stage) {
                                        is ReleaseStage.Rejected -> "${stage.reason}${stage.guideline?.let { " · $it" } ?: ""}"
                                        is ReleaseStage.Building -> stage.pipeline
                                        is ReleaseStage.Testing -> stage.channel
                                        else -> event.note ?: ""
                                    }
                                    if (detail.isNotBlank()) {
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                Text(
                                    text = event.at.dateTimeText(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                    }
                }
            }
        }
    }

    val rolloutTrack = release.tracks.firstOrNull { it.platform.name == rolloutFor }
    val currentPercent = (rolloutTrack?.stage as? ReleaseStage.RollingOut)?.percent
    if (rolloutTrack != null && currentPercent != null) {
        RolloutDialog(
            currentPercent = currentPercent,
            onConfirm = { percent ->
                rolloutFor = ""
                onAction(rolloutTrack.platform, TrackAction.IncreaseRollout, percent)
            },
            onDismiss = { rolloutFor = "" },
        )
    }
}

/** One platform of the release: badge, timeline, "since" and the allowed actions. */
@Composable
private fun TrackCard(
    track: TrackUi,
    isProcessing: Boolean,
    now: Long,
    onAction: (TrackAction) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlatformChip(platform = track.platform)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${track.platform.store} · build ${track.buildNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                StageBadge(stage = track.stage)
            }
            Spacer(modifier = Modifier.height(12.dp))
            TrackTimeline(stage = track.stage)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${track.stage.label} for ${track.enteredAt.ageText(now)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val stage = track.stage
            if (stage is ReleaseStage.Rejected) {
                Text(
                    text = "Reason: ${stage.reason}" + (stage.guideline?.let { " ($it)" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (isProcessing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (stage is ReleaseStage.Building) "Asking CI…" else "Asking ${track.platform.store}…",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else if (track.actions.isEmpty()) {
                Text(
                    text = "Live — nothing left to do on ${track.platform.label}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    track.actions.forEachIndexed { index, action ->
                        if (index == 0) {
                            Button(onClick = { onAction(action) }, modifier = Modifier.weight(1f)) { Text(action.label) }
                        } else if (action == TrackAction.Halt) {
                            OutlinedButton(onClick = { onAction(action) }, modifier = Modifier.weight(1f)) { Text(action.label) }
                        } else {
                            FilledTonalButton(onClick = { onAction(action) }, modifier = Modifier.weight(1f)) { Text(action.label) }
                        }
                    }
                }
            }
        }
    }
}
