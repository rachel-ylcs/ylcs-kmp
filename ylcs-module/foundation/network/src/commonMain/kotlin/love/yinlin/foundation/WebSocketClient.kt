package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.flow.consumeAsFlow
import love.yinlin.extension.catchingError

class WebSocketClient internal constructor(@PublishedApi internal val delegate: HttpClient) {
    abstract class Connection {
        internal var session: WebSocketSession? = null

        suspend fun send(msg: String) = session?.send(msg) != null

        abstract suspend fun onConnect()
        abstract suspend fun onError(err: Throwable)
        abstract suspend fun onDisconnect()
        abstract suspend fun onMessage(msg: String)
    }

    suspend fun connect(host: String?, path: String, connection: Connection) {
        catchingError {
            connection.session?.close()
            val newSession = delegate.webSocketSession {
                method = HttpMethod.Get
                url(scheme = URLProtocol.WSS.name, host = host, port = URLProtocol.WSS.defaultPort, path = path)
            }
            connection.session = newSession
            connection.onConnect()
            newSession.incoming.consumeAsFlow().collect { frame ->
                if (frame is Frame.Text) connection.onMessage(frame.readText())
            }
        }?.let { connection.onError(it) }
        connection.onDisconnect()
        connection.session?.close()
        connection.session = null
    }
}