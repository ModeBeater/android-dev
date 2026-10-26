package kz.kbtu.releaseflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.data.ReleaseData
import kz.kbtu.releaseflow.data.ReleaseRepository
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.service.ReleaseStats

/**
 * Holds the screen state and all application logic for every screen.
 *
 * Scoped to the Activity (created in [ReleaseFlowApp] above the NavHost), so it survives
 * navigation between screens and configuration changes. The UI only reads [uiState] and
 * calls the event methods; the ViewModel talks to the repository and never to Compose.
 */
class ReleaseViewModel(
    private val repository: ReleaseRepository = ReleaseRepository.instance,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReleaseUiState())
    val uiState: StateFlow<ReleaseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.data.collect { data -> _uiState.update { it.applyData(data) } }
        }
    }

    // -------------------------------------------------------------------
    // Apps and releases
    // -------------------------------------------------------------------

    /** Returns the new app's id, or null if the input was rejected (message is shown). */
    fun addApp(name: String, bundleId: String, packageName: String): Int? = command {
        repository.addApp(name, bundleId, packageName).id
    }

    /** Returns the new release's id, or null if the input was rejected. */
    fun createRelease(appId: Int, version: String, builds: Map<Platform, String>, plannedAt: Long?, notes: String): Int? =
        command {
            repository.createRelease(appId, version.trim(), builds, plannedAt, notes.trim()).id
        }

    fun updateNotes(releaseId: Int, notes: String) {
        repository.updateNotes(releaseId, notes)
        syncNow()
    }

    fun toggleChecklistItem(releaseId: Int, itemId: Int) {
        repository.toggleChecklistItem(releaseId, itemId)
        syncNow()
    }

    fun addChecklistItem(releaseId: Int, title: String, platform: Platform?) {
        command {
            repository.addChecklistItem(releaseId, title, platform)
            showMessage("Checklist item added")
        }
    }

    // -------------------------------------------------------------------
    // Workflow
    // -------------------------------------------------------------------

    /**
     * Runs a workflow action. External ones (CI / store) go to a coroutine and mark the
     * track as processing so the screen shows a spinner; the others apply instantly.
     */
    fun runAction(releaseId: Int, platform: Platform, action: TrackAction, percent: Int? = null) {
        if (action.isExternal) {
            runExternal(releaseId, platform, action)
        } else {
            command {
                repository.applyAction(releaseId, platform, action, percent)
                showMessage("${platform.label}: ${action.label.lowercase()} — ${stageLabel(releaseId, platform)}")
            }
        }
    }

    private fun runExternal(releaseId: Int, platform: Platform, action: TrackAction) {
        val key = "$releaseId:${platform.name}"
        if (key in _uiState.value.processing) return
        _uiState.update { it.copy(processing = it.processing + key) }
        viewModelScope.launch {
            try {
                val stage = repository.applyExternalAction(releaseId, platform, action)
                showMessage(
                    when (stage) {
                        is ReleaseStage.Rejected -> "${platform.label}: rejected — ${stage.reason}"
                        ReleaseStage.Approved -> "${platform.label}: approved, ready to release"
                        is ReleaseStage.Testing -> "${platform.label}: build is in ${stage.channel}"
                        else -> "${platform.label}: ${stage.label}"
                    },
                )
            } catch (e: IllegalStateException) {
                showMessage(e.message ?: "Operation is not allowed now")
            } finally {
                _uiState.update { it.copy(processing = it.processing - key).applyData(repository.data.value) }
            }
        }
    }

    // -------------------------------------------------------------------
    // Messages
    // -------------------------------------------------------------------

    fun onMessageShown(id: Long) = _uiState.update {
        if (it.message?.id == id) it.copy(message = null) else it
    }

    private fun showMessage(text: String) = _uiState.update {
        it.copy(message = UiMessage(id = System.nanoTime(), text = text))
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /** Runs a synchronous command; domain validation errors become snackbar messages. */
    private fun <T> command(block: () -> T): T? = try {
        block().also { syncNow() }
    } catch (e: IllegalArgumentException) {
        showMessage(e.message ?: "Invalid input")
        null
    } catch (e: IllegalStateException) {
        showMessage(e.message ?: "Operation is not allowed now")
        null
    }

    /**
     * Mirrors the repository into the UI state right away, so a screen that navigates
     * immediately after a command (e.g. to the release it just created) already sees it.
     * The collector in `init` delivers the same snapshot again; applying it twice is harmless.
     */
    private fun syncNow() = _uiState.update { it.applyData(repository.data.value) }

    private fun stageLabel(releaseId: Int, platform: Platform): String =
        repository.release(releaseId)?.track(platform)?.stage?.label ?: ""

    private fun ReleaseUiState.applyData(data: ReleaseData): ReleaseUiState {
        val appNames = data.apps.associate { it.id to it.name }
        val releaseUis = data.releases
            .map { it.toUi(appNames[it.appId] ?: "Unknown app") }
            .sortedByDescending { it.createdAt }
        val appUis = data.apps.map { app ->
            AppUi(
                id = app.id,
                name = app.name,
                bundleId = app.bundleId,
                packageName = app.packageName,
                monogram = app.monogram,
                releases = releaseUis.filter { it.appId == app.id },
            )
        }
        return copy(
            apps = appUis,
            releases = releaseUis,
            stats = ReleaseStats.compute(data.apps, data.releases),
            dataVersion = data.version,
            now = System.currentTimeMillis(),
        )
    }

    private fun Release.toUi(appName: String) = ReleaseUi(
        id = id,
        appId = appId,
        appName = appName,
        version = version,
        createdAt = createdAt,
        plannedAt = plannedAt,
        notes = notes,
        tracks = Platform.entries.mapNotNull { platform ->
            track(platform)?.let { track ->
                TrackUi(
                    releaseId = id,
                    platform = platform,
                    buildNumber = track.buildNumber,
                    stage = track.stage,
                    enteredAt = track.enteredAt,
                    history = track.history,
                    actions = track.actions,
                )
            }
        },
        checklist = checklist,
    )
}
