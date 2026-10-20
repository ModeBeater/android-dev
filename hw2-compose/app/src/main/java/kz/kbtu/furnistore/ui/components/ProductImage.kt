package kz.kbtu.furnistore.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kz.kbtu.furnistore.R
import kz.kbtu.furnistore.domain.model.Chair
import kz.kbtu.furnistore.domain.model.Product
import kz.kbtu.furnistore.domain.model.Sofa
import kz.kbtu.furnistore.domain.model.Table
import kz.kbtu.furnistore.domain.model.Wardrobe

/** Maps a domain product type to its vector drawable — the UI layer decides how a type looks. */
@DrawableRes
fun Product.iconRes(): Int = when (this) {
    is Sofa -> R.drawable.ic_sofa
    is Table -> R.drawable.ic_table
    is Chair -> R.drawable.ic_chair
    is Wardrobe -> R.drawable.ic_wardrobe
    else -> R.drawable.ic_chair
}

/**
 * Reusable component #1: a round tinted badge with the product's icon.
 * `Box` stacks the colored circle and the `Image` on top of each other.
 */
@Composable
fun ProductImage(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = product.iconRes()),
            contentDescription = product.name,
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
        )
    }
}
