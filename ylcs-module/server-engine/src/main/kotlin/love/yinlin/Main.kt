package love.yinlin

import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.http.content.staticFiles
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import love.yinlin.api.ServerEngine
import love.yinlin.extension.Json
import love.yinlin.server.Database
import love.yinlin.server.Redis
import love.yinlin.server.copyResources
import love.yinlin.server.logger
import love.yinlin.server.plugins.IPMonitor
import java.io.File
import kotlin.time.Duration.Companion.seconds

@Suppress("unused")
fun Application.module() {
    val engine = Class.forName("love.yinlin.MainServerEngine").kotlin.objectInstance as ServerEngine

    copyResources(Application::class.java.classLoader, engine.public)

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
        staticFiles(engine.public, File(engine.public))
        val scope = engine.scope()
        scope.initRouting(this@routing)
        with(engine) {
            for (api in scope.api) api()
        }
        engine.proxy?.apply {
            this@routing.listen()
        }
    }

    logger.info("服务器启动")
}

fun main(args: Array<String>) {
    EngineMain.main(args)
    Redis.close()
    Database.close()
}