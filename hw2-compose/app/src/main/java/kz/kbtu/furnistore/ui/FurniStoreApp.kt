package kz.kbtu.furnistore.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kz.kbtu.furnistore.ui.screens.CartScreen
import kz.kbtu.furnistore.ui.screens.CatalogScreen
import kz.kbtu.furnistore.ui.screens.OrdersScreen

/**
 * Root of the UI. Observes the ViewModel's [StoreUiState] and forwards user events back.
 *
 * `viewModel()` returns the same [StoreViewModel] instance after rotation — the Activity
 * is recreated, the ViewModel is not. `collectAsStateWithLifecycle()` turns the StateFlow
 * into Compose state and stops collecting while the app is in the background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurniStoreApp(viewModel: StoreViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // One-shot messages: show the snackbar, then tell the ViewModel it was consumed.
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.onMessageShown()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FurniStore · ${state.selectedTab.title}") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = {
            StoreNavigationBar(
                selected = state.selectedTab,
                cartCount = state.cartCount,
                onSelect = viewModel::selectTab,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.selectedTab) {
                StoreTab.Catalog -> CatalogScreen(
                    state = state,
                    onQueryChange = viewModel::onQueryChange,
                    onToggleMaterial = viewModel::toggleMaterial,
                    onInStockOnlyChange = viewModel::setInStockOnly,
                    onClearFilters = viewModel::clearFilters,
                    onAddToCart = viewModel::addToCart,
                )

                StoreTab.Cart -> CartScreen(
                    state = state,
                    onDecrease = { id -> viewModel.changeQuantity(id, -1) },
                    onIncrease = { id -> viewModel.changeQuantity(id, +1) },
                    onRemove = viewModel::removeFromCart,
                    onCustomerNameChange = viewModel::onCustomerNameChange,
                    onWholesaleChange = viewModel::setWholesale,
                    onCityChange = viewModel::selectCity,
                    onCheckout = viewModel::checkout,
                    onBrowseCatalog = { viewModel.selectTab(StoreTab.Catalog) },
                )

                StoreTab.Orders -> OrdersScreen(
                    state = state,
                    onDemoIncrement = viewModel::incrementDemoCounter,
                )
            }
        }
    }
}

/** Bottom navigation with a cart badge. */
@Composable
private fun StoreNavigationBar(
    selected: StoreTab,
    cartCount: Int,
    onSelect: (StoreTab) -> Unit,
) {
    NavigationBar {
        StoreTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                label = { Text(tab.title) },
                icon = {
                    if (tab == StoreTab.Cart && cartCount > 0) {
                        BadgedBox(badge = { Badge { Text(cartCount.toString()) } }) {
                            Icon(imageVector = tab.icon(), contentDescription = tab.title)
                        }
                    } else {
                        Icon(imageVector = tab.icon(), contentDescription = tab.title)
                    }
                },
            )
        }
    }
}

private fun StoreTab.icon(): ImageVector = when (this) {
    StoreTab.Catalog -> Icons.Default.Home
    StoreTab.Cart -> Icons.Default.ShoppingCart
    StoreTab.Orders -> Icons.AutoMirrored.Filled.List
}
