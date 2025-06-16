package love.yinlin

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import love.yinlin.api.initAPI
import love.yinlin.extension.Json
import love.yinlin.plugins.IPMonitor
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
fun Application.module() {
    install(ContentNegotiation) { json(Json) }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(IPMonitor)

    routing {
        staticFiles("/public", File("public"))
        initAPI()
    }

    logger.info("服务器启动")
}

fun main(args: Array<String>) {
    copyResources(Application::class.java.classLoader, "public")
    EngineMain.main(args)

    Redis.close()
    Database.close()
}