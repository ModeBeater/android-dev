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

/** The "New app" form: fields, inline validation, saving state and the id of the created row. */
data class AddAppUiState(
    val name: String = "",
    val bundleId: String = "",
    val packageName: String = "",
    val idsEdited: Boolean = false,
    val saving: Boolean = false,
    val createdAppId: Int? = null,          // set once the new app is in the database → navigate
    val error: String? = null,              // from the repository (e.g. duplicate name)
) {
    val nameError: String?
        get() = if (name.trim().length < 2) "At least 2 characters" else null

    val bundleIdError: String?
        get() = if (bundleId.isNotBlank() && !bundleId.trim().matches(Regex("""[A-Za-z0-9_.-]+"""))) "Letters, digits, dots and dashes only" else null

    val packageNameError: String?
        get() = if (packageName.isNotBlank() && !packageName.trim().matches(Regex("""[A-Za-z0-9_.]+"""))) "Letters, digits, dots and underscores only" else null

    val canSave: Boolean
        get() = !saving && nameError == null && bundleIdError == null && packageNameError == null
}

/**
 * Create half of the app's CRUD. The form state lives in the ViewModel so it survives
 * rotation; the repository adds the checks that need the database (unique name).
 */
class AddAppViewModel(
    private val repository: ReleaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddAppUiState())
    val uiState: StateFlow<AddAppUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update {
        if (it.idsEdited) it.copy(name = value, error = null)
        else it.copy(name = value, bundleId = suggestId(value), packageName = suggestId(value), error = null)
    }

    fun onBundleIdChange(value: String) = _uiState.update { it.copy(bundleId = value, idsEdited = true, error = null) }
    fun onPackageNameChange(value: String) = _uiState.update { it.copy(packageName = value, idsEdited = true, error = null) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val id = repository.addApp(state.name, state.bundleId, state.packageName)
                // Wait until Room has delivered the new row, so the details screen never flashes "not found".
                withTimeoutOrNull(2_000) { repository.data.first { data -> data.apps.any { it.id == id } } }
                _uiState.update { it.copy(saving = false, createdAppId = id) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(saving = false, error = e.message ?: "Invalid input") }
            }
        }
    }

    private fun suggestId(appName: String): String =
        "kz." + appName.trim().lowercase().replace(Regex("[^a-z0-9 ]"), "").replace(" ", ".")
}
