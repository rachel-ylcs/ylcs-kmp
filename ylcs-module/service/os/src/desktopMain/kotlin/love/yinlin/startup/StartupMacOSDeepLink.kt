package love.yinlin.startup

import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toUri
import love.yinlin.platform.Platform
import love.yinlin.service.PlatformContext
import love.yinlin.service.StartupArg
import love.yinlin.service.StartupArgs
import love.yinlin.service.StartupDoc
import love.yinlin.service.SyncStartup
import java.awt.Desktop

@StartupDoc(
    StartupArg(0, "handler", StartupMacOSDeepLink.Handler::class),
)
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