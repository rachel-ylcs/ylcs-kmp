package love.yinlin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import love.yinlin.api.initAPI
import love.yinlin.extension.Json
import love.yinlin.plugins.IPMonitor

@Suppress("unused")
fun Application.module() {
    install(ContentNegotiation) { json(Json) }
    install(IPMonitor)

    routing {
        staticFiles("/public", Resources.Public)
        initAPI()
    }

    logger.info("服务器启动")
}

fun main(args: Array<String>) {
    Resources.copyResources()
    Resources.initializeConfig()
    EngineMain.main(args)

    Redis.close()
    Database.close()
}