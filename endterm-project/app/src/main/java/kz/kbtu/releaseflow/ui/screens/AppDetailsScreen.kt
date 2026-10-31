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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kz.kbtu.releaseflow.ui.AppViewModelProvider
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AppFab
import kz.kbtu.releaseflow.ui.components.AppPill
import kz.kbtu.releaseflow.ui.components.ConfirmDialog
import kz.kbtu.releaseflow.ui.components.DangerButton
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.Hairline
import kz.kbtu.releaseflow.ui.components.KeyValueRow
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.ReleaseCard
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.components.StoreArtwork
import kz.kbtu.releaseflow.ui.components.StoreEmptyCard
import kz.kbtu.releaseflow.ui.components.StoreListingCard
import kz.kbtu.releaseflow.ui.components.TextInputDialog
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * One app: identity, the live App Store listing (REST, cached in Room), the GitHub link,
 * its releases newest first, and the two destructive-or-creative actions (delete, new
 * release). Data comes from the shared state; network actions from [AppDetailsViewModel].
 */
@Composable
fun AppDetailsScreen(
    state: ReleaseUiState,
    appId: Int,
    onOpenRelease: (Int) -> Unit,
    onNewRelease: () -> Unit,
    onOpenGithub: () -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppDetailsViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = ReleaseFlowTheme.colors
    val local by viewModel.uiState.collectAsStateWithLifecycle()
    val app = state.app(appId)

    LaunchedEffect(local.deleted) {
        if (local.deleted) onDeleted()
    }

    if (app == null) {
        if (state.loaded && !local.deleted) {
            EmptyState(
                icon = Icons.Default.Warning,
                title = "App not found",
                subtitle = "App #$appId does not exist.",
                modifier = modifier,
            )
        }
        return
    }

    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    val live = app.releases.count { it.isLiveEverywhere }
    val inFlight = app.releases.size - live
    val refreshing = local.storeRefresh == StoreRefreshState.Loading
    val refreshError = (local.storeRefresh as? StoreRefreshState.Error)?.message

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PageListPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FlatCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoreArtwork(url = app.listing?.artworkUrl, monogram = app.monogram, size = 56)
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
                    local.message?.let { message ->
                        Spacer(modifier = Modifier.height(10.dp))
                        AppPill(label = message.text, color = colors.info)
                        LaunchedEffect(message.id) {
                            delay(4_000)
                            viewModel.onMessageShown(message.id)
                        }
                    }
                }
            }

            item {
                val listing = app.listing
                if (listing != null) {
                    StoreListingCard(
                        listing = listing,
                        comparison = app.storeComparison,
                        now = state.now,
                        refreshing = refreshing,
                        error = refreshError,
                        onRefresh = viewModel::refreshStore,
                    )
                } else {
                    StoreEmptyCard(
                        bundleId = app.bundleId,
                        refreshing = refreshing,
                        error = refreshError,
                        onRefresh = viewModel::refreshStore,
                    )
                }
            }

            item {
                FlatCard(padding = PaddingValues(0.dp)) {
                    KeyValueRow(label = "App Store", caption = "bundle id", value = app.bundleId, mono = true, valueColor = colors.textSecondary)
                    Hairline()
                    KeyValueRow(label = "Google Play", caption = "package name", value = app.packageName, mono = true, valueColor = colors.textSecondary)
                    Hairline()
                    if (app.githubRepo != null) {
                        KeyValueRow(
                            label = "GitHub releases",
                            caption = app.githubRepo,
                            icon = Icons.Default.Share,
                            iconTint = colors.textSecondary,
                            onClick = onOpenGithub,
                        )
                    } else {
                        KeyValueRow(
                            label = "GitHub",
                            caption = "No repository linked",
                            value = "Link",
                            valueColor = colors.accent,
                            icon = Icons.Default.Share,
                            iconTint = colors.textSecondary,
                            onClick = { showLinkDialog = true },
                        )
                    }
                }
            }

            item { SectionHeader(title = "Releases", count = app.releases.size.takeIf { it > 0 }) }

            if (app.releases.isEmpty()) {
                item {
                    FlatCard {
                        Text(
                            text = "No releases yet — create the first one, or import one from GitHub.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                        )
                    }
                }
            }
            items(app.releases, key = { it.id }) { release ->
                ReleaseCard(release = release, onClick = { onOpenRelease(release.id) }, showAppName = false)
            }

            item {
                Column {
                    SectionHeader(title = "Danger zone")
                    FlatCard {
                        Text(
                            text = "Deleting the app removes its releases, histories and checklists from the database (ON DELETE CASCADE).",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DangerButton(text = "Delete app", onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth())
                    }
                }
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

    if (showLinkDialog) {
        TextInputDialog(
            title = "Link GitHub repository",
            label = "owner/name",
            confirmLabel = "Link",
            validate = { if (it.trim().removePrefix("https://github.com/").trim('/').matches(Regex("""[\w.-]+/[\w.-]+"""))) null else "Use the form owner/name" },
            onConfirm = { repo ->
                showLinkDialog = false
                viewModel.linkGithub(repo)
            },
            onDismiss = { showLinkDialog = false },
            supportingText = "Public repositories only, e.g. signalapp/Signal-Android",
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete ${app.name}?",
            text = "This removes the app and its ${app.releases.size} releases. There is no undo.",
            confirmLabel = "Delete",
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteApp()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}
