package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

private val ButtonHeight = 50.dp
private val ButtonPadding = PaddingValues(horizontal = 20.dp)

/** The one accent-filled button of a screen: radius 24, no elevation, 50 dp tall. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ReleaseFlowTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.accent,
            contentColor = colors.onAccent,
            disabledContainerColor = colors.surfaceSunken,
            disabledContentColor = colors.textTertiary,
        ),
        elevation = null,
        contentPadding = ButtonPadding,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** "Secondary" is a filled quiet surface, not an outline. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ReleaseFlowTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surfaceSunken,
            contentColor = colors.textPrimary,
            disabledContainerColor = colors.surfaceSunken,
            disabledContentColor = colors.textTertiary,
        ),
        elevation = null,
        contentPadding = ButtonPadding,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** A destructive action (Halt): red text on a red tint, same shape as the others. */
@Composable
fun DangerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ReleaseFlowTheme.colors
    Button(
        onClick = onClick,
        modifier = modifier.height(ButtonHeight),
        enabled = enabled,
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.tint(colors.danger),
            contentColor = colors.danger,
            disabledContainerColor = colors.surfaceSunken,
            disabledContentColor = colors.textTertiary,
        ),
        elevation = null,
        contentPadding = ButtonPadding,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Inline accent link ("Open submission checklist"). */
@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val colors = ReleaseFlowTheme.colors
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(contentColor = colors.accent, disabledContentColor = colors.textTertiary),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium)
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(imageVector = trailingIcon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
    }
}

/** Pill-shaped accent FAB without a shadow. */
@Composable
fun AppFab(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    ExtendedFloatingActionButton(
        text = { Text(text = text, style = MaterialTheme.typography.labelLarge) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = colors.accent,
        contentColor = colors.onAccent,
        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
    )
}
