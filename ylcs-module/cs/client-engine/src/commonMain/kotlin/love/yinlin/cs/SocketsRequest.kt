package love.yinlin.cs

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
import love.yinlin.uri.Uri

abstract class SocketsConnection {
    private var session: WebSocketSession? = null

    suspend fun send(msg: String) = session?.send(msg) != null

    suspend fun connect(sockets: Sockets) {
        catchingError {
            session?.close()
            val newSession = NetClient.internalWebSocketSession {
                method = HttpMethod.Get
                url(scheme = URLProtocol.WSS.name, host = Uri.parse(ClientEngine.baseUrl)!!.host, port = URLProtocol.WSS.defaultPort, path = sockets.path)
            }
            session = newSession
            onConnect()
            newSession.incoming.consumeAsFlow().collect { frame ->
                if (frame is Frame.Text) onMessage(frame.readText())
            }
        }?.let { onError(it) }
        onDisconnect()
        session?.close()
        session = null
    }

    abstract suspend fun onConnect()
    abstract suspend fun onError(err: Throwable)
    abstract suspend fun onDisconnect()
    abstract suspend fun onMessage(msg: String)
}