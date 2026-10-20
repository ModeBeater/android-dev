package kz.kbtu.furnistore.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.SampleData
import kz.kbtu.furnistore.domain.util.percent
import kz.kbtu.furnistore.domain.util.tenge
import kz.kbtu.furnistore.ui.StoreUiState
import kz.kbtu.furnistore.ui.components.CartItemRow
import kz.kbtu.furnistore.ui.components.EmptyState

/**
 * Cart tab: lines with quantity steppers, the customer form and the checkout summary.
 * The confirmation dialog's visibility is `rememberSaveable` — if the user rotates the
 * phone with the dialog open, it is still open afterwards.
 */
@Composable
fun CartScreen(
    state: StoreUiState,
    onDecrease: (Int) -> Unit,
    onIncrease: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onWholesaleChange: (Boolean) -> Unit,
    onCityChange: (String) -> Unit,
    onCheckout: () -> Unit,
    onBrowseCatalog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showConfirmDialog by rememberSaveable { mutableStateOf(false) }
    val lines = state.cartLines

    if (lines.isEmpty()) {
        EmptyState(
            icon = Icons.Default.ShoppingCart,
            title = "Your cart is empty",
            subtitle = "Add something from the catalog to place an order.",
            modifier = modifier,
            action = { Button(onClick = onBrowseCatalog) { Text("Browse catalog") } },
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(lines, key = { it.product.id }) { line ->
            CartItemRow(
                line = line,
                maxQuantity = state.stock[line.product.id] ?: 0,
                onDecrease = { onDecrease(line.product.id) },
                onIncrease = { onIncrease(line.product.id) },
                onRemove = { onRemove(line.product.id) },
            )
        }

        item {
            CustomerForm(
                name = state.customerName,
                isWholesale = state.isWholesale,
                city = state.city,
                onNameChange = onCustomerNameChange,
                onWholesaleChange = onWholesaleChange,
                onCityChange = onCityChange,
            )
        }

        item {
            CheckoutSummary(
                itemCount = state.cartCount,
                subtotal = state.cartSubtotal,
                discountFraction = state.discountFraction,
                total = state.cartTotal,
                isProcessing = state.isProcessing,
                onCheckout = { showConfirmDialog = true },
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Place order?") },
            text = {
                Text(
                    "${state.cartCount} item(s) for ${state.cartTotal.tenge()} " +
                        "will be ordered for ${state.customerName.ifBlank { "Guest" }} in ${state.city}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onCheckout()
                    },
                ) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancel") }
            },
        )
    }
}

/** Customer details: text input, a checkbox and single-select city chips. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerForm(
    name: String,
    isWholesale: Boolean,
    city: String,
    onNameChange: (String) -> Unit,
    onWholesaleChange: (Boolean) -> Unit,
    onCityChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Customer", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Name") },
                placeholder = { Text("Guest") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = isWholesale, onCheckedChange = onWholesaleChange)
                Column {
                    Text(text = "Wholesale customer", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "10% off, credit limit 1 500 000 ₸",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Delivery city",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SampleData.cities.forEach { option ->
                    FilterChip(
                        selected = option == city,
                        onClick = { onCityChange(option) },
                        label = { Text(option) },
                    )
                }
            }
        }
    }
}

/** Totals plus the checkout button; shows a spinner while the order is being processed. */
@Composable
private fun CheckoutSummary(
    itemCount: Int,
    subtotal: Double,
    discountFraction: Double,
    total: Double,
    isProcessing: Boolean,
    onCheckout: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(label = "Items", value = itemCount.toString())
            SummaryRow(label = "Subtotal", value = subtotal.tenge())
            SummaryRow(
                label = "Discount",
                value = if (discountFraction > 0) "−${discountFraction.percent()}" else "—",
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SummaryRow(label = "Total", value = total.tenge(), emphasized = true)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCheckout,
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Processing payment…")
                } else {
                    Text("Place order")
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, emphasized: Boolean = false) {
    val style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Text(text = label, style = style, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = style,
            fontWeight = if (emphasized) FontWeight.Bold else null,
        )
    }
}
