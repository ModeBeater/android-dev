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
import kz.kbtu.releaseflow.domain.util.isoDateText
import kz.kbtu.releaseflow.domain.util.parseIsoDate
import kz.kbtu.releaseflow.domain.util.toEpochMillis
import kz.kbtu.releaseflow.ui.navigation.Routes

/** The "Edit release" form: version, planned date and notes; the tracks and the history are not editable. */
data class EditReleaseUiState(
    val loaded: Boolean = false,
    val appName: String = "",
    val version: String = "",
    val plannedDate: String = "",       // "YYYY-MM-DD" or empty
    val notes: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val versionError: String?
        get() = if (!version.trim().matches(Regex("""\d+\.\d+(\.\d+)?"""))) "Use 1.2 or 1.2.3" else null

    val dateError: String?
        get() = if (plannedDate.isNotBlank() && parseIsoDate(plannedDate) == null) "Use YYYY-MM-DD" else null

    val canSave: Boolean
        get() = loaded && !saving && versionError == null && dateError == null
}

/** Update half of the release's CRUD; `releaseId` comes from the route via `SavedStateHandle`. */
class EditReleaseViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ReleaseRepository,
) : ViewModel() {

    val releaseId: Int = checkNotNull(savedStateHandle[Routes.RELEASE_ID_ARG])

    private val _uiState = MutableStateFlow(EditReleaseUiState())
    val uiState: StateFlow<EditReleaseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val release = repository.release(releaseId)
            if (release != null) {
                val app = repository.app(release.appId)
                _uiState.update {
                    it.copy(
                        loaded = true,
                        appName = app?.name ?: "",
                        version = release.version,
                        plannedDate = release.plannedAt?.isoDateText() ?: "",
                        notes = release.notes,
                    )
                }
            } else {
                _uiState.update { it.copy(error = "Release #$releaseId does not exist") }
            }
        }
    }

    fun onVersionChange(value: String) = _uiState.update { it.copy(version = value, error = null) }
    fun onPlannedDateChange(value: String) = _uiState.update { it.copy(plannedDate = value, error = null) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value, error = null) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val planned = state.plannedDate.takeIf { it.isNotBlank() }?.let { parseIsoDate(it)?.toEpochMillis() }
                repository.updateRelease(releaseId, state.version, planned, state.notes)
                _uiState.update { it.copy(saving = false, saved = true) }
            } catch (e: IllegalArgumentException) {
                _uiState.update { it.copy(saving = false, error = e.message ?: "Invalid input") }
            }
        }
    }
}
