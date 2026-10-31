package kz.kbtu.releaseflow.domain.model

/**
 * What the App Store currently says about an app (iTunes Lookup API), cached per app in the
 * database. `previousVersion` is the version seen by the sync before this one — the
 * background worker compares the two to decide whether to notify.
 */
data class StoreListing(
    val appId: Int,
    val storeAppId: Long,
    val name: String,
    val bundleId: String,
    val version: String,
    val releaseNotes: String?,
    val releasedAt: Long?,
    val rating: Double?,
    val ratingCount: Int?,
    val artworkUrl: String?,
    val storeUrl: String?,
    val sellerName: String?,
    val minimumOs: String?,
    val fetchedAt: Long,
    val previousVersion: String? = null,
)

/** How the store's live version relates to the newest version this app has shipped on iOS. */
enum class StoreComparison(val label: String) {
    Matches("matches tracked"),
    StoreAhead("newer than tracked"),
    StoreBehind("older than tracked"),
    NothingTracked("nothing live yet"),
}

/** One release of a GitHub repository (GitHub REST API), already mapped from the DTO. */
data class GitHubRelease(
    val id: Long,
    val tag: String,
    val name: String?,
    val body: String?,
    val prerelease: Boolean,
    val draft: Boolean,
    val publishedAt: Long?,
    val author: String?,
    val url: String,
    val assetCount: Int,
    val downloads: Int,
) {
    /** "v7.52.1" -> "7.52.1"; null when the tag carries no version number. */
    val version: String?
        get() = versionFromTag(tag)
}
