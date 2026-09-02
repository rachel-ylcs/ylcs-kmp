package love.yinlin.cs.plugin

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import love.yinlin.cs.BasicServerPlugin
import love.yinlin.extension.Json

object JsonPlugin : BasicServerPlugin {
    override fun Application.onInstall() {
        install(ContentNegotiation) {
            json(Json)
        }
    }
}