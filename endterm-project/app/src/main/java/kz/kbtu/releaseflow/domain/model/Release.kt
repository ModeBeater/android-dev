package kz.kbtu.releaseflow.domain.model

/**
 * An app the team ships; identified in the stores by bundle id / package name.
 * `githubRepo` ("owner/name") links the app to the repository whose GitHub releases
 * can be imported; `id` is 0 until the database assigns one.
 */
data class MobileApp(
    val id: Int,
    val name: String,
    val bundleId: String,
    val packageName: String,
    val githubRepo: String? = null,
) {
    /** Two-letter monogram for the icon placeholder. */
    val monogram: String
        get() = name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
}

/**
 * One item of the submission checklist. Items with a `guideline` are the ones the store
 * reviewer would reject the build for — the simulated review uses exactly that.
 */
data class ChecklistItem(
    val id: Int,
    val title: String,
    val platform: Platform?,          // null = applies to both platforms
    val guideline: String? = null,
    val done: Boolean = false,
)

/** One line of a track's history: the stage it entered and when. */
data class StageEvent(
    val stage: ReleaseStage,
    val at: Long,
    val note: String? = null,
)

/**
 * The journey of a release on one platform. Immutable: the current stage is simply the last
 * event of the history, and the database (not this class) is where history grows.
 *
 * In the midterm version this was a mutable class with `advance()`; with Room as the single
 * source of truth every object the UI sees is a snapshot of a database row, so the domain
 * model became value objects and the service returns *decisions* instead of mutating.
 */
data class PlatformTrack(
    val id: Int,
    val platform: Platform,
    val buildNumber: String,
    val history: List<StageEvent>,
) {
    init {
        require(history.isNotEmpty()) { "A track has at least its initial stage" }
    }

    val stage: ReleaseStage
        get() = history.last().stage

    val enteredAt: Long
        get() = history.last().at

    val actions: List<TrackAction>
        get() = allowedActions(platform, stage)

    /** A copy with one more stage in the history (used by the seed and by tests). */
    fun advanced(to: ReleaseStage, at: Long, note: String? = null): PlatformTrack =
        copy(history = history + StageEvent(to, at, note))

    /**
     * Review cycles: duration from each `Submitted` to the next `Approved` / `Rejected`.
     * A cycle that is still open (submitted, no verdict yet) is not counted.
     */
    fun reviewDurations(): List<Long> {
        val result = mutableListOf<Long>()
        var submittedAt: Long? = null
        for (event in history) {
            when (event.stage) {
                ReleaseStage.Submitted -> submittedAt = event.at
                ReleaseStage.Approved, is ReleaseStage.Rejected -> {
                    submittedAt?.let { result += event.at - it }
                    submittedAt = null
                }
                else -> Unit
            }
        }
        return result
    }

    fun rejections(): List<ReleaseStage.Rejected> = history.mapNotNull { it.stage as? ReleaseStage.Rejected }
}

/** A version of an app with one track per platform and a submission checklist. */
data class Release(
    val id: Int,
    val appId: Int,
    val version: String,
    val createdAt: Long,
    val plannedAt: Long?,
    val notes: String,
    val tracks: List<PlatformTrack>,
    val checklist: List<ChecklistItem>,
) {
    fun track(platform: Platform): PlatformTrack? = tracks.firstOrNull { it.platform == platform }

    fun checklistFor(platform: Platform): List<ChecklistItem> =
        checklist.filter { it.platform == null || it.platform == platform }

    fun isChecklistComplete(platform: Platform): Boolean = checklistFor(platform).all { it.done }

    val isLiveEverywhere: Boolean
        get() = tracks.all { it.stage == ReleaseStage.Live }

    /** Copy with the given tracks replaced (matched by platform). */
    fun withTrack(track: PlatformTrack): Release =
        copy(tracks = tracks.map { if (it.platform == track.platform) track else it })
}
