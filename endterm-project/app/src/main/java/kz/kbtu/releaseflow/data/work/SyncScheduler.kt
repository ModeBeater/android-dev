package kz.kbtu.releaseflow.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * Everything the app asks WorkManager for, in one place:
 *
 * - a *periodic* request ("store-sync") that re-runs every N minutes while the device is
 *   online and the battery is not low — unique, so rescheduling replaces, never duplicates;
 * - a *one-time* request ("store-sync-now") started from the Settings screen;
 * - their status as `Flow<WorkInfo?>` so the UI can show "Running…" / "Done".
 */
class SyncScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context.applicationContext)

    /** The constraints: no point asking the App Store without a network. */
    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun schedulePeriodic(intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<StoreSyncWorker>(intervalMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG)
            .build()
        // UPDATE keeps the existing schedule's timing but applies the new interval/constraints.
        workManager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancelPeriodic() = workManager.cancelUniqueWork(PERIODIC)

    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<StoreSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workDataOf(StoreSyncWorker.KEY_MANUAL to true))
            .addTag(TAG)
            .build()
        // KEEP: tapping "Sync now" twice does not queue a second run while one is pending.
        workManager.enqueueUniqueWork(MANUAL, ExistingWorkPolicy.KEEP, request)
    }

    val manualStatus: Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(MANUAL).map { it.firstOrNull() }

    val periodicStatus: Flow<WorkInfo?> =
        workManager.getWorkInfosForUniqueWorkFlow(PERIODIC).map { it.firstOrNull() }

    companion object {
        const val PERIODIC = "store-sync"
        const val MANUAL = "store-sync-now"
        const val TAG = "store"
    }
}
