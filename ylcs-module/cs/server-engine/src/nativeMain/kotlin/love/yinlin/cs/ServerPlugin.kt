package love.yinlin.cs

import io.ktor.server.application.Application
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install

abstract class ServerPlugin(private val name: String) : BasicServerPlugin {
    final override fun Application.onInstall() {
        install(createApplicationPlugin(name) {
            onCall {
                onPipelineCall(it)
            }
        })
    }

    abstract fun onPipelineCall(call: PipelineCall)
}