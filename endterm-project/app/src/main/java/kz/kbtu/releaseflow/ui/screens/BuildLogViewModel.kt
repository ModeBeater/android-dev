package kz.kbtu.releaseflow.ui.screens

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.kbtu.releaseflow.data.remote.BuildLogSocket
import kz.kbtu.releaseflow.data.remote.SocketEvent
import kz.kbtu.releaseflow.data.repository.ReleaseRepository
import kz.kbtu.releaseflow.domain.model.Platform
import kz.kbtu.releaseflow.domain.model.ReleaseStage
import kz.kbtu.releaseflow.ui.navigation.Routes

enum class SocketStatus(val label: String) {
    Disconnected("Disconnected"),
    Connecting("Connecting…"),
    Connected("Connected"),
    Failed("Failed"),
}

/** One line of the live log: when it arrived and whether it came from the server. */
data class LogLine(val at: Long, val text: String, val fromServer: Boolean)

data class BuildLogUiState(
    val title: String = "",
    val version: String = "",
    val pipeline: String = "",
    val url: String = BuildLogSocket.DEFAULT_URL,
    val status: SocketStatus = SocketStatus.Disconnected,
    val statusDetail: String? = null,
    val lines: List<LogLine> = emptyList(),
    val pipelineRunning: Boolean = false,
    val draft: String = "",
)

/**
 * Live log over a WebSocket. The socket is a `Flow` collected in `viewModelScope`: opening
 * the screen connects, leaving it (ViewModel cleared) disconnects — no listener leaks.
 *
 * The public echo server returns every frame it receives, so the ViewModel plays the role
 * of the CI runner too: once connected it sends the pipeline's steps one by one and shows
 * what comes back — the same push model a real build server would use.
 */
class BuildLogViewModel(
    savedStateHandle: SavedStateHandle,
    private val releaseRepository: ReleaseRepository,
    private val socket: BuildLogSocket,
) : ViewModel() {

    private val releaseId: Int = checkNotNull(savedStateHandle[Routes.RELEASE_ID_ARG])
    private val platform: Platform = Platform.valueOf(checkNotNull(savedStateHandle[Routes.PLATFORM_ARG]))

    private val _uiState = MutableStateFlow(BuildLogUiState())
    val uiState: StateFlow<BuildLogUiState> = _uiState.asStateFlow()

    private var connection: Job? = null
    private var pipelineJob: Job? = null

    init {
        viewModelScope.launch {
            val release = releaseRepository.release(releaseId)
            val app = release?.let { releaseRepository.app(it.appId) }
            val stage = release?.track(platform)?.stage
            _uiState.update {
                it.copy(
                    title = listOfNotNull(app?.name, release?.version, platform.label).joinToString(" · "),
                    version = release?.version ?: "",
                    pipeline = (stage as? ReleaseStage.Building)?.pipeline ?: "pipeline-$releaseId${platform.ordinal}",
                )
            }
            connect()
        }
    }

    fun onUrlChange(value: String) = _uiState.update { it.copy(url = value) }
    fun onDraftChange(value: String) = _uiState.update { it.copy(draft = value) }

    fun connect() {
        if (connection?.isActive == true) return
        _uiState.update { it.copy(status = SocketStatus.Connecting, statusDetail = null, lines = emptyList()) }
        connection = viewModelScope.launch {
            try {
                socket.events(_uiState.value.url.trim()).collect { event ->
                    when (event) {
                        is SocketEvent.Opened -> {
                            _uiState.update { it.copy(status = SocketStatus.Connected, statusDetail = event.server?.let { s -> "server: $s" }) }
                            append("connected to ${_uiState.value.url}", fromServer = false)
                            runPipeline()
                        }
                        is SocketEvent.Message -> append(event.text, fromServer = true, at = event.at)
                        is SocketEvent.Closed -> _uiState.update {
                            it.copy(status = SocketStatus.Disconnected, statusDetail = "closed (${event.code})", pipelineRunning = false)
                        }
                        is SocketEvent.Failed -> _uiState.update {
                            it.copy(status = SocketStatus.Failed, statusDetail = event.message, pipelineRunning = false)
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                // A URL the user typed that OkHttp cannot parse (no scheme, empty).
                _uiState.update { it.copy(status = SocketStatus.Failed, statusDetail = e.message ?: "Invalid URL", pipelineRunning = false) }
                return@launch
            }
            // Flow completed: the socket is closed.
            _uiState.update { if (it.status == SocketStatus.Connected) it.copy(status = SocketStatus.Disconnected) else it }
        }
    }

    fun disconnect() {
        pipelineJob?.cancel()
        connection?.cancel()
        connection = null
        socket.close()
        _uiState.update { it.copy(status = SocketStatus.Disconnected, statusDetail = "closed by user", pipelineRunning = false) }
    }

    /** Sends the typed line; the echo server returns it as a log line. */
    fun sendDraft() {
        val text = _uiState.value.draft.trim()
        if (text.isEmpty()) return
        if (socket.send("[manual] $text")) {
            _uiState.update { it.copy(draft = "") }
        } else {
            append("not connected — nothing sent", fromServer = false)
        }
    }

    /** Replays the pipeline steps over the socket, ~1 line per second. */
    fun runPipeline() {
        if (pipelineJob?.isActive == true || _uiState.value.status != SocketStatus.Connected) return
        pipelineJob = viewModelScope.launch {
            _uiState.update { it.copy(pipelineRunning = true) }
            val pipeline = _uiState.value.pipeline
            val steps = listOf(
                "[$pipeline] runner picked up the job",
                "[1/6] checkout — fetching sources, tag v${_uiState.value.version}",
                "[2/6] dependencies — restoring Gradle cache (hit)",
                "[3/6] build — assembling ${platform.label} release",
                "[4/6] tests — 184 unit tests passed",
                "[5/6] sign — signing with the release key",
                "[6/6] upload — sent to ${platform.testingChannel}",
                "[$pipeline] finished: SUCCESS",
            )
            for (step in steps) {
                if (!socket.send(step)) break
                delay(900)
            }
            _uiState.update { it.copy(pipelineRunning = false) }
        }
    }

    private fun append(text: String, fromServer: Boolean, at: Long = System.currentTimeMillis()) =
        _uiState.update { it.copy(lines = (it.lines + LogLine(at, text, fromServer)).takeLast(200)) }

    override fun onCleared() {
        socket.close()
    }
}
