package kz.kbtu.releaseflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kz.kbtu.releaseflow.domain.model.StoreComparison
import kz.kbtu.releaseflow.domain.model.StoreListing
import kz.kbtu.releaseflow.domain.util.ageText
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.ui.theme.MonoText
import kz.kbtu.releaseflow.ui.theme.ReleaseFlowTheme

/** The app's icon from the App Store (Coil loads and caches it) or the monogram while there is none. */
@Composable
fun StoreArtwork(url: String?, monogram: String, modifier: Modifier = Modifier, size: Int = 44) {
    if (url == null) {
        AppMonogram(monogram = monogram, modifier = modifier, large = size >= 56)
    } else {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(url).crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size.dp)
                .clip(MaterialTheme.shapes.small)
                .background(ReleaseFlowTheme.colors.surfaceSunken),
        )
    }
}

/** Colour of the store-vs-tracked comparison pill. */
@Composable
fun comparisonColor(comparison: StoreComparison) = when (comparison) {
    StoreComparison.Matches -> ReleaseFlowTheme.colors.success
    StoreComparison.StoreAhead -> ReleaseFlowTheme.colors.warning
    StoreComparison.StoreBehind -> ReleaseFlowTheme.colors.info
    StoreComparison.NothingTracked -> ReleaseFlowTheme.colors.textSecondary
}

/**
 * What the App Store says about the app: icon, name and seller, live version with its
 * comparison against the tracked releases, rating, release date, expandable release notes,
 * and when this was fetched. `refreshing` / `error` come from the network call.
 */
@Composable
fun StoreListingCard(
    listing: StoreListing,
    comparison: StoreComparison?,
    now: Long,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    var notesExpanded by rememberSaveable { mutableStateOf(false) }

    FlatCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StoreArtwork(url = listing.artworkUrl, monogram = listing.name.take(2).uppercase(), size = 56)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(listing.sellerName, listing.minimumOs?.let { "iOS $it+" }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            listing.rating?.let { rating ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "★ %.1f".format(rating), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                    listing.ratingCount?.let {
                        Text(text = "%,d ratings".format(it), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Live on the App Store", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = listing.version, style = MonoText.large, color = colors.textPrimary)
                    listing.releasedAt?.let {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "since ${it.dayText()}", style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                    }
                }
            }
            if (comparison != null) {
                AppPill(label = comparison.label, color = comparisonColor(comparison), dense = true)
            }
        }

        val notes = listing.releaseNotes
        if (!notes.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "What's new", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notes,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = if (notesExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (notes.lines().size > 3 || notes.length > 160) {
                LinkButton(
                    text = if (notesExpanded) "Show less" else "Show more",
                    onClick = { notesExpanded = !notesExpanded },
                    trailingIcon = if (notesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Checked ${listing.fetchedAt.ageText(now)} ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
                if (error != null) {
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = colors.danger)
                }
            }
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
                Spacer(modifier = Modifier.width(8.dp))
            }
            LinkButton(text = "Refresh", onClick = onRefresh, enabled = !refreshing)
        }
    }
}

/** Placeholder for an app that has not been looked up yet, or that the store does not know. */
@Composable
fun StoreEmptyCard(
    bundleId: String,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ReleaseFlowTheme.colors
    FlatCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(colors.surfaceSunken, MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "A", style = MaterialTheme.typography.titleMedium, color = colors.textTertiary)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "App Store", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                Text(
                    text = if (bundleId.isBlank()) "No bundle id" else "Not checked yet — $bundleId",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (error != null) {
                    Text(text = error, style = MaterialTheme.typography.bodySmall, color = colors.danger)
                }
            }
            if (refreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.accent)
            } else {
                LinkButton(text = "Look up", onClick = onRefresh, enabled = bundleId.isNotBlank())
            }
        }
        Text(
            text = "Google Play has no public read API: Android versions are tracked here by hand.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
    }
}
