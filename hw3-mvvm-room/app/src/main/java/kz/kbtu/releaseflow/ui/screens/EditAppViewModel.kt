package kz.kbtu.releaseflow.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.ui.navigation.Routes

/** The "Edit app" form: pre-filled from the database, validated as the user types, saved on demand. */
data class EditAppUiState(
    val loaded: Boolean = false,
    val name: String = "",
    val bundleId: String = "",
    val packageName: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,          // from the repository (e.g. duplicate name)
) {
    val nameError: String?
        get() = if (name.trim().length < 2) "At least 2 characters" else null

    val bundleIdError: String?
        get() = if (bundleId.isNotBlank() && !bundleId.trim().matches(Regex("""[A-Za-z0-9_.-]+"""))) "Letters, digits, dots and dashes only" else null

    val packageNameError: String?
        get() = if (packageName.isNotBlank() && !packageName.trim().matches(Regex("""[A-Za-z0-9_.]+"""))) "Letters, digits, dots and underscores only" else null

    val canSave: Boolean
        get() = loaded && !saving && nameError == null && bundleIdError == null && packageNameError == null
}

/**
 * Update half of the app's CRUD. The form state lives in the ViewModel (not in
 * `rememberSaveable`) because it is loaded from the database and saved back through the
 * repository; `appId` comes from the route via `SavedStateHandle`.
 */
class EditAppViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ReleaseRepository,
) : ViewModel() {

    val appId: Int = checkNotNull(savedStateHandle[Routes.APP_ID_ARG])

    private val _uiState = MutableStateFlow(EditAppUiState())
    val uiState: StateFlow<EditAppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val app = repository.app(appId)
            if (app != null) {
                _uiState.update {
                    it.copy(loaded = true, name = app.name, bundleId = app.bundleId, packageName = app.packageName)
                }
            } else {
                _uiState.update { it.copy(error = "App #$appId does not exist") }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, error = null) }
    fun onBundleIdChange(value: String) = _uiState.update { it.copy(bundleId = value, error = null) }
    fun onPackageNameChange(value: String) = _uiState.update { it.copy(packageName = value, error = null) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.updateApp(appId, state.name, state.bundleId, state.packageName)
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(saving = false, error = e.message ?: "Invalid input") }
            }
        }
    }
}
