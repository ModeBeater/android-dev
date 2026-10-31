package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kz.kbtu.releaseflow.ui.AppViewModelProvider
import kz.kbtu.releaseflow.ui.components.AppPill
import kz.kbtu.releaseflow.ui.components.AppTextField
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.components.SecondaryButton
import kz.kbtu.releaseflow.ui.components.StoreArtwork
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/**
 * "New app" form with a live App Store lookup: type the bundle id, tap "Look up", and the
 * iTunes API fills the name and shows the listing. The whole form is ViewModel state so the
 * lookup result survives rotation; `onCreated` fires once the app is in the database.
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
                Text(text = "App Store", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = state.bundleId,
                    onValueChange = viewModel::onBundleIdChange,
                    label = "iOS bundle id",
                    placeholder = "ph.telegra.Telegraph",
                    supportingText = "Looked up through the public iTunes Search API",
                    textStyle = MonoText.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SecondaryButton(
                        text = if (state.lookup == LookupState.Loading) "Looking up…" else "Look up",
                        onClick = viewModel::lookUp,
                        enabled = state.bundleId.isNotBlank() && state.lookup != LookupState.Loading,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    LookupStatus(state.lookup)
                }
                val listing = (state.lookup as? LookupState.Found)?.listing
                if (listing != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StoreArtwork(url = listing.artworkUrl, monogram = listing.name.take(2).uppercase(), size = 56)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = listing.name, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(
                                text = listOfNotNull(listing.sellerName, "version ${listing.version}").joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                            )
                        }
                        listing.rating?.let { AppPill(label = "★ %.1f".format(it), color = colors.warning, dense = true) }
                    }
                }
            }
        }

        item {
            FlatCard {
                Text(text = "Details", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "App name",
                    supportingText = if (state.name.isNotEmpty() && state.name.trim().length < 2) "At least 2 characters" else null,
                    isError = state.name.isNotEmpty() && state.name.trim().length < 2,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.packageName,
                    onValueChange = viewModel::onPackageNameChange,
                    label = "Android package",
                    textStyle = MonoText.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = state.githubRepo,
                    onValueChange = viewModel::onGithubRepoChange,
                    label = "GitHub repository (optional)",
                    placeholder = "owner/name",
                    supportingText = "Lets you import releases from GitHub",
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

        // Validation errors from the repository ("name already exists"), shown under the button.
        val message = state.message
        if (message != null) {
            item {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.danger,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }

    LaunchedEffect(state.message?.id) {
        val message = state.message ?: return@LaunchedEffect
        delay(4_000)
        viewModel.onMessageShown(message.id)
    }
}

/** The lookup's outcome next to the button: spinner, green "found", grey "not found" or a red error. */
@Composable
private fun LookupStatus(lookup: LookupState) {
    val colors = ReleaseFlowTheme.colors
    when (lookup) {
        LookupState.Idle -> Unit
        LookupState.Loading -> CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
        is LookupState.Found -> AppPill(label = "Found on the App Store", color = colors.success, dense = true)
        LookupState.NotFound -> AppPill(label = "Not on the App Store", color = colors.textSecondary, dense = true)
        is LookupState.Error -> AppPill(label = lookup.message, color = colors.danger, dense = true)
    }
}
