package kz.kbtu.releaseflow.data.remote

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/** What happens on the socket, as values the ViewModel can turn into UI state. */
sealed interface SocketEvent {
    data class Opened(val server: String?) : SocketEvent
    data class Message(val text: String, val at: Long) : SocketEvent
    data class Closed(val code: Int, val reason: String) : SocketEvent
    data class Failed(val message: String) : SocketEvent
}

/**
 * A WebSocket (OkHttp) exposed as a cold `Flow`: the connection opens when the flow is
 * collected and closes when the collector is cancelled (`awaitClose`), so a ViewModel that
 * collects it in `viewModelScope` can never leak a connection.
 *
 * The app talks to a public echo server: every line it sends for the simulated pipeline
 * comes back as a "log line" — the same push model a real CI server would use.
 */
class BuildLogSocket(private val client: OkHttpClient) {

    @Volatile
    private var socket: WebSocket? = null

    val isOpen: Boolean
        get() = socket != null

    fun events(url: String): Flow<SocketEvent> = callbackFlow {
        val request = Request.Builder().url(url).build()
        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                socket = webSocket
                trySend(SocketEvent.Opened(response.header("Server")))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                trySend(SocketEvent.Message(text, System.currentTimeMillis()))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)   // acknowledge the server's close handshake
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                socket = null
                trySend(SocketEvent.Closed(code, reason))
                this@callbackFlow.close()   // completes the flow
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                socket = null
                trySend(SocketEvent.Failed(t.message ?: t.javaClass.simpleName))
                this@callbackFlow.close()
            }
        }
        val ws = client.newWebSocket(request, listener)
        awaitClose {
            ws.close(NORMAL_CLOSURE, "screen closed")
            socket = null
        }
    }

    /** Sends a text frame; false when there is no open connection. */
    fun send(text: String): Boolean = socket?.send(text) ?: false

    fun close() {
        socket?.close(NORMAL_CLOSURE, "user")
        socket = null
    }

    companion object {
        const val NORMAL_CLOSURE = 1000
        const val DEFAULT_URL = "wss://echo.websocket.org"
    }
}
