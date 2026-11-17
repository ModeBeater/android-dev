package kz.kbtu.releaseflow

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.di.AppContainer
import kz.kbtu.releaseflow.di.DefaultAppContainer

/**
 * Process-wide setup, run once before any Activity: builds the dependency container and
 * seeds the database on the first launch. `applicationScope` outlives every screen, so the
 * seed is never cancelled by a rotation.
 */
class ReleaseFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
        applicationScope.launch {
            container.releaseRepository.seedIfEmpty()
        }
    }
}
