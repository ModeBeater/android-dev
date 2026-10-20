package kz.kbtu.furnistore.domain.service

import kotlinx.coroutines.delay
import kz.kbtu.furnistore.domain.model.Customer
import kz.kbtu.furnistore.domain.model.Order
import kz.kbtu.furnistore.domain.model.OrderItem
import kz.kbtu.furnistore.domain.store.Inventory

/** A discount rule looks at an order and returns a fraction of the subtotal to subtract. */
typealias DiscountRule = (Order) -> Double

/** The three rules used by the store; combined in [OrderService.discountFor]. */
object DiscountRules {
    val all: List<DiscountRule> = listOf(
        { order -> if (order.customer.isWholesale) 0.10 else 0.0 },      // wholesale customers
        { order -> if (order.totalQuantity >= 5) 0.05 else 0.0 },        // bulk orders
        { order -> if (order.customer.city == "Almaty") 0.02 else 0.0 }, // local pickup
    )
}

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

    /** Applies every rule, sums the fractions with `reduce`, caps the result at 25 %. */
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
        delay(1_200) // simulated round-trip to the payment gateway
        val amount = amountDue(order)
        return if (order.customer.isWholesale && amount > 1_500_000) {
            PaymentResult.Declined("wholesale credit limit exceeded")
        } else {
            PaymentResult.Success("TX-${order.id}")
        }
    }

    suspend fun ship(order: Order): String {
        delay(800) // simulated call to the courier API
        return "KZ${order.id}${order.customer.city.take(3).uppercase()}"
    }

    /** Full lifecycle of one order: pay, then ship, or cancel (and release stock) if payment fails. */
    suspend fun fulfil(order: Order): Order {
        when (val result = processPayment(order)) {
            is PaymentResult.Success -> {
                order.markPaid(amountDue(order), result.transactionId)
                val tracking = ship(order)
                order.markShipped(tracking)
            }
            is PaymentResult.Declined -> {
                order.cancel(result.reason)
                inventory.release(order.items)
            }
        }
        return order
    }
}
