package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kz.kbtu.releaseflow.domain.model.GitHubRelease
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.ui.AppViewModelProvider
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AppPill
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.LinkButton
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.SecondaryButton
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * Releases of the app's GitHub repository, straight from the REST API: a loading state, an
 * error state with retry, an empty state and the list — with pull-to-refresh. A release whose
 * version is already tracked links to it; any other can be imported with one tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitHubReleasesScreen(
    state: ReleaseUiState,
    onOpenRelease: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GitHubReleasesViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = ReleaseFlowTheme.colors
    val ui by viewModel.uiState.collectAsStateWithLifecycle()
    val app = state.app(viewModel.appId)

    LaunchedEffect(ui.message?.id) {
        val message = ui.message ?: return@LaunchedEffect
        delay(3_000)
        viewModel.onMessageShown(message.id)
    }

    PullToRefreshBox(
        isRefreshing = ui.isRefreshing,
        onRefresh = viewModel::refresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val list = ui.list) {
            GitHubListState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp, color = colors.accent)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "Asking GitHub…", style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                }
            }

            is GitHubListState.Error -> EmptyState(
                icon = Icons.Default.Warning,
                title = "Could not load releases",
                subtitle = list.message,
                action = { PrimaryButton(text = "Retry", onClick = viewModel::load) },
            )

            GitHubListState.NoRepository -> EmptyState(
                icon = Icons.Default.Share,
                title = "No repository linked",
                subtitle = "Link a GitHub repository on the app screen to import its releases.",
            )

            is GitHubListState.Success -> if (list.releases.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Share,
                    title = "No releases",
                    subtitle = "${ui.repo} has no published releases on GitHub.",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PageListPadding,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        SectionHeader(title = ui.repo ?: "GitHub", count = list.releases.size)
                        ui.message?.let { AppPill(label = it.text, color = colors.success, modifier = Modifier.padding(bottom = 4.dp)) }
                    }
                    items(list.releases, key = { it.id }) { release ->
                        val version = release.version
                        val trackedId = version?.let { v -> app?.releases?.firstOrNull { it.version == v }?.id }
                        GitHubReleaseCard(
                            release = release,
                            trackedId = trackedId,
                            importing = release.id in ui.tracking,
                            onTrack = { viewModel.track(release) },
                            onOpenTracked = { trackedId?.let(onOpenRelease) },
                        )
                    }
                    item {
                        Text(
                            text = "Data: GitHub REST API · pull down to refresh",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One GitHub release: tag in mono, name, date and author, pre-release / draft pills, assets, the notes and the action. */
@Composable
private fun GitHubReleaseCard(
    release: GitHubRelease,
    trackedId: Int?,
    importing: Boolean,
    onTrack: () -> Unit,
    onOpenTracked: () -> Unit,
) {
    val colors = ReleaseFlowTheme.colors
    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = release.tag, style = MonoText.large, color = colors.textPrimary)
                    if (release.prerelease) AppPill(label = "pre-release", color = colors.warning, dense = true)
                    if (release.draft) AppPill(label = "draft", color = colors.textSecondary, dense = true)
                }
                release.name?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = release.publishedAt?.dayText() ?: "unpublished",
                    style = MonoText.small,
                    color = colors.textTertiary,
                )
                release.author?.let {
                    Text(text = "by $it", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                }
            }
        }

        val body = release.body
        if (!body.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (release.assetCount == 0) "no assets" else "${release.assetCount} assets · ${"%,d".format(release.downloads)} downloads",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                modifier = Modifier.weight(1f),
            )
            when {
                trackedId != null -> LinkButton(text = "Tracked · open", onClick = onOpenTracked)
                release.version == null -> Text(text = "no version in tag", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                importing -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                else -> SecondaryButton(text = "Track ${release.version}", onClick = onTrack, modifier = Modifier.height(40.dp))
            }
        }
    }
}
