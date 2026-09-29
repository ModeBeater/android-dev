package furnistore.service

import furnistore.model.Customer
import furnistore.model.Order
import furnistore.model.OrderItem
import furnistore.store.Inventory
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/** A discount rule looks at an order and returns a fraction of the subtotal to subtract. */
typealias DiscountRule = (Order) -> Double

/** Result of talking to the payment gateway — a second sealed hierarchy. */
sealed class PaymentResult {
    data class Success(val transactionId: String) : PaymentResult()
    data class Declined(val reason: String) : PaymentResult()
}

/** Singleton that hands out unique order numbers. */
object OrderNumbers {
    private var counter = 1000
    fun next(): Int = ++counter
}

class OrderService(
    private val inventory: Inventory,
    private val discountRules: List<DiscountRule>,
) {
    private val orders = mutableListOf<Order>()

    val allOrders: List<Order>
        get() = orders.toList()

    /** Returns `null` when the warehouse cannot cover the order. */
    fun placeOrder(customer: Customer, items: List<OrderItem>): Order? {
        if (!inventory.reserve(items)) return null
        val order = Order(OrderNumbers.next(), customer, items)
        orders += order
        return order
    }

    /**
     * Applies every discount rule and combines the fractions with `reduce`.
     * The discount is capped at 25 % so that stacked rules never go too far.
     */
    fun discountFor(order: Order): Double {
        val fractions = discountRules.map { rule -> rule(order) }
        val total = fractions.reduce { acc, fraction -> acc + fraction }
        return minOf(total, 0.25)
    }

    fun amountDue(order: Order): Double = order.subtotal * (1 - discountFor(order))

    // -----------------------------------------------------------------------
    // Coroutines: payment and shipping are slow I/O operations, so they are
    // `suspend` functions. They pause the coroutine instead of blocking a thread.
    // -----------------------------------------------------------------------

    suspend fun processPayment(order: Order): PaymentResult {
        delay(300) // simulated round-trip to the payment gateway
        val amount = amountDue(order)
        return if (order.customer.isWholesale && amount > 1_500_000) {
            PaymentResult.Declined("wholesale credit limit exceeded")
        } else {
            PaymentResult.Success("TX-${order.id}")
        }
    }

    suspend fun ship(order: Order): String {
        delay(200) // simulated call to the courier API
        return "KZ${order.id}${order.customer.city.take(3).uppercase()}"
    }

    /** Full lifecycle of one order: pay, then ship, or cancel if payment fails. */
    suspend fun fulfil(order: Order) {
        when (val result = processPayment(order)) {
            is PaymentResult.Success -> {
                order.markPaid(amountDue(order), result.transactionId)
                val tracking = ship(order)
                order.markShipped(tracking)
            }
            is PaymentResult.Declined -> order.cancel(result.reason)
        }
        println("  processed ${order.id}: ${order.statusText()}")
    }

    /** Fulfils every open order concurrently: one coroutine per order. */
    suspend fun fulfilAll() = coroutineScope {
        orders.map { order -> async { fulfil(order) } }.awaitAll()
    }
}
