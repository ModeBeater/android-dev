package kz.kbtu.releaseflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.util.toEpochMillis
import kz.kbtu.releaseflow.ui.ReleaseUiState
import kz.kbtu.releaseflow.ui.components.AppCheckbox
import kz.kbtu.releaseflow.ui.components.AppTextField
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.FlatCard
import kz.kbtu.releaseflow.ui.components.PageListPadding
import kz.kbtu.releaseflow.ui.components.PlatformChip
import kz.kbtu.releaseflow.ui.components.PrimaryButton
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Form for a new release. Everything typed here is local `rememberSaveable` state (it is a
 * draft, not application state) and becomes a domain object only on "Create".
 * `onCreate` returns the new release id, or null when validation in the ViewModel failed.
 */
@Composable
fun NewReleaseScreen(
    state: ReleaseUiState,
    appId: Int,
    onCreate: (version: String, builds: Map<Platform, String>, plannedAt: Long?, notes: String) -> Int?,
    onCreated: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    val app = state.app(appId)
    if (app == null) {
        EmptyState(icon = Icons.Default.Warning, title = "App not found", subtitle = "App #$appId does not exist.", modifier = modifier)
        return
    }

    val suggested = app.latest?.version?.let { bumpMinor(it) } ?: "1.0.0"
    var version by rememberSaveable { mutableStateOf(suggested) }
    var iosEnabled by rememberSaveable { mutableStateOf(true) }
    var androidEnabled by rememberSaveable { mutableStateOf(true) }
    var iosBuild by rememberSaveable { mutableStateOf("") }
    var androidBuild by rememberSaveable { mutableStateOf("") }
    var plannedDate by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val versionError = if (!version.matches(Regex("""\d+\.\d+(\.\d+)?"""))) "Use 1.2 or 1.2.3" else null
    val dateError = if (plannedDate.isNotBlank() && parseDate(plannedDate) == null) "Use YYYY-MM-DD" else null
    val platformsError = if (!iosEnabled && !androidEnabled) "Choose at least one platform" else null
    val canSubmit = versionError == null && dateError == null && platformsError == null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PageListPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            FlatCard {
                Text(text = "Release", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = "Version",
                    isError = submitted && versionError != null,
                    supportingText = if (submitted && versionError != null) versionError else "Suggested from the latest release",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = MonoText.large,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = plannedDate,
                    onValueChange = { plannedDate = it },
                    label = "Planned date (optional)",
                    placeholder = "YYYY-MM-DD",
                    isError = submitted && dateError != null,
                    supportingText = if (submitted && dateError != null) dateError else "Shown on the overview as upcoming",
                    textStyle = MonoText.large,
                )
                Spacer(modifier = Modifier.height(4.dp))
                AppTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "What is in this release",
                    singleLine = false,
                    minLines = 2,
                )
            }
        }

        item {
            FlatCard {
                Text(text = "Platforms", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                PlatformRow(
                    platform = Platform.IOS,
                    enabled = iosEnabled,
                    build = iosBuild,
                    onEnabledChange = { iosEnabled = it },
                    onBuildChange = { iosBuild = it },
                )
                Spacer(modifier = Modifier.height(8.dp))
                PlatformRow(
                    platform = Platform.ANDROID,
                    enabled = androidEnabled,
                    build = androidBuild,
                    onEnabledChange = { androidEnabled = it },
                    onBuildChange = { androidBuild = it },
                )
                if (submitted && platformsError != null) {
                    Text(
                        text = platformsError,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.danger,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Text(
                    text = "A submission checklist for the chosen platforms is created automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        item {
            PrimaryButton(
                text = "Create release",
                onClick = {
                    submitted = true
                    if (canSubmit) {
                        val builds = buildMap<Platform, String> {
                            if (iosEnabled) put(Platform.IOS, iosBuild.ifBlank { version.replace(".", "") })
                            if (androidEnabled) put(Platform.ANDROID, androidBuild.ifBlank { version.replace(".", "") + "0" })
                        }
                        val planned = plannedDate.takeIf { it.isNotBlank() }?.let { parseDate(it)?.toEpochMillis() }
                        val id = onCreate(version.trim(), builds, planned, notes)
                        if (id != null) onCreated(id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PlatformRow(
    platform: Platform,
    enabled: Boolean,
    build: String,
    onEnabledChange: (Boolean) -> Unit,
    onBuildChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppCheckbox(checked = enabled, onCheckedChange = onEnabledChange)
        PlatformChip(platform = platform)
        Spacer(modifier = Modifier.width(12.dp))
        AppTextField(
            value = build,
            onValueChange = onBuildChange,
            label = "Build number",
            placeholder = "auto",
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MonoText.large,
            modifier = Modifier.weight(1f),
        )
    }
}

/** "3.4.0" -> "3.5.0", "1.2" -> "1.3" */
private fun bumpMinor(version: String): String {
    val parts = version.split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size < 2) return version
    val bumped = parts.toMutableList().apply { this[1] = this[1] + 1; for (i in 2 until size) this[i] = 0 }
    return bumped.joinToString(".")
}

private fun parseDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim())
} catch (e: DateTimeParseException) {
    null
}
