package kz.kbtu.releaseflow.domain.service

import kotlinx.coroutines.delay
import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.PlatformTrack
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.StageEvent
import kz.kbtu.releaseflow.domain.model.TrackAction

/**
 * The outside world: CI server and the two stores' review queues.
 *
 * Still simulated in this version: neither App Store Connect nor the Play Console has a
 * public read API without a developer account. The live store *listing* (version, rating,
 * release notes) is real, see `StoreRepository`.
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

/** The outcome of a workflow action: the stage the track moves to and an optional history note. */
data class Transition(val stage: ReleaseStage, val note: String? = null)

/**
 * Business rules of the release workflow. Pure Kotlin, no Android, no mutation: every
 * function returns a value; the repository persists it and Room's Flow delivers it back.
 */
class ReleaseService(private val clock: () -> Long = System::currentTimeMillis) {

    /** Creates the standard submission checklist for the chosen platforms (ids are assigned by the database). */
    fun defaultChecklist(platforms: Set<Platform>): List<ChecklistItem> {
        fun item(title: String, platform: Platform? = null, guideline: String? = null) =
            ChecklistItem(0, title, platform, guideline)

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

    /** A new release in Draft on every chosen platform, not yet stored (id 0). */
    fun newRelease(
        appId: Int,
        version: String,
        builds: Map<Platform, String>,
        plannedAt: Long?,
        notes: String,
        createdAt: Long = clock(),
    ): Release {
        require(version.matches(Regex("""\d+\.\d+(\.\d+)?"""))) { "Version must look like 1.2 or 1.2.3" }
        require(builds.isNotEmpty()) { "Choose at least one platform" }
        val tracks = builds.map { (platform, build) ->
            PlatformTrack(0, platform, build, listOf(StageEvent(ReleaseStage.Draft, createdAt)))
        }
        return Release(0, appId, version, createdAt, plannedAt, notes, tracks, defaultChecklist(builds.keys))
    }

    /**
     * Decides where a synchronous action takes the track. External actions (CheckCi /
     * CheckReview) go through [askGateway] because they suspend.
     */
    fun transition(release: Release, platform: Platform, action: TrackAction, percent: Int? = null): Transition {
        val track = release.track(platform) ?: throw IllegalArgumentException("No ${platform.label} track")
        check(action in track.actions) { "${action.label} is not allowed while ${track.stage.label}" }
        return when (action) {
            TrackAction.StartBuild -> Transition(ReleaseStage.Building("pipeline-${release.id}${platform.ordinal}"))
            TrackAction.Submit -> Transition(ReleaseStage.Submitted)
            TrackAction.Resubmit -> Transition(ReleaseStage.Submitted, note = "resubmitted after rejection")
            TrackAction.Release -> Transition(ReleaseStage.Live)
            TrackAction.StartRollout -> Transition(ReleaseStage.RollingOut(10))
            TrackAction.IncreaseRollout -> {
                val current = (track.stage as ReleaseStage.RollingOut).percent
                val target = requireNotNull(percent) { "Rollout percentage is required" }
                require(target in (current + 1)..100) { "Rollout can only go up, from ${current + 1} to 100" }
                Transition(if (target == 100) ReleaseStage.Live else ReleaseStage.RollingOut(target))
            }
            TrackAction.Halt -> Transition(ReleaseStage.Halted((track.stage as ReleaseStage.RollingOut).percent))
            TrackAction.Resume -> Transition(ReleaseStage.RollingOut((track.stage as ReleaseStage.Halted).percent))
            TrackAction.CheckCi, TrackAction.CheckReview ->
                throw IllegalStateException("${action.label} must be run through the gateway")
        }
    }

    /** Asks CI or the store (suspending) and returns their answer; unchanged stage means "still waiting". */
    suspend fun askGateway(release: Release, platform: Platform, action: TrackAction, gateway: StoreGateway): ReleaseStage {
        val track = release.track(platform) ?: throw IllegalArgumentException("No ${platform.label} track")
        check(action in track.actions) { "${action.label} is not allowed while ${track.stage.label}" }
        return when (action) {
            TrackAction.CheckCi -> gateway.checkCi(release, track)
            TrackAction.CheckReview -> gateway.checkReview(release, track)
            else -> throw IllegalStateException("${action.label} is not an external action")
        }
    }

    fun now(): Long = clock()
}
