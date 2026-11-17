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
 * "New app" form. The store ids are suggested from the name until the user edits them;
 * every field validates inline and the repository reports database-level problems (a
 * duplicate name) under the button. `onCreated` fires once the row is in the database.
 */
@Composable
fun AddAppScreen(
    onCreated: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddAppViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val colors = ReleaseFlowTheme.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.createdAppId) {
        state.createdAppId?.let(onCreated)
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
                    isError = state.nameError != null && state.name.isNotEmpty(),
                    supportingText = state.nameError?.takeIf { state.name.isNotEmpty() } ?: "Must be unique",
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.bundleId,
                    onValueChange = viewModel::onBundleIdChange,
                    label = "iOS bundle id",
                    isError = state.bundleIdError != null,
                    supportingText = state.bundleIdError ?: "Suggested from the name — edit if it differs",
                    textStyle = MonoText.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.packageName,
                    onValueChange = viewModel::onPackageNameChange,
                    label = "Android package",
                    isError = state.packageNameError != null,
                    supportingText = state.packageNameError,
                    textStyle = MonoText.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }
        }

        item {
            PrimaryButton(
                text = if (state.saving) "Saving…" else "Add app",
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
