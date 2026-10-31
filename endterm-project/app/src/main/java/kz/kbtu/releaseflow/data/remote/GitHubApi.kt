package kz.kbtu.releaseflow.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/*
 * GitHub REST API — releases of a public repository, no token needed (60 requests/hour).
 *   GET https://api.github.com/repos/signalapp/Signal-Android/releases?per_page=30
 */
interface GitHubApi {
    @GET("repos/{owner}/{repo}/releases")
    suspend fun releases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
    ): List<GitHubReleaseDto>
}

/** GitHub uses snake_case; `@SerialName` maps it to Kotlin names. */
@Serializable
data class GitHubReleaseDto(
    val id: Long,
    @SerialName("tag_name") val tagName: String,
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    val author: GitHubUserDto? = null,
    val assets: List<GitHubAssetDto> = emptyList(),
)

@Serializable
data class GitHubUserDto(val login: String)

@Serializable
data class GitHubAssetDto(
    val name: String,
    val size: Long = 0,
    @SerialName("download_count") val downloadCount: Int = 0,
    @SerialName("browser_download_url") val downloadUrl: String? = null,
)
