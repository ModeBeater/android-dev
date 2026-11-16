package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kbtu.releaseflow.ui.AppViewModelProvider
import kz.kbtu.releaseflow.ui.components.AppTextField
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * Edit a release's version, planned date and notes. Platform tracks and the stage history
 * are deliberately not editable here: they change only through the workflow actions.
 */
@Composable
fun EditReleaseScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditReleaseViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = ReleaseFlowTheme.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FlatCard {
                Text(text = state.appName.ifBlank { "Release" }, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = state.version,
                    onValueChange = viewModel::onVersionChange,
                    label = "Version",
                    enabled = state.loaded,
                    isError = state.versionError != null && state.version.isNotEmpty(),
                    supportingText = state.versionError?.takeIf { state.version.isNotEmpty() } ?: "Must stay unique within the app",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MonoText.large,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.plannedDate,
                    onValueChange = viewModel::onPlannedDateChange,
                    label = "Planned date (optional)",
                    placeholder = "YYYY-MM-DD",
                    enabled = state.loaded,
                    isError = state.dateError != null,
                    supportingText = state.dateError ?: "Shown on the overview as upcoming",
                    textStyle = MonoText.large,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = "What is in this release",
                    enabled = state.loaded,
                    singleLine = false,
                    minLines = 2,
                )
                Text(
                    text = "Tracks, build numbers and the stage history change only through the workflow actions on the release screen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        item {
            PrimaryButton(
                text = if (state.saving) "Saving…" else "Save changes",
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }

        val error = state.error
        if (error != null) {
            item {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
