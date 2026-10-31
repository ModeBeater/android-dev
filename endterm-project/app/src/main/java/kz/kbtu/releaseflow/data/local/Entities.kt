package kz.kbtu.releaseflow.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/*
 * Database schema (one table per aggregate part, foreign keys with ON DELETE CASCADE):
 *
 *   apps 1 ─── * releases 1 ─── * tracks 1 ─── * stage_events
 *     │                  └──── * checklist_items
 *     └── 1 store_listings            (cache of the App Store lookup, one row per app)
 *
 * A track's current stage is not a column: it is the last row of its stage_events, exactly
 * as in the domain model, so the history can never disagree with the stage.
 */

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val bundleId: String,
    val packageName: String,
    val githubRepo: String? = null,
)

@Entity(
    tableName = "releases",
    foreignKeys = [ForeignKey(entity = AppEntity::class, parentColumns = ["id"], childColumns = ["appId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("appId"), Index(value = ["appId", "version"], unique = true)],
)
data class ReleaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: Int,
    val version: String,
    val createdAt: Long,
    val plannedAt: Long?,
    val notes: String,
)

@Entity(
    tableName = "tracks",
    foreignKeys = [ForeignKey(entity = ReleaseEntity::class, parentColumns = ["id"], childColumns = ["releaseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("releaseId"), Index(value = ["releaseId", "platform"], unique = true)],
)
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val releaseId: Int,
    val platform: String,          // Platform.name
    val buildNumber: String,
)

/** `stage` is the sealed-class variant name; `detail` / `extra` carry its payload (see Mappers.kt). */
@Entity(
    tableName = "stage_events",
    foreignKeys = [ForeignKey(entity = TrackEntity::class, parentColumns = ["id"], childColumns = ["trackId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("trackId")],
)
data class StageEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val trackId: Int,
    val at: Long,
    val stage: String,
    val detail: String? = null,
    val extra: String? = null,
    val note: String? = null,
)

@Entity(
    tableName = "checklist_items",
    foreignKeys = [ForeignKey(entity = ReleaseEntity::class, parentColumns = ["id"], childColumns = ["releaseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("releaseId")],
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val releaseId: Int,
    val title: String,
    val platform: String?,         // null = both platforms
    val guideline: String?,
    val done: Boolean,
)

/** Cached App Store listing (iTunes Lookup API); refreshed by the sync worker and by "Refresh". */
@Entity(
    tableName = "store_listings",
    foreignKeys = [ForeignKey(entity = AppEntity::class, parentColumns = ["id"], childColumns = ["appId"], onDelete = ForeignKey.CASCADE)],
)
data class StoreListingEntity(
    @PrimaryKey val appId: Int,
    val storeAppId: Long,
    val name: String,
    val bundleId: String,
    val version: String,
    val releaseNotes: String?,
    val releasedAt: Long?,
    val rating: Double?,
    val ratingCount: Int?,
    val artworkUrl: String?,
    val storeUrl: String?,
    val sellerName: String?,
    val minimumOs: String?,
    val fetchedAt: Long,
    val previousVersion: String?,
)

// ---- relations: Room joins the child tables for us (always read inside @Transaction) ----

data class TrackWithEvents(
    @Embedded val track: TrackEntity,
    @Relation(parentColumn = "id", entityColumn = "trackId")
    val events: List<StageEventEntity>,
)

data class ReleaseWithDetails(
    @Embedded val release: ReleaseEntity,
    @Relation(entity = TrackEntity::class, parentColumn = "id", entityColumn = "releaseId")
    val tracks: List<TrackWithEvents>,
    @Relation(parentColumn = "id", entityColumn = "releaseId")
    val checklist: List<ChecklistItemEntity>,
)
