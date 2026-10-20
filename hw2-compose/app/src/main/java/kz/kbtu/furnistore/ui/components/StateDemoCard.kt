package kz.kbtu.furnistore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Reusable component #9: three counters that behave differently when the device rotates.
 *
 * - `remember`          — kept only while this composable stays in the composition.
 *                          Rotation recreates the Activity and the whole UI tree -> resets to 0.
 * - `rememberSaveable`  — additionally written into the Activity's saved-instance Bundle
 *                          -> survives rotation (and even process death).
 * - `ViewModel`         — lives in the ViewModel, which outlives the Activity during a
 *                          configuration change -> survives rotation; lost on process death
 *                          unless SavedStateHandle is used.
 */
@Composable
fun StateDemoCard(
    viewModelCount: Int,
    onViewModelIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rememberCount by remember { mutableIntStateOf(0) }
    var saveableCount by rememberSaveable { mutableStateOf(0) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Rotation demo", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Tap the buttons, then rotate the device and compare.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(12.dp))

            CounterRow(
                label = "remember",
                hint = "resets — composable is recreated",
                count = rememberCount,
                onIncrement = { rememberCount++ },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            CounterRow(
                label = "rememberSaveable",
                hint = "survives — saved to the Bundle",
                count = saveableCount,
                onIncrement = { saveableCount++ },
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            CounterRow(
                label = "ViewModel",
                hint = "survives — outlives the Activity",
                count = viewModelCount,
                onIncrement = onViewModelIncrement,
            )
        }
    }
}

@Composable
private fun CounterRow(
    label: String,
    hint: String,
    count: Int,
    onIncrement: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = count.toString(), style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.width(12.dp))
        FilledTonalButton(onClick = onIncrement) {
            Text(text = "+1")
        }
    }
}
