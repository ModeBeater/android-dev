package kz.kbtu.releaseflow.data.repository

import kotlinx.coroutines.flow.Flow
import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.StoreListing
import kz.kbtu.releaseflow.domain.model.TrackAction

/** Immutable snapshot of everything the data layer holds. */
data class ReleaseData(
    val apps: List<MobileApp>,
    val releases: List<Release>,               // newest first
    val listings: Map<Int, StoreListing>,      // by app id
)

/**
 * The data layer's contract (Repository pattern). The ViewModels depend on this interface,
 * not on Room: [OfflineReleaseRepository] is the production implementation, a fake can
 * replace it in tests. Every write is `suspend` (database I/O) and is reflected back
 * through [data], which the UI collects.
 */
interface ReleaseRepository {

    /** Live view of the database; emits after every change. */
    val data: Flow<ReleaseData>

    // ---- read once ------------------------------------------------------------
    suspend fun apps(): List<MobileApp>
    suspend fun app(id: Int): MobileApp?
    suspend fun release(id: Int): Release?
    suspend fun listing(appId: Int): StoreListing?

    // ---- apps (CRUD) ------------------------------------------------------------
    suspend fun addApp(name: String, bundleId: String, packageName: String, githubRepo: String?): Int
    suspend fun updateGithubRepo(appId: Int, repo: String?)
    suspend fun deleteApp(appId: Int)
    suspend fun saveListing(listing: StoreListing)

    // ---- releases ---------------------------------------------------------------
    suspend fun createRelease(appId: Int, version: String, builds: Map<Platform, String>, plannedAt: Long?, notes: String): Int
    suspend fun updateNotes(releaseId: Int, notes: String)
    suspend fun toggleChecklistItem(releaseId: Int, itemId: Int)
    suspend fun addChecklistItem(releaseId: Int, title: String, platform: Platform?)

    // ---- workflow ----------------------------------------------------------------
    /** Synchronous workflow step (start build, submit, release, rollout…); returns the new stage. */
    suspend fun applyAction(releaseId: Int, platform: Platform, action: TrackAction, percent: Int? = null): ReleaseStage

    /** Step that asks CI or the store — suspends while the request is in flight. */
    suspend fun applyExternalAction(releaseId: Int, platform: Platform, action: TrackAction): ReleaseStage

    // ---- lifecycle ---------------------------------------------------------------
    /** Inserts the demo data when the database is empty, then opens [data]. */
    suspend fun seedIfEmpty()

    /** Drops everything and inserts the demo data again. */
    suspend fun resetToSample()
}
