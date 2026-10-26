package kz.kbtu.releaseflow.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AppMonogram
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.ReleaseCard
import kz.kbtu.releaseflow.ui.components.SectionTitle
import kz.kbtu.releaseflow.ui.components.SummaryRow

/** One app: identifiers and its releases, newest first; FAB starts a new release. */
@Composable
fun AppDetailsScreen(
    state: ReleaseUiState,
    appId: Int,
    onOpenRelease: (Int) -> Unit,
    onNewRelease: () -> Unit,
    modifier: Modifier = Modifier,
) {
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

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppMonogram(monogram = app.monogram, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = app.name, style = MaterialTheme.typography.titleLarge)
                            SummaryRow(label = "App Store", value = app.bundleId)
                            SummaryRow(label = "Google Play", value = app.packageName)
                            SummaryRow(label = "Releases", value = app.releases.size.toString())
                        }
                    }
                }
            }

            item { SectionTitle("Releases") }

            if (app.releases.isEmpty()) {
                item {
                    Text(
                        text = "No releases yet — create the first one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(app.releases, key = { it.id }) { release ->
                ReleaseCard(release = release, onClick = { onOpenRelease(release.id) }, showAppName = false)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        ExtendedFloatingActionButton(
            onClick = onNewRelease,
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            text = { Text("New release") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        )
    }
}
