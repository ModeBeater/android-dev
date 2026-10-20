package kz.kbtu.furnistore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.furnistore.domain.SampleData
import kz.kbtu.furnistore.domain.model.Customer
import kz.kbtu.furnistore.domain.model.Material
import kz.kbtu.furnistore.domain.model.Order
import kz.kbtu.furnistore.domain.model.OrderItem
import kz.kbtu.furnistore.domain.service.DiscountRules
import kz.kbtu.furnistore.domain.service.OrderService
import kz.kbtu.furnistore.domain.store.Inventory

/**
 * Holds the screen state and all application logic.
 *
 * The ViewModel survives configuration changes (rotation): the Activity and every
 * composable are destroyed and recreated, but this object — and therefore the cart,
 * the filters and the orders — stays alive. The UI only *reads* [uiState] and calls
 * the event methods below; it never mutates state on its own.
 */
class StoreViewModel : ViewModel() {

    private val inventory = Inventory(SampleData.catalog()).apply {
        products.forEach { restock(it.id, SampleData.initialStock(it)) }
    }
    private val service = OrderService(inventory, DiscountRules.all)
    private var nextCustomerId = 1

    private val _uiState = MutableStateFlow(
        StoreUiState(products = inventory.products, stock = inventory.snapshot())
    )
    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------
    // Catalog: search and filters
    // -------------------------------------------------------------------

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun toggleMaterial(material: Material) {
        _uiState.update { state ->
            val selected = if (material in state.selectedMaterials) {
                state.selectedMaterials - material
            } else {
                state.selectedMaterials + material
            }
            state.copy(selectedMaterials = selected)
        }
    }

    fun setInStockOnly(enabled: Boolean) {
        _uiState.update { it.copy(inStockOnly = enabled) }
    }

    fun clearFilters() {
        _uiState.update { it.copy(query = "", selectedMaterials = emptySet(), inStockOnly = false) }
    }

    // -------------------------------------------------------------------
    // Cart
    // -------------------------------------------------------------------

    fun addToCart(productId: Int) {
        val product = inventory.product(productId) ?: return
        val state = _uiState.value
        val inCart = state.cart[productId] ?: 0
        val available = inventory.stockOf(productId) - inCart
        if (available <= 0) {
            _uiState.update { it.copy(message = "No more ${product.name} in stock") }
            return
        }
        _uiState.update {
            it.copy(cart = it.cart + (productId to inCart + 1), message = "${product.name} added to cart")
                .withDiscount()
        }
    }

    /** Changes the quantity by [delta]; the result is clamped to [0, stock], and 0 removes the line. */
    fun changeQuantity(productId: Int, delta: Int) {
        _uiState.update { state ->
            val current = state.cart[productId] ?: return@update state
            val wanted = (current + delta).coerceIn(0, inventory.stockOf(productId))
            val cart = if (wanted == 0) state.cart - productId else state.cart + (productId to wanted)
            state.copy(cart = cart).withDiscount()
        }
    }

    fun removeFromCart(productId: Int) {
        _uiState.update { it.copy(cart = it.cart - productId).withDiscount() }
    }

    // -------------------------------------------------------------------
    // Checkout form
    // -------------------------------------------------------------------

    fun onCustomerNameChange(name: String) {
        _uiState.update { it.copy(customerName = name) }
    }

    fun setWholesale(isWholesale: Boolean) {
        _uiState.update { it.copy(isWholesale = isWholesale).withDiscount() }
    }

    fun selectCity(city: String) {
        _uiState.update { it.copy(city = city).withDiscount() }
    }

    /**
     * Places the order and runs payment + shipping in a coroutine.
     * `viewModelScope` runs on the main thread; the suspend functions `delay`
     * instead of blocking, so the UI stays responsive and shows a progress indicator.
     */
    fun checkout() {
        val state = _uiState.value
        if (state.cart.isEmpty() || state.isProcessing) return

        val customer = Customer(
            id = nextCustomerId++,
            name = state.customerName.ifBlank { "Guest" },
            city = state.city,
            isWholesale = state.isWholesale,
        )
        val items = state.cartLines.map { OrderItem(it.product, it.quantity) }

        // Flip the flag synchronously so the button is disabled before the coroutine even starts.
        _uiState.update { it.copy(isProcessing = true) }

        viewModelScope.launch {
            val order = service.placeOrder(customer, items)
            if (order == null) {
                _uiState.update {
                    it.copy(isProcessing = false, message = "Not enough stock to place this order")
                }
                return@launch
            }

            service.fulfil(order) // suspends ~2 s: payment gateway + courier

            _uiState.update {
                it.copy(
                    isProcessing = false,
                    cart = emptyMap(),
                    discountFraction = 0.0,
                    orders = service.allOrders.map { o -> o.toSummary() }.reversed(),
                    stock = inventory.snapshot(),
                    selectedTab = StoreTab.Orders,
                    message = "Order #${order.id}: ${order.statusText()}",
                )
            }
        }
    }

    // -------------------------------------------------------------------
    // Navigation, messages, rotation demo
    // -------------------------------------------------------------------

    fun selectTab(tab: StoreTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }

    fun incrementDemoCounter() {
        _uiState.update { it.copy(demoCounter = it.demoCounter + 1) }
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /** Recomputes the discount for the current cart and customer using the HW1 rules. */
    private fun StoreUiState.withDiscount(): StoreUiState {
        val lines = cartLines
        if (lines.isEmpty()) return copy(discountFraction = 0.0)
        val preview = Order(
            id = 0,
            customer = Customer(0, customerName, city, isWholesale),
            items = lines.map { OrderItem(it.product, it.quantity) },
        )
        return copy(discountFraction = service.discountFor(preview))
    }

    private fun Order.toSummary() = OrderSummary(
        id = id,
        customerName = customer.name,
        city = customer.city,
        itemCount = totalQuantity,
        subtotal = subtotal,
        amountDue = service.amountDue(this),
        status = status,
        statusText = statusText(),
    )
}
