package love.yinlin.cs.plugin

import io.ktor.server.application.PipelineCall
import io.ktor.server.request.uri
import love.yinlin.cs.ServerPlugin
import love.yinlin.extension.catchingError

abstract class IPLogPlugin : ServerPlugin("IPLog") {
    override fun onPipelineCall(call: PipelineCall) {
        catchingError {
            val request = call.request
            val ip = request.headers["X-Real-IP"]
            val uri = request.uri
            if (filterUri(uri) && ip != null) onLog(uri, ip)
        }
    }

    abstract fun filterUri(uri: String): Boolean
    abstract fun onLog(uri: String, ip: String)
}