package love.yinlin.plugins

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.request.uri
import love.yinlin.logger

val IPMonitor = createApplicationPlugin("RequestListener") {
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
}