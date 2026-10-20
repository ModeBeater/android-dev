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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.util.tenge
import kz.kbtu.furnistore.ui.OrderSummary
import kz.kbtu.furnistore.ui.StoreUiState
import kz.kbtu.furnistore.ui.components.EmptyState
import kz.kbtu.furnistore.ui.components.OrderStatusBadge
import kz.kbtu.furnistore.ui.components.StateDemoCard

/** Orders tab: the rotation demo card on top, then the order history (newest first). */
@Composable
fun OrdersScreen(
    state: StoreUiState,
    onDemoIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StateDemoCard(
                viewModelCount = state.demoCounter,
                onViewModelIncrement = onDemoIncrement,
            )
        }

        if (state.orders.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.List,
                    title = "No orders yet",
                    subtitle = "Orders you place from the cart will appear here.",
                    modifier = Modifier.height(320.dp),
                )
            }
        } else {
            item {
                Text(
                    text = "Order history",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(state.orders, key = { it.id }) { order ->
                OrderCard(order = order)
            }
        }
    }
}

@Composable
private fun OrderCard(order: OrderSummary, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Order #${order.id}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                OrderStatusBadge(status = order.status)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${order.customerName} · ${order.city}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${order.itemCount} item(s) · subtotal ${order.subtotal.tenge()} · to pay ${order.amountDue.tenge()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = order.statusText.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
