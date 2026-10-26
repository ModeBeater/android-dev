package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.domain.model.Platform

/**
 * Dialog with one text field and validation. `validate` returns an error or `null`;
 * the typed text is `rememberSaveable`, so rotation keeps it.
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    confirmLabel: String,
    validate: (String) -> String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialValue: String = "",
    supportingText: String? = null,
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    val error = validate(text)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                isError = error != null && text.isNotEmpty(),
                supportingText = { Text(if (text.isNotEmpty() && error != null) error else supportingText ?: "") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = error == null) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Three fields for a new app; the ids are derived from the name but can be edited. */
@Composable
fun AddAppDialog(
    onConfirm: (name: String, bundleId: String, packageName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var bundleId by rememberSaveable { mutableStateOf("") }
    var packageName by rememberSaveable { mutableStateOf("") }
    var idsEdited by rememberSaveable { mutableStateOf(false) }

    fun suggestId(appName: String): String =
        "kz." + appName.trim().lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(" ", ".")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New app") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (!idsEdited) {
                            bundleId = suggestId(it)
                            packageName = suggestId(it)
                        }
                    },
                    label = { Text("App name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = bundleId,
                    onValueChange = { bundleId = it; idsEdited = true },
                    label = { Text("iOS bundle id") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it; idsEdited = true },
                    label = { Text("Android package") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, bundleId, packageName) },
                enabled = name.trim().length >= 2,
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Slider from the current percentage up to 100; 100 % means "go fully live". */
@Composable
fun RolloutDialog(
    currentPercent: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(minOf(currentPercent + 10, 100).toFloat()) }
    val target = value.toInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Staged rollout") },
        text = {
            Column {
                Text(
                    text = "Currently $currentPercent % of users. Move to $target %" +
                        (if (target == 100) " — this makes the release live for everyone." else "."),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = (currentPercent + 1).toFloat()..100f,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(target) }) { Text(if (target == 100) "Go live" else "Apply") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Choose which platform a new checklist item applies to. */
@Composable
fun PlatformPicker(
    selected: Platform?,
    onSelect: (Platform?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<Platform?> = listOf(null, Platform.IOS, Platform.ANDROID)
    ChoiceChipRow(
        options = options,
        selected = selected,
        label = { it?.label ?: "Both" },
        onSelect = onSelect,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
    )
}
