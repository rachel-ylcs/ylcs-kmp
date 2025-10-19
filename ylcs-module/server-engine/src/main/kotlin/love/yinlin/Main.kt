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
import love.yinlin.extension.Json
import love.yinlin.server.Database
import love.yinlin.server.Redis
import love.yinlin.server.copyResources
import love.yinlin.server.logger
import love.yinlin.server.plugins.IPMonitor
import java.io.File
import kotlin.reflect.full.declaredFunctions
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

        val clz = Class.forName("love.yinlin.api.ServerStartup").kotlin
        val serverStartup = clz.objectInstance
        val runFunction = clz.declaredFunctions.find { it.name == "run" }!!
        runFunction.call(serverStartup, this)
    }

    logger.info("服务器启动")
}

fun main(args: Array<String>) {
    copyResources(Application::class.java.classLoader, "public")
    EngineMain.main(args)

    Redis.close()
    Database.close()
}