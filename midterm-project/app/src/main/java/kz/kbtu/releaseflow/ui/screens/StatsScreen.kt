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
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.domain.util.durationText
import kz.kbtu.releaseflow.domain.util.monthLabel
import kz.kbtu.releaseflow.domain.util.percentText
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.BarRow
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.GroupedCard
import kz.kbtu.releaseflow.ui.components.KeyValueRow
import kz.kbtu.releaseflow.ui.components.KpiTile
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PlatformChip
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/** Statistics computed from the stage histories: review times, approval rate, rejections, cadence. */
@Composable
fun StatsScreen(
    state: ReleaseUiState,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    val stats = state.stats ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Platform.entries.forEach { platform ->
                    KpiTile(
                        label = "${platform.store} review",
                        value = stats.averageReviewMs[platform]?.durationText() ?: "—",
                        caption = "average per cycle",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KpiTile(
                    label = "Approval rate",
                    value = stats.approvalRate?.percentText() ?: "—",
                    caption = "${stats.reviewCycles} review cycles",
                    modifier = Modifier.weight(1f),
                )
                KpiTile(
                    label = "Longest review",
                    value = stats.longestReviewMs?.durationText() ?: "—",
                    caption = "${stats.liveReleases} releases live",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Column {
                SectionHeader(title = "Releases per month")
                FlatCard {
                    val max = stats.releasesPerMonth.maxOfOrNull { it.second } ?: 1
                    if (stats.releasesPerMonth.isEmpty()) {
                        Text(text = "No releases yet", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        stats.releasesPerMonth.forEach { (month, count) ->
                            BarRow(label = monthLabel(month), valueText = "$count", fraction = count.toFloat() / max)
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "Rejections", count = stats.rejections.size.takeIf { it > 0 })
                if (stats.rejections.isEmpty()) {
                    FlatCard {
                        Text(text = "No rejections — keep it that way.", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                } else {
                    GroupedCard(items = stats.rejections) { rejection ->
                        KeyValueRow(
                            label = "${rejection.appName} ${rejection.version}",
                            caption = rejection.reason + (rejection.guideline?.let { " · $it" } ?: ""),
                            value = rejection.at.dayText(),
                            valueColor = colors.textTertiary,
                            mono = true,
                            trailing = { PlatformChip(platform = rejection.platform, dense = true) },
                        )
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "Rejection reasons by guideline")
                FlatCard {
                    val byGuideline = stats.rejections
                        .groupingBy { it.guideline ?: "No guideline cited" }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                    val max = byGuideline.maxOfOrNull { it.value } ?: 1
                    if (byGuideline.isEmpty()) {
                        Text(text = "Nothing to show", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        byGuideline.forEach { (guideline, count) ->
                            BarRow(label = guideline, valueText = "$count", fraction = count.toFloat() / max, color = colors.danger)
                        }
                    }
                }
            }
        }
    }
}
