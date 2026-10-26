package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.util.dateTimeText
import kz.kbtu.releaseflow.domain.util.relativeDays
import kz.kbtu.releaseflow.ui.ActivityItem
import kz.kbtu.releaseflow.ui.AppUi
import kz.kbtu.releaseflow.ui.ReleaseUi

/** A release in a list: version, planned date, one stage badge per platform, checklist progress. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseCard(
    release: ReleaseUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showAppName: Boolean = true,
) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (showAppName) "${release.appName} ${release.version}" else "Version ${release.version}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = release.plannedAt?.let { "planned ${it.relativeDays()}" } ?: "no date",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                release.tracks.forEach { track ->
                    PlatformChip(platform = track.platform)
                    StageBadge(stage = track.stage)
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            ChecklistProgress(done = release.checklistDone, total = release.checklistTotal)
        }
    }
}

/** Thin progress bar with "6/9 checklist" caption. */
@Composable
fun ChecklistProgress(done: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Checklist $done / $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** An app in the list: monogram, name, ids and the latest version's badges. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCard(app: AppUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ElevatedCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AppMonogram(monogram = app.monogram, modifier = Modifier.size(52.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = app.bundleId,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                val latest = app.latest
                if (latest == null) {
                    Text(text = "No releases yet", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = latest.version, style = MaterialTheme.typography.bodyMedium)
                        latest.tracks.forEach { StageBadge(stage = it.stage) }
                    }
                }
            }
            Text(
                text = "${app.releases.size} rel.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Checklist line: checkbox, title, platform tag and the guideline the reviewer would cite. */
@Composable
fun ChecklistRow(item: ChecklistItem, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = item.done, onCheckedChange = { onToggle() })
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            if (item.guideline != null) {
                Text(
                    text = "Reviewer checks: ${item.guideline}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        item.platform?.let { PlatformChip(platform = it) }
    }
}

/** One line of the activity feed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityRow(item: ActivityItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "${item.appName} ${item.version}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = item.at.dateTimeText(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlatformChip(platform = item.platform)
            Spacer(modifier = Modifier.width(6.dp))
            StageBadge(stage = item.stage)
        }
    }
}
