package love.yinlin.cs

import io.ktor.server.application.PipelineCall
import io.ktor.server.application.createApplicationPlugin

abstract class ServerPlugin(name: String) {
    internal val instance = createApplicationPlugin(name) {
        onCall {
            this@ServerPlugin.onCall(it)
        }
    }

    abstract fun onCall(call: PipelineCall)
}