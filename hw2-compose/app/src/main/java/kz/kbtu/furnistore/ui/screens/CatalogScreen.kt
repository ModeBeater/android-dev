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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.model.Material
import kz.kbtu.furnistore.ui.StoreUiState
import kz.kbtu.furnistore.ui.components.EmptyState
import kz.kbtu.furnistore.ui.components.MaterialFilterRow
import kz.kbtu.furnistore.ui.components.ProductCard
import kz.kbtu.furnistore.ui.components.SearchField

/**
 * Catalog tab: search, filters and the product list.
 *
 * All data comes from [state]; all changes go out through the callbacks.
 * The only state kept here is purely visual (is the filter panel open?) —
 * it is `rememberSaveable`, so an open panel stays open after rotation.
 */
@Composable
fun CatalogScreen(
    state: StoreUiState,
    onQueryChange: (String) -> Unit,
    onToggleMaterial: (Material) -> Unit,
    onInStockOnlyChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit,
    onAddToCart: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val visible = state.visibleProducts

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "${visible.size} of ${state.products.size} products",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (state.hasActiveFilters) {
                TextButton(onClick = onClearFilters) { Text("Clear") }
                Spacer(modifier = Modifier.width(4.dp))
            }
            TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                Text(if (filtersExpanded) "Hide filters" else "Filters")
            }
        }

        if (filtersExpanded) {
            MaterialFilterRow(
                selected = state.selectedMaterials,
                onToggle = onToggleMaterial,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Switch(checked = state.inStockOnly, onCheckedChange = onInStockOnlyChange)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = "In stock only", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (visible.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = "Nothing found",
                subtitle = "Try another search term or clear the filters.",
                action = { TextButton(onClick = onClearFilters) { Text("Clear filters") } },
            )
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(visible, key = { it.id }) { product ->
                    ProductCard(
                        product = product,
                        stock = state.stock[product.id] ?: 0,
                        inCart = state.cart[product.id] ?: 0,
                        onAddToCart = { onAddToCart(product.id) },
                    )
                }
            }
        }
    }
}
