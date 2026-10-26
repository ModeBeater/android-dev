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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.domain.util.durationText
import kz.kbtu.releaseflow.domain.util.monthLabel
import kz.kbtu.releaseflow.domain.util.percentText
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.BarRow
import kz.kbtu.releaseflow.ui.components.PlatformChip
import kz.kbtu.releaseflow.ui.components.SectionTitle
import kz.kbtu.releaseflow.ui.components.StatCard

/** Statistics computed from the stage histories: review times, approval rate, rejections, cadence. */
@Composable
fun StatsScreen(
    state: ReleaseUiState,
    modifier: Modifier = Modifier,
) {
    val stats = state.stats ?: return

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Platform.entries.forEach { platform ->
                    StatCard(
                        title = "${platform.store} review",
                        value = stats.averageReviewMs[platform]?.durationText() ?: "—",
                        hint = "average per cycle",
                        modifier = Modifier.weight(1f),
                        containerColor = if (platform == Platform.IOS) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (platform == Platform.IOS) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(
                    title = "Approval rate",
                    value = stats.approvalRate?.percentText() ?: "—",
                    hint = "${stats.reviewCycles} review cycles",
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    title = "Longest review",
                    value = stats.longestReviewMs?.durationText() ?: "—",
                    hint = "${stats.liveReleases} releases live",
                    modifier = Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        item {
            Column {
                SectionTitle("Releases per month")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val max = stats.releasesPerMonth.maxOfOrNull { it.second } ?: 1
                        if (stats.releasesPerMonth.isEmpty()) {
                            Text(text = "No releases yet", style = MaterialTheme.typography.bodyMedium)
                        }
                        stats.releasesPerMonth.forEach { (month, count) ->
                            BarRow(label = monthLabel(month), valueText = "$count", fraction = count.toFloat() / max)
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionTitle("Rejections")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        if (stats.rejections.isEmpty()) {
                            Text(
                                text = "No rejections — keep it that way.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        stats.rejections.forEachIndexed { index, rejection ->
                            if (index > 0) HorizontalDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${rejection.appName} ${rejection.version}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = rejection.reason + (rejection.guideline?.let { " · $it" } ?: ""),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    PlatformChip(platform = rejection.platform)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rejection.at.dayText(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionTitle("Rejection reasons by guideline")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        val byGuideline = stats.rejections
                            .groupingBy { it.guideline ?: "No guideline cited" }
                            .eachCount()
                            .entries
                            .sortedByDescending { it.value }
                        val max = byGuideline.maxOfOrNull { it.value } ?: 1
                        if (byGuideline.isEmpty()) {
                            Text(text = "Nothing to show", style = MaterialTheme.typography.bodyMedium)
                        }
                        byGuideline.forEach { (guideline, count) ->
                            BarRow(label = guideline, valueText = "$count", fraction = count.toFloat() / max)
                        }
                    }
                }
            }
        }
    }
}
