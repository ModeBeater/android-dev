package kz.kbtu.furnistore.domain.model

import kz.kbtu.furnistore.domain.util.tenge

// ---------------------------------------------------------------------------
// Data classes — value objects with auto-generated equals/hashCode/toString/copy
// ---------------------------------------------------------------------------

data class Customer(
    val id: Int,
    val name: String,
    val city: String,
    val isWholesale: Boolean = false,
)

data class OrderItem(val product: Product, val quantity: Int) {
    val lineTotal: Double
        get() = product.price * quantity
}

// ---------------------------------------------------------------------------
// Sealed class — a closed set of order states. `when` over it is exhaustive.
// ---------------------------------------------------------------------------

sealed class OrderStatus {
    data object New : OrderStatus()
    data class Paid(val amount: Double, val transactionId: String) : OrderStatus()
    data class Shipped(val trackingNumber: String) : OrderStatus()
    data class Cancelled(val reason: String) : OrderStatus()
}

// ---------------------------------------------------------------------------
// Order — a regular class with encapsulated mutable state
// ---------------------------------------------------------------------------

class Order(val id: Int, val customer: Customer, val items: List<OrderItem>) {

    var status: OrderStatus = OrderStatus.New
        private set // only the order itself may move between states

    val subtotal: Double
        get() = items.sumOf { it.lineTotal }

    val totalQuantity: Int
        get() = items.sumOf { it.quantity }

    fun markPaid(amount: Double, transactionId: String) {
        status = OrderStatus.Paid(amount, transactionId)
    }

    fun markShipped(trackingNumber: String) {
        status = OrderStatus.Shipped(trackingNumber)
    }

    fun cancel(reason: String) {
        status = OrderStatus.Cancelled(reason)
    }

    /** Exhaustive `when` over the sealed class — no `else` branch is needed. */
    fun statusText(): String = when (val s = status) {
        OrderStatus.New -> "new"
        is OrderStatus.Paid -> "paid ${s.amount.tenge()} (tx ${s.transactionId})"
        is OrderStatus.Shipped -> "shipped, tracking ${s.trackingNumber}"
        is OrderStatus.Cancelled -> "cancelled — ${s.reason}"
    }

    override fun toString(): String =
        "Order #$id for ${customer.name} (${customer.city}): $totalQuantity item(s), ${subtotal.tenge()} — ${statusText()}"
}
