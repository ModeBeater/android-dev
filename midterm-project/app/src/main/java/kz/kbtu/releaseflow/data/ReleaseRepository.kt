package kz.kbtu.releaseflow.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kz.kbtu.releaseflow.domain.SampleData
import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.service.ReleaseService
import kz.kbtu.releaseflow.domain.service.SimulatedStoreGateway
import kz.kbtu.releaseflow.domain.service.StoreGateway

/** Immutable snapshot of everything the data layer holds. */
data class ReleaseData(
    val apps: List<MobileApp>,
    val releases: List<Release>,
    val version: Long,
)

/**
 * Single source of truth (data layer). In-memory for now: owns the domain objects and
 * publishes a snapshot after every command. Room (HW3) and the real store/CI APIs (HW4)
 * plug in here without touching the ViewModel or the UI.
 *
 * `version` is bumped on every change because `Release` objects are mutable — two
 * snapshots could otherwise be `equals` and StateFlow would swallow the update.
 */
class ReleaseRepository private constructor(
    private val service: ReleaseService = ReleaseService(),
    private val gateway: StoreGateway = SimulatedStoreGateway(),
) {
    private val apps = SampleData.apps.toMutableList()
    private val releases = SampleData.releases(service, System.currentTimeMillis()).toMutableList()
    private var nextAppId = (apps.maxOfOrNull { it.id } ?: 0) + 1
    private var nextReleaseId = (releases.maxOfOrNull { it.id } ?: 0) + 1
    private var version = 0L

    private val _data = MutableStateFlow(snapshot())
    val data: StateFlow<ReleaseData> = _data.asStateFlow()

    // ---- queries -----------------------------------------------------------

    fun app(id: Int): MobileApp? = apps.firstOrNull { it.id == id }
    fun release(id: Int): Release? = releases.firstOrNull { it.id == id }

    // ---- apps --------------------------------------------------------------

    fun addApp(name: String, bundleId: String, packageName: String): MobileApp {
        require(name.isNotBlank()) { "App name is required" }
        require(apps.none { it.name.equals(name.trim(), ignoreCase = true) }) { "An app with this name already exists" }
        val app = MobileApp(nextAppId++, name.trim(), bundleId.trim(), packageName.trim())
        apps += app
        publish()
        return app
    }

    // ---- releases ----------------------------------------------------------

    fun createRelease(appId: Int, version: String, builds: Map<Platform, String>, plannedAt: Long?, notes: String): Release {
        requireNotNull(app(appId)) { "Unknown app" }
        require(releases.none { it.appId == appId && it.version == version }) { "Version $version already exists for this app" }
        val release = service.createRelease(nextReleaseId++, appId, version, builds, plannedAt, notes)
        releases += release
        publish()
        return release
    }

    fun updateNotes(releaseId: Int, notes: String) {
        release(releaseId)?.notes = notes
        publish()
    }

    fun toggleChecklistItem(releaseId: Int, itemId: Int) {
        release(releaseId)?.toggleChecklistItem(itemId)
        publish()
    }

    fun addChecklistItem(releaseId: Int, title: String, platform: Platform?) {
        release(releaseId)?.addChecklistItem(title, platform)
        publish()
    }

    /** Synchronous workflow step (start build, submit, release, rollout…). */
    fun applyAction(releaseId: Int, platform: Platform, action: TrackAction, percent: Int? = null) {
        val release = requireNotNull(release(releaseId)) { "Unknown release" }
        service.apply(release, platform, action, percent)
        publish()
    }

    /** Step that asks CI or the store — suspends while the (simulated) request is in flight. */
    suspend fun applyExternalAction(releaseId: Int, platform: Platform, action: TrackAction): ReleaseStage {
        val release = requireNotNull(release(releaseId)) { "Unknown release" }
        val stage = service.applyExternal(release, platform, action, gateway)
        publish()
        return stage
    }

    // ---- helpers -----------------------------------------------------------

    private fun snapshot() = ReleaseData(apps = apps.toList(), releases = releases.toList(), version = version)

    private fun publish() {
        version++
        _data.value = snapshot()
    }

    companion object {
        /** One repository per process — the simplest dependency injection for this project. */
        val instance: ReleaseRepository by lazy { ReleaseRepository() }
    }
}
