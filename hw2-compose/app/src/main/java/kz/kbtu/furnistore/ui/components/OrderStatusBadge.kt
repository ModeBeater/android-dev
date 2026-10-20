package kz.kbtu.furnistore.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.model.OrderStatus

/**
 * Reusable component #7: colored pill for an order status.
 * The `when` is exhaustive over the sealed class — adding a new status
 * would be a compile error here until it is handled.
 */
@Composable
fun OrderStatusBadge(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val (label, container, content) = when (status) {
        OrderStatus.New -> Triple("New", scheme.surfaceVariant, scheme.onSurfaceVariant)
        is OrderStatus.Paid -> Triple("Paid", scheme.secondaryContainer, scheme.onSecondaryContainer)
        is OrderStatus.Shipped -> Triple("Shipped", scheme.primaryContainer, scheme.onPrimaryContainer)
        is OrderStatus.Cancelled -> Triple("Cancelled", scheme.errorContainer, scheme.onErrorContainer)
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
