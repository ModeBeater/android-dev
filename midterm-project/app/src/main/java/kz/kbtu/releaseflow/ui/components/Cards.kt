package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.util.dateTimeText
import kz.kbtu.releaseflow.domain.util.relativeDays
import kz.kbtu.releaseflow.ui.ActivityItem
import kz.kbtu.releaseflow.ui.AppUi
import kz.kbtu.releaseflow.ui.AttentionItem
import kz.kbtu.releaseflow.ui.ReleaseUi
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/** A release in a list: version in mono, planned date, one platform row with its stage pill, checklist progress. */
@Composable
fun ReleaseCard(
    release: ReleaseUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAppName: Boolean = true,
) {
    val colors = ReleaseFlowTheme.colors
    FlatCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                if (showAppName) {
                    Text(
                        text = release.appName,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = release.version,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
            }
            Text(
                text = release.plannedAt?.let { "planned ${it.relativeDays()}" } ?: "no date",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        release.tracks.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlatformChip(platform = track.platform, dense = true)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "build ${track.buildNumber}",
                    style = MonoText.small,
                    color = colors.textTertiary,
                    modifier = Modifier.weight(1f),
                )
                StageBadge(stage = track.stage, dense = true)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        ChecklistProgress(done = release.checklistDone, total = release.checklistTotal)
    }
}

/** An app in the list: monogram, name, bundle id in mono, the latest version with its stage pills, release count. */
@Composable
fun AppCard(app: AppUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReleaseFlowTheme.colors
    FlatCard(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppMonogram(monogram = app.monogram)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = app.bundleId,
                    style = MonoText.small,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = app.releases.size.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                )
                Text(
                    text = if (app.releases.size == 1) "release" else "releases",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            }
        }
        val latest = app.latest
        Spacer(modifier = Modifier.height(12.dp))
        if (latest == null) {
            Text(text = "No releases yet", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Latest", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                AppPill(label = latest.version, color = colors.textSecondary, dense = true, mono = true)
                latest.tracks.forEach { track ->
                    AppPill(
                        label = track.stage.label,
                        color = stageColor(track.stage),
                        dense = true,
                        filled = track.stage == ReleaseStage.Live,
                        leading = { PlatformDot(platform = track.platform, size = 6) },
                    )
                }
            }
        }
    }
}

/** Checklist line: checkbox, title, platform tag and the guideline the reviewer would cite. */
@Composable
fun ChecklistRow(item: ChecklistItem, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReleaseFlowTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 4.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCheckbox(checked = item.done, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                color = if (item.done) colors.textTertiary else colors.textPrimary,
            )
            if (item.guideline != null) {
                Text(
                    text = "Reviewer checks: ${item.guideline}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.done) colors.textTertiary else colors.warning,
                )
            }
        }
        item.platform?.let {
            Spacer(modifier = Modifier.width(8.dp))
            PlatformChip(platform = it, dense = true)
        }
    }
}

/** One line of the activity feed: platform dot, "App 3.5.0", the stage it entered and when. */
@Composable
fun ActivityRow(item: ActivityItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReleaseFlowTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.tint(stageColor(item.stage)), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            PlatformDot(platform = item.platform, size = 8)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${item.appName} ${item.version}",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${item.platform.label} · ${item.at.dateTimeText()}",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        StageBadge(stage = item.stage, dense = true)
    }
}

/** Something to look at today: red for a problem, yellow for a long wait, blue for a deadline. */
@Composable
fun AttentionRow(item: AttentionItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReleaseFlowTheme.colors
    val tone: Color = when (item.severity) {
        AttentionItem.Severity.Problem -> colors.danger
        AttentionItem.Severity.Waiting -> colors.warning
        AttentionItem.Severity.Upcoming -> colors.info
    }
    val icon: ImageVector = when (item.severity) {
        AttentionItem.Severity.Problem -> Icons.Default.Warning
        AttentionItem.Severity.Waiting -> Icons.Default.Notifications
        AttentionItem.Severity.Upcoming -> Icons.Default.DateRange
    }
    KeyValueRow(
        label = item.title,
        caption = item.detail,
        icon = icon,
        iconTint = tone,
        onClick = onClick,
        modifier = modifier,
    )
}
