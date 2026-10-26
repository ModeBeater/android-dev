package kz.kbtu.releaseflow.domain.service

import kotlinx.coroutines.delay
import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.PlatformTrack
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.TrackAction

/**
 * The outside world: CI server and the two stores.
 *
 * In this version it is simulated; in the second half of the semester the same interface
 * is implemented with Retrofit (GitLab API, iTunes Lookup, App Store Connect API) and
 * polled by WorkManager. The ViewModel and the UI do not change.
 */
interface StoreGateway {
    suspend fun checkCi(release: Release, track: PlatformTrack): ReleaseStage
    suspend fun checkReview(release: Release, track: PlatformTrack): ReleaseStage
}

/**
 * Deterministic simulation: CI always succeeds; the "reviewer" rejects the build when a
 * checklist item that maps to a store guideline is still unchecked — so the checklist is
 * not decoration, it decides the outcome of the review.
 */
class SimulatedStoreGateway(private val latencyMs: Long = 1_200) : StoreGateway {

    override suspend fun checkCi(release: Release, track: PlatformTrack): ReleaseStage {
        delay(latencyMs)
        return ReleaseStage.Testing(track.platform.testingChannel)
    }

    override suspend fun checkReview(release: Release, track: PlatformTrack): ReleaseStage {
        delay(latencyMs)
        return when (track.stage) {
            ReleaseStage.Submitted -> ReleaseStage.InReview
            ReleaseStage.InReview -> {
                val missing = release.checklistFor(track.platform).firstOrNull { !it.done && it.guideline != null }
                if (missing == null) {
                    ReleaseStage.Approved
                } else {
                    ReleaseStage.Rejected(reason = "${missing.title} is missing", guideline = missing.guideline)
                }
            }
            else -> track.stage
        }
    }
}

/** Business rules of the release workflow. Pure Kotlin, no Android. */
class ReleaseService(private val clock: () -> Long = System::currentTimeMillis) {

    /** Creates the standard submission checklist for the chosen platforms. */
    fun defaultChecklist(platforms: Set<Platform>): List<ChecklistItem> {
        var id = 0
        fun item(title: String, platform: Platform? = null, guideline: String? = null) =
            ChecklistItem(++id, title, platform, guideline)

        val items = mutableListOf(
            item("Version bumped and git tag created"),
            item("Release notes written (EN / RU)"),
            item("QA sign-off on the release build"),
        )
        if (Platform.IOS in platforms) {
            items += item("App Store screenshots updated", Platform.IOS, guideline = "2.3.3 Accurate metadata")
            items += item("Privacy nutrition labels reviewed", Platform.IOS, guideline = "5.1.1 Data collection")
            items += item("Build uploaded to TestFlight", Platform.IOS)
        }
        if (Platform.ANDROID in platforms) {
            items += item("Play Store screenshots updated", Platform.ANDROID, guideline = "Store listing policy")
            items += item("Data safety form reviewed", Platform.ANDROID, guideline = "Data safety policy")
            items += item("AAB uploaded to Internal testing", Platform.ANDROID)
        }
        return items
    }

    fun createRelease(
        id: Int,
        appId: Int,
        version: String,
        builds: Map<Platform, String>,
        plannedAt: Long?,
        notes: String,
        createdAt: Long = clock(),
    ): Release {
        require(version.matches(Regex("""\d+\.\d+(\.\d+)?"""))) { "Version must look like 1.2 or 1.2.3" }
        require(builds.isNotEmpty()) { "Choose at least one platform" }
        val tracks = builds.map { (platform, build) -> PlatformTrack(platform, build, ReleaseStage.Draft, createdAt) }
        return Release(id, appId, version, createdAt, plannedAt, notes, tracks, defaultChecklist(builds.keys))
    }

    /**
     * Applies a synchronous action. External actions (CheckCi / CheckReview) go through
     * [applyExternal] because they suspend.
     */
    fun apply(release: Release, platform: Platform, action: TrackAction, percent: Int? = null) {
        val track = release.track(platform) ?: throw IllegalArgumentException("No ${platform.label} track")
        check(action in track.actions) { "${action.label} is not allowed while ${track.stage.label}" }
        val now = clock()
        when (action) {
            TrackAction.StartBuild -> track.advance(ReleaseStage.Building("pipeline-${release.id}${platform.ordinal}"), now)
            TrackAction.Submit -> track.advance(ReleaseStage.Submitted, now)
            TrackAction.Resubmit -> track.advance(ReleaseStage.Submitted, now, note = "resubmitted after rejection")
            TrackAction.Release -> track.advance(ReleaseStage.Live, now)
            TrackAction.StartRollout -> track.advance(ReleaseStage.RollingOut(10), now)
            TrackAction.IncreaseRollout -> {
                val current = (track.stage as ReleaseStage.RollingOut).percent
                val target = requireNotNull(percent) { "Rollout percentage is required" }
                require(target in (current + 1)..100) { "Rollout can only go up, from ${current + 1} to 100" }
                track.advance(if (target == 100) ReleaseStage.Live else ReleaseStage.RollingOut(target), now)
            }
            TrackAction.Halt -> {
                val current = (track.stage as ReleaseStage.RollingOut).percent
                track.advance(ReleaseStage.Halted(current), now)
            }
            TrackAction.Resume -> {
                val current = (track.stage as ReleaseStage.Halted).percent
                track.advance(ReleaseStage.RollingOut(current), now)
            }
            TrackAction.CheckCi, TrackAction.CheckReview ->
                throw IllegalStateException("${action.label} must be run through the gateway")
        }
    }

    /** Asks CI or the store (suspending) and records whatever they answered. */
    suspend fun applyExternal(release: Release, platform: Platform, action: TrackAction, gateway: StoreGateway): ReleaseStage {
        val track = release.track(platform) ?: throw IllegalArgumentException("No ${platform.label} track")
        check(action in track.actions) { "${action.label} is not allowed while ${track.stage.label}" }
        val next = when (action) {
            TrackAction.CheckCi -> gateway.checkCi(release, track)
            TrackAction.CheckReview -> gateway.checkReview(release, track)
            else -> throw IllegalStateException("${action.label} is not an external action")
        }
        if (next != track.stage) track.advance(next, clock())
        return next
    }
}
