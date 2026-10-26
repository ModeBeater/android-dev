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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import kz.kbtu.releaseflow.ui.components.EmptyState
import kz.kbtu.releaseflow.ui.components.PlatformChip
import kz.kbtu.releaseflow.ui.components.SectionTitle
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("${app.name} — new release")
                    OutlinedTextField(
                        value = version,
                        onValueChange = { version = it },
                        label = { Text("Version") },
                        singleLine = true,
                        isError = submitted && versionError != null,
                        supportingText = { Text(if (submitted && versionError != null) versionError else "Suggested from the latest release") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = plannedDate,
                        onValueChange = { plannedDate = it },
                        label = { Text("Planned date (optional)") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        isError = submitted && dateError != null,
                        supportingText = { Text(if (submitted && dateError != null) dateError else "Shown on the dashboard as upcoming") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("What is in this release") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    SectionTitle("Platforms")
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        text = "A submission checklist for the chosen platforms is created automatically.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }

        item {
            Button(
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
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create release")
            }
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
        Checkbox(checked = enabled, onCheckedChange = onEnabledChange)
        PlatformChip(platform = platform)
        Spacer(modifier = Modifier.width(12.dp))
        OutlinedTextField(
            value = build,
            onValueChange = onBuildChange,
            label = { Text("${platform.label} build number") },
            placeholder = { Text("auto") },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
