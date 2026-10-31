package kz.kbtu.releaseflow

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.data.work.Notifications
import kz.kbtu.releaseflow.di.AppContainer
import kz.kbtu.releaseflow.di.DefaultAppContainer

/**
 * Process-wide setup, run once before any Activity:
 * - builds the dependency container,
 * - creates the notification channel,
 * - seeds the database on the first launch,
 * - (re)schedules the periodic store sync from the saved settings.
 *
 * `applicationScope` outlives every screen, so the seed is never cancelled by a rotation.
 */
class ReleaseFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        Notifications.createChannel(this)
        applicationScope.launch {
            container.releaseRepository.seedIfEmpty()
            val settings = container.settingsRepository.settings.first()
            if (settings.syncEnabled) {
                container.syncScheduler.schedulePeriodic(settings.syncIntervalMinutes)
            } else {
                container.syncScheduler.cancelPeriodic()
            }
        }
    }
}
