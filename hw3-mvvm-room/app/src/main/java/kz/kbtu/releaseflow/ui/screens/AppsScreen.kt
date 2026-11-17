package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AppCard
import kz.kbtu.releaseflow.ui.components.AppFab
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.SectionHeader

/** Apps tab: every app with its latest release and store status; the FAB opens the "new app" form. */
@Composable
fun AppsScreen(
    state: ReleaseUiState,
    onOpenApp: (Int) -> Unit,
    onAddApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.apps.isEmpty()) {
            if (state.loaded) {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "No apps yet",
                    subtitle = "Add the apps your team ships to start tracking releases.",
                    action = { PrimaryButton(text = "Add app", onClick = onAddApp) },
                )
            }
        } else {
            LazyColumn(
                contentPadding = PageListPadding,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SectionHeader(title = "Tracked apps", count = state.apps.size) }
                items(state.apps, key = { it.id }) { app ->
                    AppCard(app = app, onClick = { onOpenApp(app.id) })
                }
            }
        }

        // Sits above the floating tab bar.
        AppFab(
            text = "New app",
            icon = Icons.Default.Add,
            onClick = onAddApp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 92.dp),
        )
    }
}
