package kz.kbtu.releaseflow.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.data.repository.SettingsRepository
import kz.kbtu.releaseflow.data.repository.StoreLookup
import kz.kbtu.releaseflow.data.repository.StoreRepository
import kz.kbtu.releaseflow.domain.model.StoreListing
import kz.kbtu.releaseflow.ui.UiMessage
import retrofit2.HttpException
import java.io.IOException

/** The App Store lookup on the "New app" form: idle → loading → found / not found / error. */
sealed interface LookupState {
    data object Idle : LookupState
    data object Loading : LookupState
    data class Found(val listing: StoreListing) : LookupState
    data object NotFound : LookupState
    data class Error(val message: String) : LookupState
}

data class AddAppUiState(
    val name: String = "",
    val bundleId: String = "",
    val packageName: String = "",
    val githubRepo: String = "",
    val idsEdited: Boolean = false,
    val lookup: LookupState = LookupState.Idle,
    val saving: Boolean = false,
    val createdAppId: Int? = null,          // set once the new app is in the database → navigate
    val message: UiMessage? = null,
) {
    val canSave: Boolean
        get() = name.trim().length >= 2 && !saving
}

/**
 * The form state lives here (not in `rememberSaveable`) because a network call is part of
 * the flow: the lookup must survive rotation and its result fills the other fields.
 */
class AddAppViewModel(
    private val releaseRepository: ReleaseRepository,
    private val storeRepository: StoreRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAppUiState())
    val uiState: StateFlow<AddAppUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update {
        if (it.idsEdited) it.copy(name = value)
        else it.copy(name = value, bundleId = suggestId(value), packageName = suggestId(value))
    }

    fun onBundleIdChange(value: String) = _uiState.update { it.copy(bundleId = value, idsEdited = true, lookup = LookupState.Idle) }
    fun onPackageNameChange(value: String) = _uiState.update { it.copy(packageName = value, idsEdited = true) }
    fun onGithubRepoChange(value: String) = _uiState.update { it.copy(githubRepo = value) }

    /** Asks the App Store about the typed bundle id; a hit fills the name. */
    fun lookUp() {
        val bundleId = _uiState.value.bundleId.trim()
        if (bundleId.isEmpty() || _uiState.value.lookup == LookupState.Loading) return
        _uiState.update { it.copy(lookup = LookupState.Loading) }
        viewModelScope.launch {
            val country = settingsRepository.settings.first().storeCountry
            val result = try {
                when (val found = storeRepository.lookup(bundleId, country)) {
                    is StoreLookup.Found -> LookupState.Found(found.listing)
                    StoreLookup.NotFound -> LookupState.NotFound
                }
            } catch (e: IOException) {
                LookupState.Error("No connection to the App Store")
            } catch (e: HttpException) {
                LookupState.Error("App Store answered ${e.code()}")
            }
            _uiState.update { state ->
                val listing = (result as? LookupState.Found)?.listing
                state.copy(
                    lookup = result,
                    name = if (listing != null && state.name.isBlank()) listing.name else state.name,
                    packageName = if (listing != null && state.packageName.isBlank()) listing.bundleId else state.packageName,
                    idsEdited = true,
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val id = releaseRepository.addApp(state.name, state.bundleId, state.packageName, state.githubRepo)
                // Keep the lookup result: the details screen then shows the store card right away.
                (state.lookup as? LookupState.Found)?.let { releaseRepository.saveListing(it.listing.copy(appId = id)) }
                withTimeoutOrNull(2_000) { releaseRepository.data.first { data -> data.apps.any { it.id == id } } }
                _uiState.update { it.copy(saving = false, createdAppId = id) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(saving = false, message = UiMessage(System.nanoTime(), e.message ?: "Invalid input")) }
            }
        }
    }

    fun onMessageShown(id: Long) = _uiState.update { if (it.message?.id == id) it.copy(message = null) else it }

    private fun suggestId(appName: String): String =
        "kz." + appName.trim().lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(" ", ".")
}
