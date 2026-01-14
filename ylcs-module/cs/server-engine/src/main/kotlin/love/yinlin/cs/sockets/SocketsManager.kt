package love.yinlin.cs.sockets

import io.ktor.server.websocket.WebSocketServerSession
import io.ktor.websocket.send

abstract class SocketsManager(val session: Any) {
    abstract suspend fun onMessage(msg: String)
    abstract suspend fun onError(err: Throwable)
    abstract suspend fun onClose()

    protected suspend fun send(data: String) = (session as WebSocketServerSession).send(data)
}