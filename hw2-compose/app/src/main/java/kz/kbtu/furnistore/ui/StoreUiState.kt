package kz.kbtu.furnistore.ui

import kz.kbtu.furnistore.domain.model.Material
import kz.kbtu.furnistore.domain.model.OrderStatus
import kz.kbtu.furnistore.domain.model.Product

/** The three destinations of the bottom navigation bar. */
enum class StoreTab(val title: String) {
    Catalog("Catalog"),
    Cart("Cart"),
    Orders("Orders"),
}

/** One line of the cart, derived from the `cart` map for the UI. */
data class CartLine(val product: Product, val quantity: Int) {
    val lineTotal: Double
        get() = product.price * quantity
}

/**
 * Immutable snapshot of an order for the UI. `Order` itself is mutable (its status changes),
 * so the ViewModel converts it into this value object every time something changes —
 * a new snapshot is a new object, which is what triggers recomposition.
 */
data class OrderSummary(
    val id: Int,
    val customerName: String,
    val city: String,
    val itemCount: Int,
    val subtotal: Double,
    val amountDue: Double,
    val status: OrderStatus,
    val statusText: String,
)

/**
 * Everything the screen needs, in one immutable data class.
 * The UI never changes it directly — it calls ViewModel methods, and the ViewModel
 * publishes a new copy. Derived values (visible products, cart lines, totals) are
 * computed getters so they are always consistent with the raw fields.
 */
data class StoreUiState(
    val products: List<Product> = emptyList(),
    val stock: Map<Int, Int> = emptyMap(),
    // Catalog filters
    val query: String = "",
    val selectedMaterials: Set<Material> = emptySet(),
    val inStockOnly: Boolean = false,
    // Cart: productId -> quantity (insertion order preserved by LinkedHashMap)
    val cart: Map<Int, Int> = emptyMap(),
    val discountFraction: Double = 0.0,
    // Checkout form
    val customerName: String = "",
    val isWholesale: Boolean = false,
    val city: String = "Almaty",
    // Orders
    val orders: List<OrderSummary> = emptyList(),
    val isProcessing: Boolean = false,
    // Navigation and one-shot messages
    val selectedTab: StoreTab = StoreTab.Catalog,
    val message: String? = null,
    // Counter for the rotation demo card
    val demoCounter: Int = 0,
) {
    val visibleProducts: List<Product>
        get() = products.filter { product ->
            val matchesQuery = query.isBlank() ||
                product.name.contains(query, ignoreCase = true) ||
                product.material.label.contains(query, ignoreCase = true)
            val matchesMaterial = selectedMaterials.isEmpty() || product.material in selectedMaterials
            val matchesStock = !inStockOnly || (stock[product.id] ?: 0) > 0
            matchesQuery && matchesMaterial && matchesStock
        }

    val cartLines: List<CartLine>
        get() = cart.mapNotNull { (productId, quantity) ->
            products.firstOrNull { it.id == productId }?.let { CartLine(it, quantity) }
        }

    val cartCount: Int
        get() = cart.values.sum()

    val cartSubtotal: Double
        get() = cartLines.sumOf { it.lineTotal }

    val cartTotal: Double
        get() = cartSubtotal * (1 - discountFraction)

    val hasActiveFilters: Boolean
        get() = query.isNotBlank() || selectedMaterials.isNotEmpty() || inStockOnly
}
