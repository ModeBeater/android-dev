package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import kz.kbtu.releaseflow.ui.components.EmptyState

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
                action = { Button(onClick = { showAddDialog = true }) { Text("Add app") } },
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.apps, key = { it.id }) { app ->
                    AppCard(app = app, onClick = { onOpenApp(app.id) })
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showAddDialog = true },
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
            text = { Text("New app") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
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
