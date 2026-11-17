package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/** House-styled AlertDialog: flat surface, radius 24, accent text buttons. */
@Composable
private fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = ReleaseFlowTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary) },
        text = content,
        confirmButton = { LinkButton(text = confirmLabel, onClick = onConfirm, enabled = confirmEnabled) },
        dismissButton = { LinkButton(text = "Cancel", onClick = onDismiss) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

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
    val showError = error != null && text.isNotEmpty()

    AppDialog(title = title, onDismiss = onDismiss, confirmLabel = confirmLabel, onConfirm = { onConfirm(text) }, confirmEnabled = error == null) {
        AppTextField(
            value = text,
            onValueChange = { text = it },
            label = label,
            isError = showError,
            supportingText = if (showError) error else supportingText,
            containerColor = ReleaseFlowTheme.colors.surfaceSunken,
        )
    }
}

/** Yes/no question for a destructive action; the confirm button reads in the danger colour. */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReleaseFlowTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary) },
        text = { Text(text = text, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.danger),
            ) { Text(text = confirmLabel, style = MaterialTheme.typography.labelMedium) }
        },
        dismissButton = { LinkButton(text = "Cancel", onClick = onDismiss) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
    )
}

/** Slider from the current percentage up to 100; 100 % means "go fully live". */
@Composable
fun RolloutDialog(
    currentPercent: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReleaseFlowTheme.colors
    var value by rememberSaveable { mutableStateOf(minOf(currentPercent + 10, 100).toFloat()) }
    val target = value.toInt()

    AppDialog(
        title = "Staged rollout",
        onDismiss = onDismiss,
        confirmLabel = if (target == 100) "Go live" else "Apply",
        onConfirm = { onConfirm(target) },
    ) {
        Column {
            Text(
                text = "Currently $currentPercent % of users. Move to $target %" +
                    (if (target == 100) " — this makes the release live for everyone." else "."),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = value,
                onValueChange = { value = it },
                valueRange = (currentPercent + 1).toFloat()..100f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = colors.accent,
                    activeTrackColor = colors.accent,
                    inactiveTrackColor = colors.surfaceSunken,
                ),
            )
        }
    }
}

/** Choose which platform a new checklist item applies to. */
@Composable
fun PlatformPicker(
    selected: Platform?,
    onSelect: (Platform?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options: List<Platform?> = listOf(null, Platform.IOS, Platform.ANDROID)
    SegmentedPills(
        options = options,
        selected = selected,
        label = { it?.label ?: "Both" },
        onSelect = onSelect,
        modifier = modifier,
    )
}
