package love.yinlin.startup

import love.yinlin.uri.Uri
import love.yinlin.uri.toUri
import love.yinlin.platform.Platform
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupHandler
import love.yinlin.service.SyncStartup
import java.awt.Desktop

@StartupHandler(index = 0, name = "onDeepLinkOpen", handlerType = StartupMacOSDeepLink.Handler::class, returnType = Unit::class, Uri::class)
class StartupMacOSDeepLink : SyncStartup {
    fun interface Handler {
        fun handle(uri: Uri)
    }

    override fun init(context: PlatformContext, args: StartupArgs) {
        val handler: Handler = args[0]
        Platform.use(Platform.MacOS) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                handler.handle(event.uri.toUri())
            }
        }
    }
}