package love.yinlin.cs

import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin
import io.ktor.server.application.PipelineCall
import io.ktor.server.application.PluginInstance
import io.ktor.server.application.createApplicationPlugin

abstract class ServerPlugin(private val name: String) : BaseServerPlugin<Unit, PluginInstance> {
    final override fun buildPlugin(): BaseApplicationPlugin<Application, Unit, PluginInstance> = createApplicationPlugin(name) {
        onCall {
            this@ServerPlugin.onCall(it)
        }
    }

    final override fun Unit.configurePlugin() = onConfigure()

    open fun onConfigure() { }

    abstract fun onCall(call: PipelineCall)
}