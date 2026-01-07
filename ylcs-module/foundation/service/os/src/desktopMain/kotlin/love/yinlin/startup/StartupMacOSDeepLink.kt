package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.uri.Uri
import love.yinlin.uri.toUri
import love.yinlin.platform.Platform
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.StartupHandler
import love.yinlin.SyncStartup
import java.awt.Desktop

@StartupHandler(index = 0, name = "onDeepLinkOpen", handlerType = StartupMacOSDeepLink.Handler::class, returnType = Unit::class, Uri::class)
@Stable
class StartupMacOSDeepLink : SyncStartup() {
    fun interface Handler {
        fun handle(uri: Uri)
    }

    override fun init(context: Context, args: StartupArgs) {
        val handler: Handler = args[0]
        Platform.use(Platform.MacOS) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                handler.handle(event.uri.toUri())
            }
        }
    }
}