package kz.kbtu.furnistore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.util.tenge
import kz.kbtu.furnistore.ui.CartLine

/** Reusable component #6: one line of the cart with a quantity stepper and a remove button. */
@Composable
fun CartItemRow(
    line: CartLine,
    maxQuantity: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(product = line.product, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = line.product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${line.product.price.tenge()} × ${line.quantity} = ${line.lineTotal.tenge()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            QuantityStepper(
                quantity = line.quantity,
                onDecrease = onDecrease,
                onIncrease = onIncrease,
                canIncrease = line.quantity < maxQuantity,
            )
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove ${line.product.name}",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
