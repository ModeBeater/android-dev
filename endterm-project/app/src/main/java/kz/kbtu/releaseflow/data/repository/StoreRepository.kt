package kz.kbtu.releaseflow.data.repository

import kz.kbtu.releaseflow.data.remote.GitHubApi
import kz.kbtu.releaseflow.data.remote.GitHubReleaseDto
import kz.kbtu.releaseflow.data.remote.ItunesApi
import kz.kbtu.releaseflow.data.remote.ItunesAppDto
import kz.kbtu.releaseflow.domain.model.GitHubRelease
import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.StoreListing
import java.time.Instant
import java.time.format.DateTimeParseException

/** Result of one App Store lookup: the store knows the bundle id, or it does not. */
sealed interface StoreLookup {
    data class Found(val listing: StoreListing) : StoreLookup
    data object NotFound : StoreLookup
}

/** What a refresh changed, so the sync worker knows whether to notify. */
sealed interface StoreRefresh {
    data class NewVersion(val listing: StoreListing, val previousVersion: String) : StoreRefresh
    data class Unchanged(val listing: StoreListing) : StoreRefresh
    data class FirstSeen(val listing: StoreListing) : StoreRefresh
    data object NotFound : StoreRefresh
}

/**
 * The external APIs behind one interface. Network errors are *not* swallowed here: an
 * `IOException` (no network) or `retrofit2.HttpException` (4xx/5xx) propagates and the
 * ViewModel or the worker decides how to show or retry it.
 */
interface StoreRepository {
    /** Asks the App Store about a bundle id; nothing is stored. */
    suspend fun lookup(bundleId: String, country: String): StoreLookup

    /** Asks the App Store about an app and caches the answer in the database. */
    suspend fun refreshListing(app: MobileApp, country: String): StoreRefresh

    /** Releases of a public GitHub repository ("owner/name"), newest first. */
    suspend fun gitHubReleases(repo: String): List<GitHubRelease>
}

class NetworkStoreRepository(
    private val itunesApi: ItunesApi,
    private val gitHubApi: GitHubApi,
    private val releaseRepository: ReleaseRepository,
) : StoreRepository {

    override suspend fun lookup(bundleId: String, country: String): StoreLookup {
        val response = itunesApi.lookup(bundleId.trim(), country)
        val dto = response.results.firstOrNull() ?: return StoreLookup.NotFound
        return StoreLookup.Found(dto.toListing(appId = 0, previousVersion = null, fetchedAt = System.currentTimeMillis()))
    }

    override suspend fun refreshListing(app: MobileApp, country: String): StoreRefresh {
        if (app.bundleId.isBlank()) return StoreRefresh.NotFound
        val dto = itunesApi.lookup(app.bundleId, country).results.firstOrNull() ?: return StoreRefresh.NotFound
        val previous = releaseRepository.listing(app.id)
        val listing = dto.toListing(appId = app.id, previousVersion = previous?.version, fetchedAt = System.currentTimeMillis())
        releaseRepository.saveListing(listing)
        return when {
            previous == null -> StoreRefresh.FirstSeen(listing)
            previous.version != listing.version -> StoreRefresh.NewVersion(listing, previous.version)
            else -> StoreRefresh.Unchanged(listing)
        }
    }

    override suspend fun gitHubReleases(repo: String): List<GitHubRelease> {
        val (owner, name) = repo.trim().trim('/').split('/').also { require(it.size == 2) { "Repository must look like owner/name" } }
        return gitHubApi.releases(owner, name)
            .map { it.toDomain() }
            .sortedByDescending { it.publishedAt ?: 0L }
    }
}

// ---- DTO -> domain ----------------------------------------------------------------

private fun ItunesAppDto.toListing(appId: Int, previousVersion: String?, fetchedAt: Long) = StoreListing(
    appId = appId,
    storeAppId = trackId,
    name = trackName,
    bundleId = bundleId,
    version = version,
    releaseNotes = releaseNotes?.trim()?.takeIf { it.isNotEmpty() },
    releasedAt = currentVersionReleaseDate?.let { parseInstant(it) },
    rating = averageUserRating,
    ratingCount = userRatingCount,
    artworkUrl = artworkUrl512 ?: artworkUrl100,
    storeUrl = trackViewUrl,
    sellerName = sellerName,
    minimumOs = minimumOsVersion,
    fetchedAt = fetchedAt,
    previousVersion = previousVersion,
)

private fun GitHubReleaseDto.toDomain() = GitHubRelease(
    id = id,
    tag = tagName,
    name = name?.takeIf { it.isNotBlank() && it != tagName },
    body = body?.trim()?.takeIf { it.isNotEmpty() },
    prerelease = prerelease,
    draft = draft,
    publishedAt = publishedAt?.let { parseInstant(it) },
    author = author?.login,
    url = htmlUrl,
    assetCount = assets.size,
    downloads = assets.sumOf { it.downloadCount },
)

/** "2026-10-14T16:41:03Z" -> epoch millis; null when the format is unexpected. */
private fun parseInstant(text: String): Long? = try {
    Instant.parse(text).toEpochMilli()
} catch (e: DateTimeParseException) {
    null
}
