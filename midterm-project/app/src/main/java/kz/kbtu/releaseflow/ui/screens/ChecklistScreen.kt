package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.ChecklistProgress
import kz.kbtu.releaseflow.ui.components.ChecklistRow
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.PlatformPicker
import kz.kbtu.releaseflow.ui.components.SectionTitle
import kz.kbtu.releaseflow.ui.components.TextInputDialog

/**
 * Submission checklist of one release, grouped: common items, iOS, Android.
 * Items with a guideline decide the outcome of the (simulated) store review.
 */
@Composable
fun ChecklistScreen(
    state: ReleaseUiState,
    releaseId: Int,
    onToggle: (Int) -> Unit,
    onAdd: (title: String, platform: Platform?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val release = state.release(releaseId)
    if (release == null) {
        EmptyState(icon = Icons.Default.Warning, title = "Release not found", subtitle = "Release #$releaseId does not exist.", modifier = modifier)
        return
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var newItemPlatform by rememberSaveable { mutableStateOf("") }   // "" = both

    val groups = listOf<Pair<String, Platform?>>(
        "Both platforms" to null,
        "iOS — App Store" to Platform.IOS,
        "Android — Google Play" to Platform.ANDROID,
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "${release.appName} ${release.version}", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    ChecklistProgress(done = release.checklistDone, total = release.checklistTotal)
                    Text(
                        text = "Items marked \"Reviewer checks\" are what the store rejects a build for. Complete them before submitting.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        groups.forEach { (title, platform) ->
            val items = release.checklist.filter { it.platform == platform }
            if (items.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            SectionTitle(title)
                            items.forEachIndexed { index, checklistItem ->
                                if (index > 0) HorizontalDivider()
                                ChecklistRow(item = checklistItem, onToggle = { onToggle(checklistItem.id) })
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionTitle("Add an item")
                PlatformPicker(
                    selected = Platform.entries.firstOrNull { it.name == newItemPlatform },
                    onSelect = { newItemPlatform = it?.name ?: "" },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Add checklist item")
                }
            }
        }
    }

    if (showAddDialog) {
        TextInputDialog(
            title = "New checklist item",
            label = "What has to be done",
            confirmLabel = "Add",
            validate = { if (it.trim().length < 3) "At least 3 characters" else null },
            onConfirm = { title ->
                showAddDialog = false
                onAdd(title, Platform.entries.firstOrNull { it.name == newItemPlatform })
            },
            onDismiss = { showAddDialog = false },
        )
    }
}
