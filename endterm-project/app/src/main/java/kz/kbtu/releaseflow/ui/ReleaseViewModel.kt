package kz.kbtu.releaseflow.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kz.kbtu.releaseflow.data.repository.ReleaseData
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.Release
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.domain.model.TrackAction
import kz.kbtu.releaseflow.domain.model.compareWithStore
import kz.kbtu.releaseflow.domain.service.ReleaseStats
import kz.kbtu.releaseflow.ui.navigation.Routes

/**
 * Holds the screen state and the application logic shared by every screen.
 *
 * Scoped to the Activity (created in [ReleaseFlowApp] above the NavHost), so it survives
 * navigation between screens and configuration changes. It collects the repository's
 * `Flow` once: every database write anywhere in the app — a screen, the sync worker —
 * ends up here as a new [ReleaseUiState]. The UI only reads [uiState] and calls the event
 * methods; the ViewModel never touches Compose or Android UI classes.
 */
class ReleaseViewModel(
    private val repository: ReleaseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReleaseUiState())
    val uiState: StateFlow<ReleaseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.data.collect { data -> _uiState.update { it.applyData(data) } }
        }
    }

    // -------------------------------------------------------------------
    // Releases
    // -------------------------------------------------------------------

    /** Creates the release, waits until it is visible in the state, then navigates to it (replacing the form). */
    fun createRelease(appId: Int, version: String, builds: Map<Platform, String>, plannedAt: Long?, notes: String) = command {
        val id = repository.createRelease(appId, version.trim(), builds, plannedAt, notes.trim())
        awaitInState { data -> data.releases.any { it.id == id } }
        navigateTo(Routes.release(id), popUpTo = Routes.NEW_RELEASE)
    }

    fun updateNotes(releaseId: Int, notes: String) = command { repository.updateNotes(releaseId, notes) }

    fun toggleChecklistItem(releaseId: Int, itemId: Int) = command { repository.toggleChecklistItem(releaseId, itemId) }

    fun addChecklistItem(releaseId: Int, title: String, platform: Platform?) = command {
        repository.addChecklistItem(releaseId, title, platform)
        showMessage("Checklist item added")
    }

    // -------------------------------------------------------------------
    // Workflow
    // -------------------------------------------------------------------

    /**
     * Runs a workflow action. External ones (CI / store) mark the track as processing so
     * the screen shows a spinner while the (simulated) request is in flight.
     */
    fun runAction(releaseId: Int, platform: Platform, action: TrackAction, percent: Int? = null) {
        if (action.isExternal) {
            runExternal(releaseId, platform, action)
        } else {
            command {
                val stage = repository.applyAction(releaseId, platform, action, percent)
                showMessage("${platform.label}: ${action.label.lowercase()} — ${stage.label}")
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
            } catch (e: IllegalArgumentException) {
                showMessage(e.message ?: "Invalid input")
            } finally {
                _uiState.update { it.copy(processing = it.processing - key) }
            }
        }
    }

    // -------------------------------------------------------------------
    // One-shot events
    // -------------------------------------------------------------------

    fun onMessageShown(id: Long) = _uiState.update {
        if (it.message?.id == id) it.copy(message = null) else it
    }

    fun onNavigated(id: Long) = _uiState.update {
        if (it.navigation?.id == id) it.copy(navigation = null) else it
    }

    private fun showMessage(text: String) = _uiState.update {
        it.copy(message = UiMessage(id = System.nanoTime(), text = text))
    }

    private fun navigateTo(route: String, popUpTo: String? = null) = _uiState.update {
        it.copy(navigation = NavigationEvent(id = System.nanoTime(), route = route, popUpTo = popUpTo))
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    /** Runs a suspending command on the repository; domain validation errors become snackbar messages. */
    private fun command(block: suspend () -> Unit) = viewModelScope.launch {
        try {
            block()
        } catch (e: IllegalArgumentException) {
            showMessage(e.message ?: "Invalid input")
        } catch (e: IllegalStateException) {
            showMessage(e.message ?: "Operation is not allowed now")
        }
    }

    /** Waits (briefly) until Room has delivered a snapshot that satisfies [predicate]. */
    private suspend fun awaitInState(predicate: (ReleaseData) -> Boolean) {
        withTimeoutOrNull(2_000) { repository.data.first(predicate) }
    }

    private fun ReleaseUiState.applyData(data: ReleaseData): ReleaseUiState {
        val appNames = data.apps.associate { it.id to it.name }
        val releaseUis = data.releases
            .map { it.toUi(appNames[it.appId] ?: "Unknown app") }
            .sortedByDescending { it.createdAt }
        val appUis = data.apps.map { app ->
            val listing = data.listings[app.id]
            AppUi(
                id = app.id,
                name = app.name,
                bundleId = app.bundleId,
                packageName = app.packageName,
                githubRepo = app.githubRepo,
                monogram = app.monogram,
                releases = releaseUis.filter { it.appId == app.id },
                listing = listing,
                storeComparison = listing?.let { compareWithStore(it.version, data.releases.filter { r -> r.appId == app.id }, Platform.IOS) },
            )
        }
        return copy(
            loaded = true,
            apps = appUis,
            releases = releaseUis,
            stats = ReleaseStats.compute(data.apps, data.releases),
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
        tracks = tracks.map { track ->
            TrackUi(
                releaseId = id,
                platform = track.platform,
                buildNumber = track.buildNumber,
                stage = track.stage,
                enteredAt = track.enteredAt,
                history = track.history,
                actions = track.actions,
            )
        },
        checklist = checklist,
    )
}
