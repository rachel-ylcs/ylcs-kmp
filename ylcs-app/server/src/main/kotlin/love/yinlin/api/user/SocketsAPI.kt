package love.yinlin.api.user

import io.ktor.server.routing.Routing
import io.ktor.server.websocket.webSocket
import love.yinlin.api.ImplMap
import love.yinlin.api.user.sockets.LyricsSocketsManager
import love.yinlin.data.rachel.sockets.LyricsSockets

fun Routing.socketsAPI(implMap: ImplMap) {
    webSocket(LyricsSockets.path) {
        LyricsSocketsManager.dispatchMessage(this)
    }
}