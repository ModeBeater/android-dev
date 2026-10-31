package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.util.durationText
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.ActivityRow
import kz.kbtu.releaseflow.ui.components.AttentionRow
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.GroupedCard
import kz.kbtu.releaseflow.ui.components.KpiTile
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.ReleaseCard
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * Overview: what is in flight, what needs a decision, what is due soon, what just happened.
 * Every number is a derived getter of [ReleaseUiState].
 */
@Composable
fun DashboardScreen(
    state: ReleaseUiState,
    onOpenRelease: (Int) -> Unit,
    onOpenApp: (Int) -> Unit,
    onOpenApps: () -> Unit,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    val attention = state.attention
    val upcoming = state.upcoming
    val activity = state.recentActivity
    val stats = state.stats

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiTile(
                    label = "In review",
                    value = state.tracksInReview.toString(),
                    caption = "waiting for the stores",
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    label = "Rolling out",
                    value = state.tracksRollingOut.toString(),
                    caption = "staged on Google Play",
                    modifier = Modifier.weight(1f),
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiTile(
                    label = "Needs a decision",
                    value = state.problems.toString(),
                    caption = if (state.problems == 0) "no rejections or halts" else "rejected or halted",
                    note = if (state.problems > 0) "action" else null,
                    noteColor = colors.danger,
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    label = "Live, last 30 days",
                    value = state.liveLast30Days.toString(),
                    caption = stats?.averageReviewMs?.values?.filterNotNull()?.minOrNull()
                        ?.let { "fastest review ${it.durationText()}" } ?: "no reviews yet",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStats,
                )
            }
        }

        item {
            Column {
                SectionHeader(title = "Needs attention", count = attention.size.takeIf { it > 0 })
                if (attention.isEmpty()) {
                    FlatCard {
                        Text(
                            text = "Nothing blocked — all releases are moving.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                } else {
                    GroupedCard(items = attention) { item ->
                        AttentionRow(
                            item = item,
                            onClick = {
                                val releaseId = item.releaseId
                                val appId = item.appId
                                if (releaseId != null) onOpenRelease(releaseId) else if (appId != null) onOpenApp(appId)
                            },
                        )
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Upcoming", actionLabel = "All apps", onAction = onOpenApps)
            if (upcoming.isEmpty()) {
                FlatCard {
                    Text(text = "No planned releases.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }
        }
        items(upcoming.take(3).size) { index ->
            val release = upcoming[index]
            ReleaseCard(release = release, onClick = { onOpenRelease(release.id) })
        }

        item {
            Column {
                SectionHeader(title = "Recent activity", actionLabel = "Statistics", onAction = onOpenStats)
                if (activity.isEmpty()) {
                    FlatCard {
                        Text(text = "No events yet.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                } else {
                    GroupedCard(items = activity) { item -> ActivityRow(item = item, onClick = { onOpenRelease(item.releaseId) }) }
                }
            }
        }
    }
}
