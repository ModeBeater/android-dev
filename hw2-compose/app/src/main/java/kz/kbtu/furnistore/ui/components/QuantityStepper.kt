package kz.kbtu.furnistore.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Reusable component #5: "− 2 +" quantity control.
 * Stateless — the quantity is hoisted to the caller (ultimately the ViewModel).
 */
@Composable
fun QuantityStepper(
    quantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    canIncrease: Boolean = true,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDecrease) {
            Text(text = "−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = quantity.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 28.dp),
        )
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Increase quantity")
        }
    }
}
