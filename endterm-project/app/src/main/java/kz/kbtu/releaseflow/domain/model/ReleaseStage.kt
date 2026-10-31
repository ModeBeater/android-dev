package kz.kbtu.releaseflow.domain.model

/** The two stores a mobile release goes to. */
enum class Platform(
    val label: String,
    val store: String,
    val testingChannel: String,
    val supportsStagedRollout: Boolean,
) {
    IOS("iOS", "App Store", "TestFlight", supportsStagedRollout = false),
    ANDROID("Android", "Google Play", "Internal testing", supportsStagedRollout = true),
}

/**
 * Where a platform track of a release currently is.
 *
 * A sealed class, not an enum: some stages carry data (pipeline id, rejection reason,
 * rollout percentage), and every `when` over it must be exhaustive — adding a stage
 * forces every screen and rule in the app to handle it.
 */
sealed class ReleaseStage {
    data object Draft : ReleaseStage()
    data class Building(val pipeline: String) : ReleaseStage()
    data class Testing(val channel: String) : ReleaseStage()
    data object Submitted : ReleaseStage()
    data object InReview : ReleaseStage()
    data class Rejected(val reason: String, val guideline: String?) : ReleaseStage()
    data object Approved : ReleaseStage()
    data class RollingOut(val percent: Int) : ReleaseStage()
    data object Live : ReleaseStage()
    data class Halted(val percent: Int) : ReleaseStage()

    val label: String
        get() = when (this) {
            Draft -> "Draft"
            is Building -> "Building"
            is Testing -> "Testing"
            Submitted -> "Submitted"
            InReview -> "In review"
            is Rejected -> "Rejected"
            Approved -> "Approved"
            is RollingOut -> "Rolling out $percent %"
            Live -> "Live"
            is Halted -> "Halted at $percent %"
        }

    /** Position on the 4-step timeline: 0 build · 1 test · 2 review · 3 release. */
    val step: Int
        get() = when (this) {
            Draft, is Building -> 0
            is Testing -> 1
            Submitted, InReview, is Rejected -> 2
            Approved, is RollingOut, Live, is Halted -> 3
        }

    /** Stages that need a human decision before the release can move on. */
    val isProblem: Boolean
        get() = this is Rejected || this is Halted

    /** Stages where the app is waiting for an external system (CI or the store). */
    val isWaitingExternal: Boolean
        get() = this is Building || this == Submitted || this == InReview
}

/** Everything a release manager can do with one platform track. */
enum class TrackAction(val label: String, val isExternal: Boolean = false) {
    StartBuild("Start build"),
    CheckCi("Check CI status", isExternal = true),
    Submit("Submit for review"),
    CheckReview("Check review status", isExternal = true),
    Resubmit("Resubmit"),
    Release("Release"),
    StartRollout("Start rollout (10 %)"),
    IncreaseRollout("Change rollout %"),
    Halt("Halt rollout"),
    Resume("Resume rollout"),
}

/** The workflow rules: which actions are valid from which stage on which platform. */
fun allowedActions(platform: Platform, stage: ReleaseStage): List<TrackAction> = when (stage) {
    ReleaseStage.Draft -> listOf(TrackAction.StartBuild)
    is ReleaseStage.Building -> listOf(TrackAction.CheckCi)
    is ReleaseStage.Testing -> listOf(TrackAction.Submit)
    ReleaseStage.Submitted -> listOf(TrackAction.CheckReview)
    ReleaseStage.InReview -> listOf(TrackAction.CheckReview)
    is ReleaseStage.Rejected -> listOf(TrackAction.Resubmit)
    ReleaseStage.Approved -> if (platform.supportsStagedRollout) {
        listOf(TrackAction.StartRollout, TrackAction.Release)
    } else {
        listOf(TrackAction.Release)
    }
    is ReleaseStage.RollingOut -> listOf(TrackAction.IncreaseRollout, TrackAction.Halt)
    ReleaseStage.Live -> emptyList()
    is ReleaseStage.Halted -> listOf(TrackAction.Resume)
}
