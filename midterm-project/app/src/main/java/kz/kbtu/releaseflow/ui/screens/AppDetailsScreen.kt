package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AppFab
import kz.kbtu.releaseflow.ui.components.AppMonogram
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.GroupedCard
import kz.kbtu.releaseflow.ui.components.KeyValueRow
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.ReleaseCard
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/** One app: identifiers and its releases, newest first; FAB starts a new release. */
@Composable
fun AppDetailsScreen(
    state: ReleaseUiState,
    appId: Int,
    onOpenRelease: (Int) -> Unit,
    onNewRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    val app = state.app(appId)
    if (app == null) {
        EmptyState(
            icon = Icons.Default.Warning,
            title = "App not found",
            subtitle = "App #$appId does not exist.",
            modifier = modifier,
        )
        return
    }

    val live = app.releases.count { it.isLiveEverywhere }
    val inFlight = app.releases.size - live

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PageListPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FlatCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppMonogram(monogram = app.monogram, large = true)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.name, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                            Text(
                                text = "${app.releases.size} releases · $live live · $inFlight in flight",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                    }
                }
            }

            item {
                GroupedCard(
                    items = listOf(
                        Triple("App Store", "bundle id", app.bundleId),
                        Triple("Google Play", "package name", app.packageName),
                    ),
                ) { (store, caption, id) ->
                    KeyValueRow(label = store, caption = caption, value = id, mono = true, valueColor = colors.textSecondary)
                }
            }

            item { SectionHeader(title = "Releases", count = app.releases.size.takeIf { it > 0 }) }

            if (app.releases.isEmpty()) {
                item {
                    FlatCard {
                        Text(
                            text = "No releases yet — create the first one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            items(app.releases, key = { it.id }) { release ->
                ReleaseCard(release = release, onClick = { onOpenRelease(release.id) }, showAppName = false)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        AppFab(
            text = "New release",
            icon = Icons.Default.Add,
            onClick = onNewRelease,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 20.dp),
        )
    }
}
