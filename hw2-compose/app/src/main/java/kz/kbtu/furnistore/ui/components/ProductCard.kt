package kz.kbtu.furnistore.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.model.Material
import kz.kbtu.furnistore.domain.model.Product
import kz.kbtu.furnistore.domain.model.Sofa
import kz.kbtu.furnistore.domain.util.tenge
import kz.kbtu.furnistore.ui.theme.FurniStoreTheme

/**
 * Reusable component #2: one product in the catalog list.
 *
 * Stateless ("state hoisting"): everything it shows comes in as parameters and
 * the only thing it does on its own is call [onAddToCart]. That makes it trivial
 * to preview and to reuse with any data source.
 */
@Composable
fun ProductCard(
    product: Product,
    stock: Int,
    inCart: Int,
    onAddToCart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val available = stock - inCart

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProductImage(product = product, modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = product.details(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = product.price.tenge(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(horizontalAlignment = Alignment.End) {
                StockLabel(stock = stock)
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onAddToCart,
                    enabled = available > 0,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Spacer(modifier = Modifier.width(ButtonDefaults.IconSpacing))
                    Text(text = if (inCart > 0) "In cart: $inCart" else "Add")
                }
            }
        }
    }
}

/** Small colored text that reflects how many units are left. */
@Composable
private fun StockLabel(stock: Int) {
    val (text, color) = when (stock) {
        0 -> "Out of stock" to MaterialTheme.colorScheme.error
        in 1..2 -> "Only $stock left" to MaterialTheme.colorScheme.tertiary
        else -> "$stock in stock" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(text = text, style = MaterialTheme.typography.labelMedium, color = color)
}

@Preview(showBackground = true)
@Composable
private fun ProductCardPreview() {
    FurniStoreTheme {
        ProductCard(
            product = Sofa(1, "Oslo sofa", 450_000.0, Material.FABRIC, weightKg = 85.0, seats = 3),
            stock = 2,
            inCart = 1,
            onAddToCart = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
