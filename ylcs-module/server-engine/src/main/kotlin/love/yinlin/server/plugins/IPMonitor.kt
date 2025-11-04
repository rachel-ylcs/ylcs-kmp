package love.yinlin.server.plugins

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.uri
import love.yinlin.extension.catchingError
import love.yinlin.server.logger

val IPMonitor = createApplicationPlugin("RequestListener") {
    onCall {
        catchingError {
            val request = it.request
            val ip = request.headers["X-Real-IP"]
            val uri = request.uri
            if (uri.startsWith("/user") ||
                uri.startsWith("/sys") ||
                uri.startsWith("/res"))
                logger.debug("URL: {} IP: {}", uri, ip)
        }?.let { err ->
            logger.error("RequestListener - {}", err.stackTraceToString())
        }
    }
}