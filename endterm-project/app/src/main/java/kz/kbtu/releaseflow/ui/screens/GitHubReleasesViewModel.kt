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
import kz.kbtu.releaseflow.data.repository.StoreRepository
import kz.kbtu.releaseflow.domain.model.GitHubRelease
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.util.dayText
import kz.kbtu.releaseflow.ui.UiMessage
import kz.kbtu.releaseflow.ui.navigation.Routes
import retrofit2.HttpException
import java.io.IOException

/** The classic three states of a network list, plus "nothing to load". */
sealed interface GitHubListState {
    data object Loading : GitHubListState
    data class Success(val releases: List<GitHubRelease>) : GitHubListState
    data class Error(val message: String) : GitHubListState
    data object NoRepository : GitHubListState
}

data class GitHubUiState(
    val appName: String = "",
    val repo: String? = null,
    val list: GitHubListState = GitHubListState.Loading,
    val isRefreshing: Boolean = false,
    val trackedVersions: Set<String> = emptySet(),   // versions that already exist as releases of this app
    val tracking: Set<Long> = emptySet(),            // GitHub release ids being imported right now
    val message: UiMessage? = null,
)

/**
 * Loads the releases of the app's GitHub repository (REST API via Retrofit) and lets the
 * user turn one into a tracked release. The list is kept in the ViewModel, so rotating the
 * phone does not reload it; pull-to-refresh does.
 */
class GitHubReleasesViewModel(
    savedStateHandle: SavedStateHandle,
    private val releaseRepository: ReleaseRepository,
    private val storeRepository: StoreRepository,
) : ViewModel() {

    val appId: Int = checkNotNull(savedStateHandle[Routes.APP_ID_ARG])

    private val _uiState = MutableStateFlow(GitHubUiState())
    val uiState: StateFlow<GitHubUiState> = _uiState.asStateFlow()

    init {
        // Tracked versions follow the database, so a just-imported release is marked at once.
        viewModelScope.launch {
            releaseRepository.data.collect { data ->
                val app = data.apps.firstOrNull { it.id == appId }
                _uiState.update {
                    it.copy(
                        appName = app?.name ?: "",
                        repo = app?.githubRepo,
                        trackedVersions = data.releases.filter { r -> r.appId == appId }.map { r -> r.version }.toSet(),
                    )
                }
            }
        }
        load()
    }

    fun load() = fetch(refreshing = false)

    fun refresh() = fetch(refreshing = true)

    private fun fetch(refreshing: Boolean) {
        viewModelScope.launch {
            val repo = releaseRepository.app(appId)?.githubRepo
            if (repo.isNullOrBlank()) {
                _uiState.update { it.copy(list = GitHubListState.NoRepository, isRefreshing = false) }
                return@launch
            }
            _uiState.update { it.copy(isRefreshing = refreshing, list = if (refreshing) it.list else GitHubListState.Loading) }
            val result = try {
                GitHubListState.Success(storeRepository.gitHubReleases(repo))
            } catch (e: IOException) {
                GitHubListState.Error("No connection — pull down to retry")
            } catch (e: HttpException) {
                GitHubListState.Error(
                    when (e.code()) {
                        404 -> "Repository $repo was not found on GitHub"
                        403 -> "GitHub rate limit reached (60 requests/hour) — try again later"
                        else -> "GitHub answered ${e.code()}"
                    },
                )
            } catch (e: IllegalArgumentException) {
                GitHubListState.Error(e.message ?: "Invalid repository")
            }
            _uiState.update { it.copy(list = result, isRefreshing = false) }
        }
    }

    /** Imports a GitHub release as a tracked release (Draft on both platforms). */
    fun track(release: GitHubRelease) {
        val version = release.version ?: return
        if (release.id in _uiState.value.tracking) return
        _uiState.update { it.copy(tracking = it.tracking + release.id) }
        viewModelScope.launch {
            try {
                val builds = mapOf(
                    Platform.IOS to version.replace(".", ""),
                    Platform.ANDROID to version.replace(".", "") + "0",
                )
                val notes = buildString {
                    append("Imported from GitHub release ${release.tag}")
                    release.publishedAt?.let { append(" (published ${it.dayText()})") }
                    release.name?.let { append(" — ").append(it) }
                }
                releaseRepository.createRelease(appId, version, builds, plannedAt = null, notes = notes)
                showMessage("Now tracking $version")
            } catch (e: IllegalArgumentException) {
                showMessage(e.message ?: "Could not create the release")
            } finally {
                _uiState.update { it.copy(tracking = it.tracking - release.id) }
            }
        }
    }

    fun onMessageShown(id: Long) = _uiState.update { if (it.message?.id == id) it.copy(message = null) else it }

    private fun showMessage(text: String) = _uiState.update { it.copy(message = UiMessage(System.nanoTime(), text)) }
}
