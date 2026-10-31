package kz.kbtu.releaseflow.data.remote

import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Query

/*
 * iTunes Search API — the public, key-less read API of the App Store.
 *   GET https://itunes.apple.com/lookup?bundleId=ph.telegra.Telegraph&country=kz
 * Answers {"resultCount": 1, "results": [ { ...one object per app... } ]}; an unknown bundle id
 * is not an error but resultCount = 0.
 */

/** Retrofit turns this interface into an HTTP client; `suspend` makes the call main-safe. */
interface ItunesApi {
    @GET("lookup")
    suspend fun lookup(
        @Query("bundleId") bundleId: String,
        @Query("country") country: String,
    ): ItunesLookupResponse
}

@Serializable
data class ItunesLookupResponse(
    val resultCount: Int = 0,
    val results: List<ItunesAppDto> = emptyList(),
)

/**
 * Only the fields the app uses; `ignoreUnknownKeys` in the Json configuration drops the
 * other ~50. Every optional field has a default so a missing key never fails the parse.
 */
@Serializable
data class ItunesAppDto(
    val trackId: Long,
    val trackName: String,
    val bundleId: String,
    val version: String,
    val releaseNotes: String? = null,
    val currentVersionReleaseDate: String? = null,   // ISO-8601, e.g. "2026-10-14T16:41:03Z"
    val averageUserRating: Double? = null,
    val userRatingCount: Int? = null,
    val artworkUrl100: String? = null,
    val artworkUrl512: String? = null,
    val trackViewUrl: String? = null,
    val sellerName: String? = null,
    val minimumOsVersion: String? = null,
    val primaryGenreName: String? = null,
)
