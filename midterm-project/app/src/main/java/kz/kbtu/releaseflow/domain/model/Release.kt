package kz.kbtu.releaseflow.domain.model

/** An app the team ships; identified in the stores by bundle id / package name. */
data class MobileApp(
    val id: Int,
    val name: String,
    val bundleId: String,
    val packageName: String,
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

/** The journey of a release on one platform. Mutable: only `advance` may change the history. */
class PlatformTrack(
    val platform: Platform,
    var buildNumber: String,
    initialStage: ReleaseStage,
    createdAt: Long,
) {
    private val _history = mutableListOf(StageEvent(initialStage, createdAt))

    val history: List<StageEvent>
        get() = _history.toList()

    val stage: ReleaseStage
        get() = _history.last().stage

    val enteredAt: Long
        get() = _history.last().at

    val actions: List<TrackAction>
        get() = allowedActions(platform, stage)

    fun advance(to: ReleaseStage, at: Long, note: String? = null) {
        _history += StageEvent(to, at, note)
    }

    /**
     * Review cycles: duration from each `Submitted` to the next `Approved` / `Rejected`.
     * A cycle that is still open (submitted, no verdict yet) is not counted.
     */
    fun reviewDurations(): List<Long> {
        val result = mutableListOf<Long>()
        var submittedAt: Long? = null
        for (event in _history) {
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

    fun rejections(): List<ReleaseStage.Rejected> = _history.mapNotNull { it.stage as? ReleaseStage.Rejected }
}

/** A version of an app with one track per platform and a submission checklist. */
class Release(
    val id: Int,
    val appId: Int,
    val version: String,
    val createdAt: Long,
    val plannedAt: Long?,
    var notes: String,
    tracks: List<PlatformTrack>,
    checklist: List<ChecklistItem>,
) {
    val tracks: Map<Platform, PlatformTrack> = tracks.associateBy { it.platform }

    private val _checklist = checklist.toMutableList()
    private var nextItemId = (checklist.maxOfOrNull { it.id } ?: 0) + 1

    val checklist: List<ChecklistItem>
        get() = _checklist.toList()

    fun track(platform: Platform): PlatformTrack? = tracks[platform]

    fun checklistFor(platform: Platform): List<ChecklistItem> =
        _checklist.filter { it.platform == null || it.platform == platform }

    fun isChecklistComplete(platform: Platform): Boolean = checklistFor(platform).all { it.done }

    fun toggleChecklistItem(itemId: Int) {
        val index = _checklist.indexOfFirst { it.id == itemId }
        require(index >= 0) { "Unknown checklist item $itemId" }
        val item = _checklist[index]
        _checklist[index] = item.copy(done = !item.done)
    }

    fun addChecklistItem(title: String, platform: Platform?) {
        require(title.isNotBlank()) { "Checklist item cannot be empty" }
        _checklist += ChecklistItem(nextItemId++, title.trim(), platform)
    }

    val isLiveEverywhere: Boolean
        get() = tracks.values.all { it.stage == ReleaseStage.Live }
}
