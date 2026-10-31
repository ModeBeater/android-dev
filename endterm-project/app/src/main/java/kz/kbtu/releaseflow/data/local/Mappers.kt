package kz.kbtu.releaseflow.data.local

import kz.kbtu.releaseflow.domain.model.ChecklistItem
import kz.kbtu.releaseflow.domain.model.MobileApp
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.PlatformTrack
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.StageEvent
import kz.kbtu.releaseflow.domain.model.StoreListing

/*
 * Entity <-> domain mapping. The domain never sees Room types and Room never sees the sealed
 * class: a ReleaseStage is stored as its variant name plus up to two payload columns.
 */

// ---- stages -------------------------------------------------------------------

/** `stage` column, `detail` column, `extra` column. */
fun ReleaseStage.toColumns(): Triple<String, String?, String?> = when (this) {
    ReleaseStage.Draft -> Triple("Draft", null, null)
    is ReleaseStage.Building -> Triple("Building", pipeline, null)
    is ReleaseStage.Testing -> Triple("Testing", channel, null)
    ReleaseStage.Submitted -> Triple("Submitted", null, null)
    ReleaseStage.InReview -> Triple("InReview", null, null)
    is ReleaseStage.Rejected -> Triple("Rejected", reason, guideline)
    ReleaseStage.Approved -> Triple("Approved", null, null)
    is ReleaseStage.RollingOut -> Triple("RollingOut", percent.toString(), null)
    ReleaseStage.Live -> Triple("Live", null, null)
    is ReleaseStage.Halted -> Triple("Halted", percent.toString(), null)
}

fun stageFromColumns(stage: String, detail: String?, extra: String?): ReleaseStage = when (stage) {
    "Draft" -> ReleaseStage.Draft
    "Building" -> ReleaseStage.Building(detail ?: "")
    "Testing" -> ReleaseStage.Testing(detail ?: "")
    "Submitted" -> ReleaseStage.Submitted
    "InReview" -> ReleaseStage.InReview
    "Rejected" -> ReleaseStage.Rejected(detail ?: "", extra)
    "Approved" -> ReleaseStage.Approved
    "RollingOut" -> ReleaseStage.RollingOut(detail?.toIntOrNull() ?: 0)
    "Live" -> ReleaseStage.Live
    "Halted" -> ReleaseStage.Halted(detail?.toIntOrNull() ?: 0)
    else -> throw IllegalStateException("Unknown stage '$stage' in the database")
}

fun StageEvent.toEntity(trackId: Int): StageEventEntity {
    val (stage, detail, extra) = stage.toColumns()
    return StageEventEntity(trackId = trackId, at = at, stage = stage, detail = detail, extra = extra, note = note)
}

fun StageEventEntity.toDomain(): StageEvent = StageEvent(stageFromColumns(stage, detail, extra), at, note)

// ---- apps ----------------------------------------------------------------------

fun AppEntity.toDomain(): MobileApp = MobileApp(id, name, bundleId, packageName, githubRepo)

fun MobileApp.toEntity(): AppEntity = AppEntity(id, name, bundleId, packageName, githubRepo)

// ---- releases ------------------------------------------------------------------

fun ChecklistItemEntity.toDomain(): ChecklistItem =
    ChecklistItem(id, title, platform?.let { Platform.valueOf(it) }, guideline, done)

fun ChecklistItem.toEntity(releaseId: Int): ChecklistItemEntity =
    ChecklistItemEntity(id = 0, releaseId = releaseId, title = title, platform = platform?.name, guideline = guideline, done = done)

fun TrackWithEvents.toDomain(): PlatformTrack = PlatformTrack(
    id = track.id,
    platform = Platform.valueOf(track.platform),
    buildNumber = track.buildNumber,
    // @Relation gives no ordering guarantee: order by time, then by insertion.
    history = events.sortedWith(compareBy({ it.at }, { it.id })).map { it.toDomain() },
)

fun ReleaseWithDetails.toDomain(): Release = Release(
    id = release.id,
    appId = release.appId,
    version = release.version,
    createdAt = release.createdAt,
    plannedAt = release.plannedAt,
    notes = release.notes,
    tracks = tracks.map { it.toDomain() }.sortedBy { it.platform.ordinal },
    checklist = checklist.sortedBy { it.id }.map { it.toDomain() },
)

// ---- store listings -------------------------------------------------------------

fun StoreListingEntity.toDomain(): StoreListing = StoreListing(
    appId, storeAppId, name, bundleId, version, releaseNotes, releasedAt, rating, ratingCount,
    artworkUrl, storeUrl, sellerName, minimumOs, fetchedAt, previousVersion,
)

fun StoreListing.toEntity(): StoreListingEntity = StoreListingEntity(
    appId, storeAppId, name, bundleId, version, releaseNotes, releasedAt, rating, ratingCount,
    artworkUrl, storeUrl, sellerName, minimumOs, fetchedAt, previousVersion,
)
