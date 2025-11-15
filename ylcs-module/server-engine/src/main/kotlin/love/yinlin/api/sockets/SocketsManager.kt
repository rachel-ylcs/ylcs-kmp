package love.yinlin.api.sockets

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.server.websocket.sendSerialized

abstract class SocketsManager(val session: Any) {
    abstract suspend fun onMessage(msg: String)
    abstract suspend fun onError(err: Throwable)
    abstract suspend fun onClose()

    protected suspend inline fun <reified T> send(data: T) = (session as WebSocketServerSession).sendSerialized(data)
}