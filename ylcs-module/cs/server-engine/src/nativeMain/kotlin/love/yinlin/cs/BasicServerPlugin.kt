package love.yinlin.cs

import io.ktor.server.application.Application

interface BasicServerPlugin {
    fun Application.onInstall()
}