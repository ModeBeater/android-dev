package furnistore

import furnistore.model.Chair
import furnistore.model.Customer
import furnistore.model.Material
import furnistore.model.OrderItem
import furnistore.model.OrderStatus
import furnistore.model.Product
import furnistore.model.Sofa
import furnistore.model.Table
import furnistore.model.Wardrobe
import furnistore.service.DiscountRule
import furnistore.service.OrderService
import furnistore.store.Inventory
import furnistore.util.section
import furnistore.util.tenge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.system.measureTimeMillis

const val STORE_NAME = "FurniStore"
const val LOW_STOCK_THRESHOLD = 2

/** Builds the catalog. Each product type is a different subclass of [Product]. */
fun buildCatalog(): List<Product> = listOf(
    Sofa(1, "Oslo sofa", 450_000.0, Material.FABRIC, weightKg = 85.0, seats = 3),
    Sofa(2, "Nord sofa", 320_000.0, Material.LEATHER, weightKg = 60.0, seats = 2),
    Table(3, "Astana dining table", 280_000.0, Material.OAK, weightKg = 48.0, lengthCm = 180, widthCm = 90),
    Table(4, "Cafe table", 65_000.0, Material.PINE, weightKg = 12.0, lengthCm = 70, widthCm = 70),
    Chair(5, "Loft chair", 25_000.0, Material.METAL, weightKg = 5.5, hasArmrests = false),
    Chair(6, "Comfort chair", 42_000.0, Material.FABRIC, weightKg = 7.0, hasArmrests = true),
    Wardrobe(7, "Almaty wardrobe", 390_000.0, Material.OAK, weightKg = 120.0, doors = 3),
    Wardrobe(8, "Mini wardrobe", 210_000.0, Material.PINE, weightKg = 70.0, doors = 2),
)

fun main() = runBlocking<Unit> {
    // -----------------------------------------------------------------------
    // Variables and basic data types
    // -----------------------------------------------------------------------
    val openedYear: Int = 2019
    val currentYear = 2026            // type inferred as Int
    val vatRate: Double = 0.12
    val isOpen: Boolean = true
    var ordersPlaced = 0              // `var` because it changes later
    val yearsOnMarket = currentYear - openedYear

    println("$STORE_NAME — furniture store, $yearsOnMarket years on the market, VAT ${(vatRate * 100).toInt()}%")
    if (!isOpen) {
        println("The store is closed today.")
        return@runBlocking
    }

    // -----------------------------------------------------------------------
    // Catalog and stock: polymorphism through describe(), loops and `when`
    // -----------------------------------------------------------------------
    val catalog = buildCatalog()
    val inventory = Inventory(catalog)

    section("Catalog")
    for (product in catalog) {
        println(product.describe())          // each subclass prints its own details
    }

    // initial stock depends on the product type
    for (product in catalog) {
        val units = when (product) {
            is Chair -> 20
            is Table -> 6
            is Sofa -> 2
            is Wardrobe -> 5
            else -> 1
        }
        inventory.restock(product.id, units)
    }

    section("Stock")
    inventory.printStock()

    section("Assembly and delivery (overridden methods)")
    for (product in catalog) {
        println("${product.name.padEnd(22)} assembly ${product.assemblyMinutes().toString().padStart(3)} min, delivery 100 km: ${product.deliveryCost(100).tenge()}")
    }

    // -----------------------------------------------------------------------
    // Collections: List, Set, Map and map / filter / reduce
    // -----------------------------------------------------------------------
    section("Collection operations")

    val names: List<String> = catalog.map { it.name }
    println("Product names: $names")

    val oakProducts = catalog.filter { it.material == Material.OAK }
    println("Oak products: ${oakProducts.map { it.name }}")

    val catalogValue = catalog.map { it.price }.reduce { acc, price -> acc + price }
    println("Sum of all catalog prices (reduce): ${catalogValue.tenge()}")

    val cheapest = catalog.minByOrNull { it.price }
    val priciest = catalog.maxByOrNull { it.price }
    println("Cheapest: ${cheapest?.name}, most expensive: ${priciest?.name}")

    val (affordable, premium) = catalog.partition { it.price < 100_000 }
    println("Under 100 000 ₸: ${affordable.size} products, premium: ${premium.size} products")

    val byType: Map<String, List<Product>> = catalog.groupBy { it::class.simpleName ?: "Unknown" }
    for ((type, products) in byType) {
        println("  $type -> ${products.joinToString { it.name }}")
    }

    val materialsInStock: Set<Material> = catalog.map { it.material }.toSet()
    println("Distinct materials: ${materialsInStock.map { it.label }}")

    val wishlist = setOf(1, 2, 6)                 // product ids saved by a customer
    val trending = setOf(2, 6, 7)                 // product ids that sell best this week
    val bothWishedAndTrending = wishlist intersect trending
    println("Wishlist ∩ trending: ${bothWishedAndTrending.mapNotNull { inventory.product(it)?.name }}")

    // higher-order function with a lambda as the search rule
    val budgetPicks = inventory.find { it.price < 100_000 && it.material != Material.METAL }
    println("Budget non-metal picks: ${budgetPicks.map { it.name }}")

    // -----------------------------------------------------------------------
    // Data classes: equality, copy(), destructuring
    // -----------------------------------------------------------------------
    section("Customers (data classes)")
    val aigerim = Customer(1, "Aigerim", "Almaty")
    val bereket = Customer(2, "Bereket Furniture LLP", "Astana", isWholesale = true)
    val daniyar = Customer(3, "Daniyar", "Shymkent")

    val aigerimMoved = aigerim.copy(city = "Astana")
    println("Original: $aigerim")
    println("Copy with a new city: $aigerimMoved")
    println("aigerim == aigerimMoved? ${aigerim == aigerimMoved} (city differs); aigerim == aigerim.copy()? ${aigerim == aigerim.copy()} (all fields equal)")
    val (_, name, city) = daniyar
    println("Destructured: $name from $city")

    // -----------------------------------------------------------------------
    // Functions, lambdas and higher-order functions: discount rules
    // -----------------------------------------------------------------------
    val discountRules: List<DiscountRule> = listOf(
        { order -> if (order.customer.isWholesale) 0.10 else 0.0 },   // wholesale customers
        { order -> if (order.totalQuantity >= 5) 0.05 else 0.0 },     // bulk orders
        { order -> if (order.customer.city == "Almaty") 0.02 else 0.0 }, // local pickup
    )
    val service = OrderService(inventory, discountRules)

    // -----------------------------------------------------------------------
    // Placing orders: nullable result + Elvis operator
    // -----------------------------------------------------------------------
    section("Placing orders")

    fun item(productId: Int, quantity: Int): OrderItem {
        val product = inventory.product(productId)
            ?: throw IllegalArgumentException("Unknown product id $productId")
        return OrderItem(product, quantity)
    }

    val requests = listOf(
        aigerim to listOf(item(1, 1), item(6, 4)),
        bereket to listOf(item(7, 4), item(3, 2)),
        daniyar to listOf(item(5, 12)),
        aigerim to listOf(item(2, 3)),              // only 2 Nord sofas in stock -> rejected
    )

    for ((customer, items) in requests) {
        val order = service.placeOrder(customer, items)
        if (order == null) {
            println("Rejected for ${customer.name}: not enough stock for ${items.joinToString { "${it.quantity}x ${it.product.name}" }}")
            continue
        }
        ordersPlaced++
        val discount = service.discountFor(order)
        println("Placed order #${order.id} for ${customer.name}: ${order.subtotal.tenge()}, discount ${(discount * 100).toInt()}% -> due ${service.amountDue(order).tenge()}")
    }
    println("Orders placed: $ordersPlaced of ${requests.size}")

    section("Stock after reservations")
    inventory.printStock()
    val toReorder = inventory.lowStock(LOW_STOCK_THRESHOLD)
    println("Needs re-ordering: ${toReorder.ifEmpty { setOf("nothing") }}")

    // -----------------------------------------------------------------------
    // Coroutines: suspend functions run concurrently with async/await
    // -----------------------------------------------------------------------
    section("Fulfilment (coroutines)")

    val heartbeat = launch {
        repeat(3) {
            delay(150)
            println("  ...gateway still working")
        }
    }
    val elapsed = measureTimeMillis {
        service.fulfilAll()
    }
    heartbeat.join()
    println("All orders processed concurrently in ~$elapsed ms (sequentially it would take ~${service.allOrders.size * 500} ms)")

    // -----------------------------------------------------------------------
    // Summary: exhaustive `when` on the sealed class, grouping, while loop
    // -----------------------------------------------------------------------
    section("Order summary")
    for (order in service.allOrders) {
        println(order)
    }

    val shippedOrders = service.allOrders.filter { it.status is OrderStatus.Shipped }
    val revenueByCity: Map<String, Double> = shippedOrders
        .groupBy { it.customer.city }
        .mapValues { (_, orders) -> orders.sumOf { service.amountDue(it) } }
    println("Revenue by city:")
    for ((cityName, revenue) in revenueByCity) {
        println("  $cityName: ${revenue.tenge()}")
    }

    // assembly workshop queue — every product reports its own assembly time (polymorphism)
    val queue = ArrayDeque(shippedOrders.flatMap { it.items })
    var totalMinutes = 0
    while (queue.isNotEmpty()) {
        val line = queue.removeFirst()
        totalMinutes += line.product.assemblyMinutes() * line.quantity
    }
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    println("Assembly workload for shipped orders: $hours h $minutes min")
}
