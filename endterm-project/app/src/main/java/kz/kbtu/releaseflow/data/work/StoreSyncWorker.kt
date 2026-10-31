package kz.kbtu.releaseflow.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.flow.first
import kz.kbtu.releaseflow.ReleaseFlowApplication
import kz.kbtu.releaseflow.data.repository.StoreRefresh
import java.io.IOException

/**
 * Background job: asks the App Store about every tracked app, caches the answers and
 * notifies when a store version changed. Scheduled periodically by [SyncScheduler] (with
 * a network constraint) and started once on demand from the Settings screen.
 *
 * `CoroutineWorker` runs `doWork` on a background dispatcher, so the suspending repository
 * calls can be used directly. Returning `retry()` on a network failure lets WorkManager
 * re-run the job with exponential back-off once the constraint is met again.
 */
class StoreSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as ReleaseFlowApplication).container
        val settings = container.settingsRepository.settings.first()
        val manual = inputData.getBoolean(KEY_MANUAL, false)

        var checked = 0
        var changed = 0
        var notFound = 0
        var failed = 0

        for (app in container.releaseRepository.apps()) {
            if (app.bundleId.isBlank()) continue
            try {
                when (val refresh = container.storeRepository.refreshListing(app, settings.storeCountry)) {
                    is StoreRefresh.NewVersion -> {
                        changed++
                        if (settings.notificationsEnabled) {
                            Notifications.newStoreVersion(applicationContext, app.id, app.name, refresh.listing.version, refresh.previousVersion)
                        }
                    }
                    is StoreRefresh.FirstSeen, is StoreRefresh.Unchanged -> Unit
                    StoreRefresh.NotFound -> notFound++
                }
                checked++
            } catch (e: IOException) {
                failed++          // no network / DNS / timeout: try the next app, retry the job later
            } catch (e: retrofit2.HttpException) {
                failed++          // the API answered with an error code
            }
        }

        val summary = buildString {
            append("$checked checked")
            if (changed > 0) append(", $changed new version${if (changed > 1) "s" else ""}")
            if (notFound > 0) append(", $notFound not on the store")
            if (failed > 0) append(", $failed failed")
        }
        container.settingsRepository.recordSync(System.currentTimeMillis(), summary)
        if (manual && settings.notificationsEnabled) Notifications.syncSummary(applicationContext, summary)

        return when {
            checked == 0 && failed > 0 && runAttemptCount < 3 -> Result.retry()
            else -> Result.success(workDataOf(KEY_SUMMARY to summary, KEY_CHANGED to changed))
        }
    }

    companion object {
        const val KEY_MANUAL = "manual"
        const val KEY_SUMMARY = "summary"
        const val KEY_CHANGED = "changed"
    }
}
