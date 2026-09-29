package furnistore.store

import furnistore.model.OrderItem
import furnistore.model.Product

/**
 * Warehouse: knows every product and how many units are in stock.
 * Backed by a `Map` (id -> product) and a `MutableMap` (id -> quantity).
 */
class Inventory(products: List<Product>) {

    private val byId: Map<Int, Product> = products.associateBy { it.id }
    private val stock: MutableMap<Int, Int> = products.associate { it.id to 0 }.toMutableMap()

    val products: List<Product>
        get() = byId.values.toList()

    /** Returns `null` for an unknown id — the caller has to deal with the absence. */
    fun product(id: Int): Product? = byId[id]

    fun stockOf(productId: Int): Int = stock.getOrDefault(productId, 0)

    fun restock(productId: Int, quantity: Int) {
        require(quantity > 0) { "Restock quantity must be positive" }
        stock[productId] = stockOf(productId) + quantity
    }

    /**
     * Higher-order function: the caller passes the search rule as a lambda,
     * e.g. `inventory.find { it.price < 100_000 }`.
     */
    fun find(predicate: (Product) -> Boolean): List<Product> = products.filter(predicate)

    /** Checks availability of every line and reserves the stock atomically. */
    fun reserve(items: List<OrderItem>): Boolean {
        val allAvailable = items.all { stockOf(it.product.id) >= it.quantity }
        if (!allAvailable) return false
        for (item in items) {
            stock[item.product.id] = stockOf(item.product.id) - item.quantity
        }
        return true
    }

    /** Product names that need re-ordering — a `Set` because each name should appear once. */
    fun lowStock(threshold: Int = 2): Set<String> =
        products.filter { stockOf(it.id) <= threshold }.map { it.name }.toSet()

    fun printStock() {
        for ((index, product) in products.withIndex()) {
            val units = stockOf(product.id)
            val marker = when (units) {
                0 -> "  OUT"
                in 1..2 -> "  LOW"
                else -> ""
            }
            println("${index + 1}. ${product.name.padEnd(22)} ${units.toString().padStart(3)} pcs$marker")
        }
    }
}
