package love.yinlin.cs.plugin

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import love.yinlin.cs.BasicServerPlugin
import love.yinlin.extension.Json
import kotlin.time.Duration

class WebSocketPlugin(
    private val socketPingPeriod: Duration,
    private val socketTimeout: Duration,
    private val socketMaxFrameSize: Long,
    private val socketMasking: Boolean,
) : BasicServerPlugin {
    override fun Application.onInstall() {
        install(WebSockets) {
            pingPeriod = socketPingPeriod
            timeout = socketTimeout
            maxFrameSize = socketMaxFrameSize
            masking = socketMasking
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
    }
}