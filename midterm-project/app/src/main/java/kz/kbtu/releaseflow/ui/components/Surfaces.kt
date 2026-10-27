package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kz.kbtu.releaseflow.ui.theme.KpiNumberStyle
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/** Horizontal page padding and the room a scrolling list leaves under the floating bottom bar. */
val PagePadding = 20.dp
val PageListPadding = PaddingValues(start = PagePadding, top = 8.dp, end = PagePadding, bottom = 140.dp)

/**
 * The surface every card is built from: a lighter block on the canvas, radius 20, no outline,
 * no elevation. Optional `onClick` makes the whole card a button.
 */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = ReleaseFlowTheme.colors.surface,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Hairline between rows inside a grouped card — the only place a border is used. */
@Composable
fun Hairline(modifier: Modifier = Modifier, startIndent: Int = 16) {
    HorizontalDivider(
        modifier = modifier.padding(start = startIndent.dp),
        thickness = 1.dp,
        color = ReleaseFlowTheme.colors.border,
    )
}

/** A card of rows separated by hairlines: one `row` per item, a hairline between each pair. */
@Composable
fun <T> GroupedCard(
    items: List<T>,
    modifier: Modifier = Modifier,
    row: @Composable (T) -> Unit,
) {
    FlatCard(modifier = modifier, padding = PaddingValues(0.dp)) {
        items.forEachIndexed { index, item ->
            if (index > 0) Hairline()
            row(item)
        }
    }
}

/**
 * Title above a group of content: a normal heading in the primary colour with an optional
 * grey count and an accent action on the right — never a tracked-out uppercase overline.
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = ReleaseFlowTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (count != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = count.toString(), style = MaterialTheme.typography.titleMedium, color = colors.textTertiary)
        }
        Spacer(modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            Row(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelMedium, color = colors.accent)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

/**
 * One row of a grouped card: an optional icon in a tinted circle, a label with a caption,
 * a value on the right (or a chevron when the row navigates).
 */
@Composable
fun KeyValueRow(
    label: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    value: String? = null,
    valueColor: Color = ReleaseFlowTheme.colors.textPrimary,
    icon: ImageVector? = null,
    iconTint: Color = ReleaseFlowTheme.colors.accent,
    mono: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = ReleaseFlowTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(colors.tint(iconTint), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = value,
                style = if (mono) MonoText.medium else MaterialTheme.typography.labelLarge,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** A metric: quiet label, big tabular number, optional caption and a coloured note on the right of the number. */
@Composable
fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    note: String? = null,
    noteColor: Color = ReleaseFlowTheme.colors.success,
    onClick: (() -> Unit)? = null,
) {
    val colors = ReleaseFlowTheme.colors
    FlatCard(modifier = modifier, onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(text = value, style = KpiNumberStyle, color = colors.textPrimary, maxLines = 1)
            if (note != null) {
                Spacer(modifier = Modifier.width(8.dp))
                AppPill(label = note, color = noteColor, dense = true, modifier = Modifier.padding(bottom = 5.dp))
            }
        }
        if (caption != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Thin pill-shaped progress bar with the "6 / 9" caption. */
@Composable
fun ChecklistProgress(done: Int, total: Int, modifier: Modifier = Modifier, label: String = "Checklist") {
    val colors = ReleaseFlowTheme.colors
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "$done / $total",
                style = MonoText.small,
                color = if (total > 0 && done == total) colors.success else colors.textSecondary,
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = if (total > 0 && done == total) colors.success else colors.accent,
            trackColor = colors.surfaceSunken,
            strokeCap = StrokeCap.Round,
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

/** A label, a value and a horizontal bar: the share of `fraction` in the accent colour. */
@Composable
fun BarRow(
    label: String,
    valueText: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = ReleaseFlowTheme.colors.accent,
) {
    val colors = ReleaseFlowTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = valueText, style = MonoText.medium, color = colors.textSecondary)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.surfaceSunken, CircleShape),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .background(color, CircleShape),
            )
        }
    }
}
