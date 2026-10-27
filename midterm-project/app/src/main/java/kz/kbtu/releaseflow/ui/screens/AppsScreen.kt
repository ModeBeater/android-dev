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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AddAppDialog
import kz.kbtu.releaseflow.ui.components.AppCard
import kz.kbtu.releaseflow.ui.components.AppFab
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.SectionHeader

/**
 * Apps tab: every app with its latest release, plus a FAB that opens the "new app" dialog.
 * `onAddApp` returns the new id (or null when the ViewModel rejected the input) so the
 * screen can navigate straight to it.
 */
@Composable
fun AppsScreen(
    state: ReleaseUiState,
    onOpenApp: (Int) -> Unit,
    onAddApp: (name: String, bundleId: String, packageName: String) -> Int?,
    modifier: Modifier = Modifier,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        if (state.apps.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Filled.List,
                title = "No apps yet",
                subtitle = "Add the apps your team ships to start tracking releases.",
                action = { PrimaryButton(text = "Add app", onClick = { showAddDialog = true }) },
            )
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
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 92.dp),
        )
    }

    if (showAddDialog) {
        AddAppDialog(
            onConfirm = { name, bundleId, packageName ->
                val id = onAddApp(name, bundleId, packageName)
                if (id != null) {
                    showAddDialog = false
                    onOpenApp(id)
                }
            },
            onDismiss = { showAddDialog = false },
        )
    }
}
