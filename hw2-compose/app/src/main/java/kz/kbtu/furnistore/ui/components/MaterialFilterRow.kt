package kz.kbtu.furnistore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.domain.model.Material

/**
 * Reusable component #4: a horizontally scrolling row of material filter chips.
 * Multi-select: the selected set is owned by the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialFilterRow(
    selected: Set<Material>,
    onToggle: (Material) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(Material.entries) { material ->
            val isSelected = material in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(material) },
                label = { Text(material.label.replaceFirstChar { it.uppercase() }) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    }
                } else {
                    null
                },
            )
        }
    }
}
