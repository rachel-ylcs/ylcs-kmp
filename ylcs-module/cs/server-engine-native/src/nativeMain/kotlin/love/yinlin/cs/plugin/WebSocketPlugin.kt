package love.yinlin.cs.plugin

import io.ktor.serialization.WebsocketContentConverter
import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import love.yinlin.cs.BaseServerPlugin
import kotlin.time.Duration

class WebSocketPlugin(
    private val socketPingPeriod: Duration,
    private val socketTimeout: Duration,
    private val socketMaxFrameSize: Long,
    private val socketMasking: Boolean,
    private val socketContentConverter: WebsocketContentConverter?,
) : BaseServerPlugin<WebSockets.WebSocketOptions, WebSockets> {
    override fun buildPlugin(): BaseApplicationPlugin<Application, WebSockets.WebSocketOptions, WebSockets> = WebSockets
    override fun WebSockets.WebSocketOptions.configurePlugin() {
        pingPeriod = socketPingPeriod
        timeout = socketTimeout
        maxFrameSize = socketMaxFrameSize
        masking = socketMasking
        contentConverter = socketContentConverter
    }
}