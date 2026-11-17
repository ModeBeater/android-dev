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
 * Edit an existing app. Every field validates inline; the repository adds the checks that
 * need the database (unique name) and reports them under the button. `onSaved` fires once
 * the row is updated — the details screen behind this one already shows the new values,
 * because it observes the same Room Flow.
 */
@Composable
fun EditAppScreen(
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAppViewModel = viewModel(factory = AppViewModelProvider.Factory),
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
                Text(text = "Identity", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "App name",
                    enabled = state.loaded,
                    isError = state.nameError != null && state.name.isNotEmpty(),
                    supportingText = state.nameError?.takeIf { state.name.isNotEmpty() } ?: "Shown everywhere in the app",
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.bundleId,
                    onValueChange = viewModel::onBundleIdChange,
                    label = "iOS bundle id",
                    enabled = state.loaded,
                    isError = state.bundleIdError != null,
                    supportingText = state.bundleIdError ?: "Identifier in the App Store",
                    textStyle = MonoText.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.packageName,
                    onValueChange = viewModel::onPackageNameChange,
                    label = "Android package",
                    enabled = state.loaded,
                    isError = state.packageNameError != null,
                    supportingText = state.packageNameError,
                    textStyle = MonoText.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
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
