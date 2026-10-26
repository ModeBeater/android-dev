package kz.kbtu.releaseflow.ui

import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.StageEvent
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.service.ReleaseStatistics

// ---------------------------------------------------------------------------
// UI models — immutable snapshots of the mutable domain objects
// ---------------------------------------------------------------------------

/** One-shot message for the snackbar; the id lets the UI acknowledge exactly this message. */
data class UiMessage(val id: Long, val text: String)

data class TrackUi(
    val releaseId: Int,
    val platform: Platform,
    val buildNumber: String,
    val stage: ReleaseStage,
    val enteredAt: Long,
    val history: List<StageEvent>,
    val actions: List<TrackAction>,
) {
    /** Key used in [ReleaseUiState.processing]. */
    val key: String
        get() = "$releaseId:${platform.name}"
}

data class ReleaseUi(
    val id: Int,
    val appId: Int,
    val appName: String,
    val version: String,
    val createdAt: Long,
    val plannedAt: Long?,
    val notes: String,
    val tracks: List<TrackUi>,
    val checklist: List<ChecklistItem>,
) {
    val checklistDone: Int
        get() = checklist.count { it.done }

    val checklistTotal: Int
        get() = checklist.size

    val isLiveEverywhere: Boolean
        get() = tracks.all { it.stage == ReleaseStage.Live }

    val hasProblem: Boolean
        get() = tracks.any { it.stage.isProblem }

    fun track(platform: Platform): TrackUi? = tracks.firstOrNull { it.platform == platform }
}

data class AppUi(
    val id: Int,
    val name: String,
    val bundleId: String,
    val packageName: String,
    val monogram: String,
    val releases: List<ReleaseUi>,
) {
    val latest: ReleaseUi?
        get() = releases.firstOrNull()
}

/** Something the release manager should look at today. */
data class AttentionItem(
    val releaseId: Int,
    val title: String,
    val detail: String,
    val severity: Severity,
) {
    enum class Severity { Problem, Waiting, Upcoming }
}

/** One row of the activity feed. */
data class ActivityItem(
    val releaseId: Int,
    val appName: String,
    val version: String,
    val platform: Platform,
    val stage: ReleaseStage,
    val at: Long,
)

// ---------------------------------------------------------------------------
// Screen state
// ---------------------------------------------------------------------------

/**
 * Everything the screens need, in one immutable data class owned by [ReleaseViewModel].
 * Derived values are getters, so they are always consistent with the raw fields.
 */
data class ReleaseUiState(
    val apps: List<AppUi> = emptyList(),
    val releases: List<ReleaseUi> = emptyList(),       // newest first
    val stats: ReleaseStatistics? = null,
    val processing: Set<String> = emptySet(),           // TrackUi.key of tracks waiting for CI / store
    val message: UiMessage? = null,
    val dataVersion: Long = 0,
    val now: Long = System.currentTimeMillis(),
) {
    fun app(appId: Int): AppUi? = apps.firstOrNull { it.id == appId }
    fun release(releaseId: Int): ReleaseUi? = releases.firstOrNull { it.id == releaseId }
    fun isProcessing(track: TrackUi): Boolean = track.key in processing

    private val allTracks: List<Pair<ReleaseUi, TrackUi>>
        get() = releases.flatMap { release -> release.tracks.map { release to it } }

    // ---- dashboard numbers ------------------------------------------------

    val tracksInReview: Int
        get() = allTracks.count { (_, t) -> t.stage == ReleaseStage.Submitted || t.stage == ReleaseStage.InReview }

    val tracksRollingOut: Int
        get() = allTracks.count { (_, t) -> t.stage is ReleaseStage.RollingOut }

    val problems: Int
        get() = allTracks.count { (_, t) -> t.stage.isProblem }

    val liveLast30Days: Int
        get() = allTracks.count { (_, t) ->
            t.history.any { it.stage == ReleaseStage.Live && it.at >= now - 30L * 24 * 60 * 60 * 1000 }
        }

    /** Problems first, then long waits, then releases due soon with an unfinished checklist. */
    val attention: List<AttentionItem>
        get() {
            val items = mutableListOf<AttentionItem>()
            for ((release, track) in allTracks) {
                val title = "${release.appName} ${release.version} · ${track.platform.label}"
                when (val stage = track.stage) {
                    is ReleaseStage.Rejected -> items += AttentionItem(
                        release.id, title, "Rejected: ${stage.reason}", AttentionItem.Severity.Problem,
                    )
                    is ReleaseStage.Halted -> items += AttentionItem(
                        release.id, title, "Rollout halted at ${stage.percent} %", AttentionItem.Severity.Problem,
                    )
                    ReleaseStage.Submitted, ReleaseStage.InReview -> {
                        val waitingMs = now - track.enteredAt
                        if (waitingMs > 2L * 24 * 60 * 60 * 1000) {
                            items += AttentionItem(
                                release.id, title, "In review for ${waitingMs / (24 * 60 * 60 * 1000)} days", AttentionItem.Severity.Waiting,
                            )
                        }
                    }
                    else -> Unit
                }
            }
            for (release in releases) {
                val planned = release.plannedAt ?: continue
                val daysLeft = (planned - now) / (24 * 60 * 60 * 1000)
                if (daysLeft in 0..7 && release.checklistDone < release.checklistTotal) {
                    items += AttentionItem(
                        release.id,
                        "${release.appName} ${release.version}",
                        "Planned in $daysLeft d, checklist ${release.checklistDone}/${release.checklistTotal}",
                        AttentionItem.Severity.Upcoming,
                    )
                }
            }
            return items.sortedBy { it.severity.ordinal }
        }

    /** Releases with a planned date that has not passed, soonest first. */
    val upcoming: List<ReleaseUi>
        get() = releases.filter { it.plannedAt != null && it.plannedAt >= now - 24 * 60 * 60 * 1000 && !it.isLiveEverywhere }
            .sortedBy { it.plannedAt }

    /** The last stage changes across all releases, newest first. */
    val recentActivity: List<ActivityItem>
        get() = allTracks
            .flatMap { (release, track) ->
                track.history.drop(1).map { event ->
                    ActivityItem(release.id, release.appName, release.version, track.platform, event.stage, event.at)
                }
            }
            .sortedByDescending { it.at }
            .take(8)
}
