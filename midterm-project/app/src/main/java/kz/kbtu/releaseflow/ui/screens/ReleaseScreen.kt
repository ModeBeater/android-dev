package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kz.kbtu.releaseflow.domain.model.StageEvent
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.util.ageText
import kz.kbtu.releaseflow.domain.util.dateTimeText
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.domain.util.relativeDays
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.TrackUi
import kz.kbtu.releaseflow.ui.components.ChecklistProgress
import kz.kbtu.releaseflow.ui.components.DangerButton
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.GroupedCard
import kz.kbtu.releaseflow.ui.components.Hairline
import kz.kbtu.releaseflow.ui.components.KeyValueRow
import kz.kbtu.releaseflow.ui.components.LinkButton
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PlatformChip
import kz.kbtu.releaseflow.ui.components.PlatformDot
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.RolloutDialog
import kz.kbtu.releaseflow.ui.components.SecondaryButton
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.components.StageBadge
import kz.kbtu.releaseflow.ui.components.TrackTimeline
import kz.kbtu.releaseflow.ui.components.stageColor
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

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
    val colors = ReleaseFlowTheme.colors
    val release = state.release(releaseId)
    if (release == null) {
        EmptyState(icon = Icons.Default.Warning, title = "Release not found", subtitle = "Release #$releaseId does not exist.", modifier = modifier)
        return
    }

    // Which platform's rollout dialog is open ("" = none) — survives rotation.
    var rolloutFor by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FlatCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = release.appName, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                        Text(text = "Version ${release.version}", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                    }
                    LinkButton(text = "App", onClick = onOpenApp, trailingIcon = Icons.Default.KeyboardArrowRight)
                }
                if (release.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = release.notes, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
                Spacer(modifier = Modifier.height(14.dp))
                ChecklistProgress(done = release.checklistDone, total = release.checklistTotal, label = "Submission checklist")
                LinkButton(
                    text = "Open submission checklist",
                    onClick = onOpenChecklist,
                    trailingIcon = Icons.Default.KeyboardArrowRight,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }

        item {
            GroupedCard(
                items = listOf(
                    "Created" to release.createdAt.dayText(),
                    "Planned" to (release.plannedAt?.let { "${it.dayText()} · ${it.relativeDays(state.now)}" } ?: "not set"),
                ),
            ) { (label, value) ->
                KeyValueRow(label = label, value = value, icon = Icons.Default.DateRange, iconTint = colors.info, valueColor = colors.textSecondary)
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
            SectionHeader(title = "History")
            FlatCard(padding = PaddingValues(0.dp)) {
                release.tracks.forEachIndexed { trackIndex, track ->
                    if (trackIndex > 0) Hairline(startIndent = 0)
                    track.history.asReversed().forEachIndexed { eventIndex, event ->
                        if (eventIndex > 0) Hairline()
                        HistoryRow(platform = track.platform, event = event)
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

/** One platform of the release: chip, store and build, stage pill, timeline, "since" and the allowed actions. */
@Composable
private fun TrackCard(
    track: TrackUi,
    isProcessing: Boolean,
    now: Long,
    onAction: (TrackAction) -> Unit,
) {
    val colors = ReleaseFlowTheme.colors
    val stage = track.stage
    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlatformChip(platform = track.platform)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = track.platform.store, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(text = "build ${track.buildNumber}", style = MonoText.small, color = colors.textTertiary)
            }
            StageBadge(stage = stage)
        }
        Spacer(modifier = Modifier.height(18.dp))
        TrackTimeline(stage = stage)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "${stage.label} for ${track.enteredAt.ageText(now)}",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        if (stage is ReleaseStage.Rejected) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.tint(colors.danger), MaterialTheme.shapes.small)
                    .padding(12.dp),
            ) {
                Text(text = stage.reason, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
                stage.guideline?.let {
                    Text(text = "Guideline $it", style = MonoText.small, color = colors.danger)
                }
            }
        }
        if (stage is ReleaseStage.RollingOut) {
            Spacer(modifier = Modifier.height(10.dp))
            RolloutMeter(percent = stage.percent)
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (isProcessing) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (stage is ReleaseStage.Building) "Asking CI…" else "Asking ${track.platform.store}…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
            }
        } else if (track.actions.isEmpty()) {
            Text(
                text = "Live — nothing left to do on ${track.platform.label}.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.success,
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                track.actions.forEachIndexed { index, action ->
                    when {
                        action == TrackAction.Halt -> DangerButton(text = action.label, onClick = { onAction(action) }, modifier = Modifier.weight(1f))
                        index == 0 -> PrimaryButton(text = action.label, onClick = { onAction(action) }, modifier = Modifier.weight(1f))
                        else -> SecondaryButton(text = action.label, onClick = { onAction(action) }, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Share of users on the staged rollout, as a thin bar. */
@Composable
private fun RolloutMeter(percent: Int) {
    val colors = ReleaseFlowTheme.colors
    Column {
        Row {
            Text(text = "Rollout", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
            Text(text = "$percent %", style = MonoText.small, color = colors.textSecondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.surfaceSunken, MaterialTheme.shapes.extraLarge),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent.coerceIn(1, 100) / 100f)
                    .height(6.dp)
                    .background(colors.success, MaterialTheme.shapes.extraLarge),
            )
        }
    }
}

/** One line of the history: platform dot in a stage-tinted disc, the stage, its detail and the time. */
@Composable
private fun HistoryRow(platform: Platform, event: StageEvent) {
    val colors = ReleaseFlowTheme.colors
    val detail = when (val stage = event.stage) {
        is ReleaseStage.Rejected -> "${stage.reason}${stage.guideline?.let { " · $it" } ?: ""}"
        is ReleaseStage.Building -> stage.pipeline
        is ReleaseStage.Testing -> stage.channel
        is ReleaseStage.RollingOut -> "${stage.percent} % of users"
        is ReleaseStage.Halted -> event.note ?: "at ${stage.percent} %"
        else -> event.note ?: ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(colors.tint(stageColor(event.stage)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            PlatformDot(platform = platform, size = 7)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = event.stage.label, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            if (detail.isNotBlank()) {
                Text(text = detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = event.at.dateTimeText(), style = MonoText.small, color = colors.textTertiary)
    }
}
