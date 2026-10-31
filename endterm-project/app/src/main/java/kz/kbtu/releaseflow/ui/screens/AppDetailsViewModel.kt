package kz.kbtu.releaseflow.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.data.repository.SettingsRepository
import kz.kbtu.releaseflow.data.repository.StoreRefresh
import kz.kbtu.releaseflow.data.repository.StoreRepository
import kz.kbtu.releaseflow.ui.UiMessage
import kz.kbtu.releaseflow.ui.navigation.Routes
import retrofit2.HttpException
import java.io.IOException

/** Network state of the "Refresh" button on the store card. */
sealed interface StoreRefreshState {
    data object Idle : StoreRefreshState
    data object Loading : StoreRefreshState
    data class Error(val message: String) : StoreRefreshState
}

data class AppDetailsUiState(
    val storeRefresh: StoreRefreshState = StoreRefreshState.Idle,
    val deleted: Boolean = false,
    val message: UiMessage? = null,
)

/**
 * Per-screen ViewModel for the actions of one app: refresh its App Store listing, link a
 * GitHub repository, delete it. The app's data itself comes from the shared
 * [kz.kbtu.releaseflow.ui.ReleaseViewModel]; `appId` is read from the route arguments
 * through the `SavedStateHandle`.
 */
class AppDetailsViewModel(
    savedStateHandle: SavedStateHandle,
    private val releaseRepository: ReleaseRepository,
    private val storeRepository: StoreRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val appId: Int = checkNotNull(savedStateHandle[Routes.APP_ID_ARG])

    private val _uiState = MutableStateFlow(AppDetailsUiState())
    val uiState: StateFlow<AppDetailsUiState> = _uiState.asStateFlow()

    fun refreshStore() {
        if (_uiState.value.storeRefresh == StoreRefreshState.Loading) return
        _uiState.update { it.copy(storeRefresh = StoreRefreshState.Loading) }
        viewModelScope.launch {
            try {
                val app = releaseRepository.app(appId) ?: return@launch
                val country = settingsRepository.settings.first().storeCountry
                val text = when (val refresh = storeRepository.refreshListing(app, country)) {
                    is StoreRefresh.NewVersion -> "App Store moved from ${refresh.previousVersion} to ${refresh.listing.version}"
                    is StoreRefresh.FirstSeen -> "Found ${refresh.listing.name} ${refresh.listing.version} on the App Store"
                    is StoreRefresh.Unchanged -> "App Store still has ${refresh.listing.version}"
                    StoreRefresh.NotFound -> "${app.bundleId} is not on the App Store (${country.uppercase()})"
                }
                _uiState.update { it.copy(storeRefresh = StoreRefreshState.Idle, message = UiMessage(System.nanoTime(), text)) }
            } catch (e: IOException) {
                _uiState.update { it.copy(storeRefresh = StoreRefreshState.Error("No connection — check the network and try again")) }
            } catch (e: HttpException) {
                _uiState.update { it.copy(storeRefresh = StoreRefreshState.Error("App Store answered ${e.code()}")) }
            }
        }
    }

    fun linkGithub(repo: String) = viewModelScope.launch {
        try {
            releaseRepository.updateGithubRepo(appId, repo)
        } catch (e: IllegalArgumentException) {
            _uiState.update { it.copy(message = UiMessage(System.nanoTime(), e.message ?: "Invalid repository")) }
        }
    }

    fun deleteApp() = viewModelScope.launch {
        releaseRepository.deleteApp(appId)
        _uiState.update { it.copy(deleted = true) }
    }

    fun onMessageShown(id: Long) = _uiState.update { if (it.message?.id == id) it.copy(message = null) else it }
}
