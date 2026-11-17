package kz.kbtu.releaseflow.di

import android.content.Context
import kz.kbtu.releaseflow.data.local.ReleaseDatabase
import kz.kbtu.releaseflow.data.repository.OfflineReleaseRepository
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.domain.service.ReleaseService
import kz.kbtu.releaseflow.domain.service.SimulatedStoreGateway

/**
 * Manual dependency injection (the approach of Google's "Android Basics with Compose"):
 * the container owns the long-lived objects and the ViewModel factory pulls them from here.
 * Swapping the implementation — a fake repository in tests — is a one-line change in
 * [DefaultAppContainer].
 */
interface AppContainer {
    val releaseRepository: ReleaseRepository
}

class DefaultAppContainer(context: Context) : AppContainer {

    private val appContext = context.applicationContext

    override val releaseRepository: ReleaseRepository by lazy {
        OfflineReleaseRepository(
            dao = ReleaseDatabase.getDatabase(appContext).releaseDao(),
            service = ReleaseService(),
            gateway = SimulatedStoreGateway(),
        )
    }
}
