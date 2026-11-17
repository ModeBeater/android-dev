package kz.kbtu.releaseflow.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kz.kbtu.releaseflow.domain.model.Release

/**
 * All database access. Queries return `Flow` so the UI re-renders on every change (Room
 * re-runs the query when any of its tables is written). Writes are `suspend`: Room runs them
 * on its own I/O dispatcher, never on the main thread.
 *
 * An abstract class rather than an interface so that multi-table writes can be grouped in
 * `@Transaction` methods with a body.
 */
@Dao
abstract class ReleaseDao {

    // ---- observe (Flow) ------------------------------------------------------

    @Query("SELECT * FROM apps ORDER BY name COLLATE NOCASE")
    abstract fun observeApps(): Flow<List<AppEntity>>

    @Transaction
    @Query("SELECT * FROM releases ORDER BY createdAt DESC")
    abstract fun observeReleases(): Flow<List<ReleaseWithDetails>>

    // ---- read once (suspend) ---------------------------------------------------

    @Query("SELECT * FROM apps ORDER BY name COLLATE NOCASE")
    abstract suspend fun apps(): List<AppEntity>

    @Query("SELECT * FROM apps WHERE id = :id")
    abstract suspend fun app(id: Int): AppEntity?

    @Transaction
    @Query("SELECT * FROM releases WHERE id = :id")
    abstract suspend fun release(id: Int): ReleaseWithDetails?

    @Query("SELECT COUNT(*) FROM apps")
    abstract suspend fun appCount(): Int

    /** `excludeId` lets an edit form keep its own current name. */
    @Query("SELECT EXISTS(SELECT 1 FROM apps WHERE name = :name COLLATE NOCASE AND id != :excludeId)")
    abstract suspend fun appNameExists(name: String, excludeId: Int = 0): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM releases WHERE appId = :appId AND version = :version AND id != :excludeId)")
    abstract suspend fun versionExists(appId: Int, version: String, excludeId: Int = 0): Boolean

    // ---- create ------------------------------------------------------------------

    @Insert
    abstract suspend fun insertApp(app: AppEntity): Long

    @Insert
    abstract suspend fun insertRelease(release: ReleaseEntity): Long

    @Insert
    abstract suspend fun insertTrack(track: TrackEntity): Long

    @Insert
    abstract suspend fun insertEvent(event: StageEventEntity): Long

    @Insert
    abstract suspend fun insertChecklistItem(item: ChecklistItemEntity): Long

    /**
     * Inserts a whole release (its tracks, their histories and the checklist) atomically and
     * returns the new release id. Either every row is written or none is.
     */
    @Transaction
    open suspend fun insertReleaseGraph(release: Release): Int {
        val releaseId = insertRelease(
            ReleaseEntity(
                appId = release.appId,
                version = release.version,
                createdAt = release.createdAt,
                plannedAt = release.plannedAt,
                notes = release.notes,
            ),
        ).toInt()
        for (track in release.tracks) {
            val trackId = insertTrack(TrackEntity(releaseId = releaseId, platform = track.platform.name, buildNumber = track.buildNumber)).toInt()
            for (event in track.history) insertEvent(event.toEntity(trackId))
        }
        for (item in release.checklist) insertChecklistItem(item.toEntity(releaseId))
        return releaseId
    }

    // ---- update --------------------------------------------------------------------

    @Update
    abstract suspend fun updateApp(app: AppEntity)

    @Query("UPDATE releases SET version = :version, plannedAt = :plannedAt, notes = :notes WHERE id = :releaseId")
    abstract suspend fun updateRelease(releaseId: Int, version: String, plannedAt: Long?, notes: String)

    @Query("UPDATE checklist_items SET done = NOT done WHERE id = :itemId")
    abstract suspend fun toggleChecklistItem(itemId: Int): Int

    // ---- delete --------------------------------------------------------------------

    /** Cascades to releases, tracks, events and checklist items. */
    @Delete
    abstract suspend fun deleteApp(app: AppEntity)

    /** Cascades to the release's tracks, events and checklist items. */
    @Query("DELETE FROM releases WHERE id = :releaseId")
    abstract suspend fun deleteRelease(releaseId: Int)
}
