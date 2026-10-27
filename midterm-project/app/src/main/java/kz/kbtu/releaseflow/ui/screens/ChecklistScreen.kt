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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.ChecklistProgress
import kz.kbtu.releaseflow.ui.components.ChecklistRow
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.Hairline
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PlatformPicker
import kz.kbtu.releaseflow.ui.components.SecondaryButton
import kz.kbtu.releaseflow.ui.components.SectionHeader
import kz.kbtu.releaseflow.ui.components.TextInputDialog
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

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
    val colors = ReleaseFlowTheme.colors
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
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FlatCard {
                ChecklistProgress(done = release.checklistDone, total = release.checklistTotal, label = "Completed")
                Text(
                    text = "Items marked “Reviewer checks” are what the store rejects a build for. Complete them before submitting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        groups.forEach { (title, platform) ->
            val items = release.checklist.filter { it.platform == platform }
            if (items.isNotEmpty()) {
                item {
                    Column {
                        SectionHeader(title = title, count = items.count { it.done }.let { done -> if (done == items.size) null else items.size - done })
                        FlatCard(padding = PaddingValues(vertical = 4.dp)) {
                            items.forEachIndexed { index, checklistItem ->
                                if (index > 0) Hairline(startIndent = 52)
                                ChecklistRow(item = checklistItem, onToggle = { onToggle(checklistItem.id) })
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                SectionHeader(title = "Add an item")
                FlatCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Applies to",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f),
                        )
                        PlatformPicker(
                            selected = Platform.entries.firstOrNull { it.name == newItemPlatform },
                            onSelect = { newItemPlatform = it?.name ?: "" },
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SecondaryButton(text = "Add checklist item", onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth())
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
