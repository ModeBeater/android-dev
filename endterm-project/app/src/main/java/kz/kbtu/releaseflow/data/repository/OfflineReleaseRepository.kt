package kz.kbtu.releaseflow.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kz.kbtu.releaseflow.data.local.AppEntity
import kz.kbtu.releaseflow.data.local.ChecklistItemEntity
import kz.kbtu.releaseflow.data.local.ReleaseDao
import kz.kbtu.releaseflow.data.local.toDomain
import kz.kbtu.releaseflow.data.local.toEntity
import kz.kbtu.releaseflow.domain.SampleData
import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.StageEvent
import kz.kbtu.releaseflow.domain.model.StoreListing
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.service.ReleaseService
import kz.kbtu.releaseflow.domain.service.StoreGateway

/**
 * Room-backed implementation of [ReleaseRepository]: the database is the single source of
 * truth. A command loads the aggregate, lets the domain service decide, writes the result,
 * and Room's `Flow` pushes the new snapshot to every collector — the ViewModel never has to
 * "refresh" anything by hand.
 */
class OfflineReleaseRepository(
    private val dao: ReleaseDao,
    private val service: ReleaseService,
    private val gateway: StoreGateway,
) : ReleaseRepository {

    /** Becomes true once [seedIfEmpty] ran, so the UI never sees an empty database before the seed. */
    private val ready = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val data: Flow<ReleaseData> = ready
        .filter { it }
        .flatMapLatest {
            combine(dao.observeApps(), dao.observeReleases(), dao.observeListings()) { apps, releases, listings ->
                ReleaseData(
                    apps = apps.map { it.toDomain() },
                    releases = releases.map { it.toDomain() },
                    listings = listings.associate { it.appId to it.toDomain() },
                )
            }
        }

    // ---- read once ------------------------------------------------------------

    override suspend fun apps(): List<MobileApp> = dao.apps().map { it.toDomain() }
    override suspend fun app(id: Int): MobileApp? = dao.app(id)?.toDomain()
    override suspend fun release(id: Int): Release? = dao.release(id)?.toDomain()
    override suspend fun listing(appId: Int): StoreListing? = dao.listing(appId)?.toDomain()

    // ---- apps -------------------------------------------------------------------

    override suspend fun addApp(name: String, bundleId: String, packageName: String, githubRepo: String?): Int {
        require(name.isNotBlank()) { "App name is required" }
        require(!dao.appNameExists(name.trim())) { "An app with this name already exists" }
        val repo = githubRepo?.trim()?.removePrefix("https://github.com/")?.trim('/')?.takeIf { it.isNotBlank() }
        require(repo == null || repo.matches(Regex("""[\w.-]+/[\w.-]+"""))) { "GitHub repository must look like owner/name" }
        return dao.insertApp(AppEntity(name = name.trim(), bundleId = bundleId.trim(), packageName = packageName.trim(), githubRepo = repo)).toInt()
    }

    override suspend fun updateGithubRepo(appId: Int, repo: String?) {
        val clean = repo?.trim()?.removePrefix("https://github.com/")?.trim('/')?.takeIf { it.isNotBlank() }
        require(clean == null || clean.matches(Regex("""[\w.-]+/[\w.-]+"""))) { "GitHub repository must look like owner/name" }
        dao.updateGithubRepo(appId, clean)
    }

    override suspend fun deleteApp(appId: Int) {
        val app = dao.app(appId) ?: return
        dao.deleteApp(app)   // ON DELETE CASCADE removes its releases, tracks, events, checklist, listing
    }

    override suspend fun saveListing(listing: StoreListing) = dao.upsertListing(listing.toEntity())

    // ---- releases ------------------------------------------------------------------

    override suspend fun createRelease(appId: Int, version: String, builds: Map<Platform, String>, plannedAt: Long?, notes: String): Int {
        requireNotNull(dao.app(appId)) { "Unknown app" }
        require(!dao.versionExists(appId, version)) { "Version $version already exists for this app" }
        val release = service.newRelease(appId, version, builds, plannedAt, notes)
        return dao.insertReleaseGraph(release)
    }

    override suspend fun updateNotes(releaseId: Int, notes: String) = dao.updateNotes(releaseId, notes)

    override suspend fun toggleChecklistItem(releaseId: Int, itemId: Int) {
        val changed = dao.toggleChecklistItem(itemId)
        require(changed == 1) { "Unknown checklist item $itemId" }
    }

    override suspend fun addChecklistItem(releaseId: Int, title: String, platform: Platform?) {
        require(title.isNotBlank()) { "Checklist item cannot be empty" }
        dao.insertChecklistItem(
            ChecklistItemEntity(releaseId = releaseId, title = title.trim(), platform = platform?.name, guideline = null, done = false),
        )
    }

    // ---- workflow -------------------------------------------------------------------

    override suspend fun applyAction(releaseId: Int, platform: Platform, action: TrackAction, percent: Int?): ReleaseStage {
        val release = requireNotNull(release(releaseId)) { "Unknown release" }
        val transition = service.transition(release, platform, action, percent)
        val track = requireNotNull(release.track(platform))
        dao.insertEvent(StageEvent(transition.stage, service.now(), transition.note).toEntity(track.id))
        return transition.stage
    }

    override suspend fun applyExternalAction(releaseId: Int, platform: Platform, action: TrackAction): ReleaseStage {
        val release = requireNotNull(release(releaseId)) { "Unknown release" }
        val next = service.askGateway(release, platform, action, gateway)
        val track = requireNotNull(release.track(platform))
        if (next != track.stage) dao.insertEvent(StageEvent(next, service.now()).toEntity(track.id))
        return next
    }

    // ---- lifecycle --------------------------------------------------------------------

    override suspend fun seedIfEmpty() {
        if (dao.appCount() == 0) insertSample()
        ready.value = true
    }

    override suspend fun resetToSample() {
        dao.deleteAllApps()   // cascades to every other table
        insertSample()
    }

    private suspend fun insertSample() {
        val now = service.now()
        val ids = SampleData.apps.associate { app -> app.name to dao.insertApp(app.toEntity()).toInt() }
        for ((appName, releases) in SampleData.releases(service, now)) {
            val appId = ids.getValue(appName)
            for (release in releases) dao.insertReleaseGraph(release.copy(appId = appId))
        }
    }
}
