package love.yinlin

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import love.yinlin.api.ImplFunc
import love.yinlin.api.commonAPI
import love.yinlin.api.testAPI
import love.yinlin.api.userAPI

@Suppress("unused")
fun Application.module() {

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = false
        })
    }

    install(createApplicationPlugin("RequestListener") {
        onCall {
            try {
                val request = it.request
                val ip = request.headers["X-Real-IP"]
                val uri = request.uri
                if (uri.startsWith("/user") ||
                    uri.startsWith("/sys") ||
                    uri.startsWith("/res"))
                    logger.debug("URL: {} IP: {}", uri, ip)
            }
            catch (err: Exception) {
                logger.error("RequestListener - {}", err.stackTraceToString())
            }
        }
    })

    routing {
        staticFiles("/public", Resources.Public)

        val implMap = mutableMapOf<String, ImplFunc>()
        testAPI(implMap)
        commonAPI(implMap)
        userAPI(implMap)
    }

    logger.info("服务器启动")
}

fun main(args: Array<String>) {
    Resources.copyResources()
    EngineMain.main(args)
}