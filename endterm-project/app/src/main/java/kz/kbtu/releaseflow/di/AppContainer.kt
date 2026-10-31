package kz.kbtu.releaseflow.di

import android.content.Context
import kz.kbtu.releaseflow.data.local.ReleaseDatabase
import kz.kbtu.releaseflow.data.remote.BuildLogSocket
import kz.kbtu.releaseflow.data.remote.Network
import kz.kbtu.releaseflow.data.repository.NetworkStoreRepository
import kz.kbtu.releaseflow.data.repository.OfflineReleaseRepository
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.data.repository.SettingsRepository
import kz.kbtu.releaseflow.data.repository.StoreRepository
import kz.kbtu.releaseflow.data.repository.settingsDataStore
import kz.kbtu.releaseflow.data.work.SyncScheduler
import kz.kbtu.releaseflow.domain.service.ReleaseService
import kz.kbtu.releaseflow.domain.service.SimulatedStoreGateway

/**
 * Manual dependency injection (the approach of Google's "Android Basics with Compose"):
 * the container owns every long-lived object and the ViewModel factory pulls them from
 * here. Swapping an implementation — a fake repository in tests, a real CI gateway later —
 * is a one-line change in [DefaultAppContainer].
 */
interface AppContainer {
    val releaseRepository: ReleaseRepository
    val storeRepository: StoreRepository
    val settingsRepository: SettingsRepository
    val syncScheduler: SyncScheduler
    val buildLogSocket: BuildLogSocket
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

    override val storeRepository: StoreRepository by lazy {
        NetworkStoreRepository(Network.itunesApi, Network.gitHubApi, releaseRepository)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(appContext.settingsDataStore)
    }

    override val syncScheduler: SyncScheduler by lazy { SyncScheduler(appContext) }

    override val buildLogSocket: BuildLogSocket by lazy { BuildLogSocket(Network.client) }
}
