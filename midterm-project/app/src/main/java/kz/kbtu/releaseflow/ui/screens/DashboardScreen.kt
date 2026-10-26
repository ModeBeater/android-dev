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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.util.durationText
import kz.kbtu.releaseflow.ui.AttentionItem
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.ActivityRow
import kz.kbtu.releaseflow.ui.components.ReleaseCard
import kz.kbtu.releaseflow.ui.components.SectionTitle
import kz.kbtu.releaseflow.ui.components.StatCard

/**
 * Dashboard: what is in flight, what needs a decision, what is due soon, what just happened.
 * Every number is a derived getter of [ReleaseUiState].
 */
@Composable
fun DashboardScreen(
    state: ReleaseUiState,
    onOpenRelease: (Int) -> Unit,
    onOpenApps: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val attention = state.attention
    val upcoming = state.upcoming
    val activity = state.recentActivity
    val stats = state.stats

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "In review",
                    value = state.tracksInReview.toString(),
                    hint = "tracks waiting for the stores",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                StatCard(
                    title = "Rolling out",
                    value = state.tracksRollingOut.toString(),
                    hint = "staged on Google Play",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Needs a decision",
                    value = state.problems.toString(),
                    hint = if (state.problems == 0) "no rejections or halts" else "rejected or halted",
                    modifier = Modifier.weight(1f),
                    containerColor = if (state.problems == 0) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.errorContainer,
                    contentColor = if (state.problems == 0) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onErrorContainer,
                )
                StatCard(
                    title = "Went live, 30 d",
                    value = state.liveLast30Days.toString(),
                    hint = stats?.averageReviewMs?.values?.filterNotNull()?.minOrNull()?.let { "fastest review ${it.durationText()}" }
                        ?: "no reviews yet",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Needs attention")
                if (attention.isEmpty()) {
                    Text(
                        text = "Nothing blocked — all releases are moving.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                attention.forEach { item ->
                    AttentionCard(item = item, onClick = { onOpenRelease(item.releaseId) })
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Upcoming", modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenApps) { Text("All apps") }
                }
                if (upcoming.isEmpty()) {
                    Text(
                        text = "No planned releases.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                upcoming.take(3).forEach { release ->
                    ReleaseCard(release = release, onClick = { onOpenRelease(release.id) })
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Recent activity", modifier = Modifier.weight(1f))
                    TextButton(onClick = onOpenStats) { Text("Statistics") }
                }
                activity.forEach { item ->
                    ActivityRow(item = item, onClick = { onOpenRelease(item.releaseId) })
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttentionCard(item: AttentionItem, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (item.severity) {
        AttentionItem.Severity.Problem -> scheme.errorContainer to scheme.onErrorContainer
        AttentionItem.Severity.Waiting -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        AttentionItem.Severity.Upcoming -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.titleMedium)
            Text(text = item.detail, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
