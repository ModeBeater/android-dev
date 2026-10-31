package kz.kbtu.releaseflow.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database. One instance per process (`getDatabase`), created lazily on first use.
 * `fallbackToDestructiveMigration` is acceptable for a course project: a schema change wipes
 * the local cache and the seed recreates the demo data.
 */
@Database(
    entities = [
        AppEntity::class,
        ReleaseEntity::class,
        TrackEntity::class,
        StageEventEntity::class,
        ChecklistItemEntity::class,
        StoreListingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class ReleaseDatabase : RoomDatabase() {

    abstract fun releaseDao(): ReleaseDao

    companion object {
        @Volatile
        private var instance: ReleaseDatabase? = null

        fun getDatabase(context: Context): ReleaseDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, ReleaseDatabase::class.java, "releaseflow.db")
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
