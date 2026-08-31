package love.yinlin.cs

import io.ktor.server.application.Application
import io.ktor.server.application.BaseApplicationPlugin

interface BaseServerPlugin<TConfiguration : Any, TPlugin : Any> {
    fun buildPlugin(): BaseApplicationPlugin<Application, TConfiguration, TPlugin>
    fun TConfiguration.configurePlugin()
}