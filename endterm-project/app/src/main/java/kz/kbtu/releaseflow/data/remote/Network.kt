package kz.kbtu.releaseflow.data.remote

import kotlinx.serialization.json.Json
import kz.kbtu.releaseflow.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * One OkHttp client shared by both REST APIs and the WebSocket (connection pool, timeouts,
 * logging), and one Retrofit per base URL. Built once by the AppContainer.
 */
object Network {

    val json: Json = Json {
        ignoreUnknownKeys = true     // the APIs send far more fields than the DTOs declare
        coerceInputValues = true     // null for a non-null field with a default → use the default
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS)   // keeps the WebSocket alive
        .addInterceptor { chain ->
            // GitHub requires a User-Agent and prefers an explicit API media type.
            val request = chain.request().newBuilder()
                .header("User-Agent", "ReleaseFlow/2.0 (KBTU Mobile Programming)")
                .header("Accept", "application/json, application/vnd.github+json")
                .build()
            chain.proceed(request)
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val itunesApi: ItunesApi by lazy { retrofit("https://itunes.apple.com/").create(ItunesApi::class.java) }
    val gitHubApi: GitHubApi by lazy { retrofit("https://api.github.com/").create(GitHubApi::class.java) }
}
