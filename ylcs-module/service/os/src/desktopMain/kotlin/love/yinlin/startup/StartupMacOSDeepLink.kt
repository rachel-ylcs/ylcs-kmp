package love.yinlin.startup

import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toUri
import love.yinlin.platform.Platform
import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup
import java.awt.Desktop

class StartupMacOSDeepLink : SyncStartup {
    fun interface Handler {
        fun handle(uri: Uri)
    }

    override fun init(context: PlatformContext, args: Array<Any?>) {
        val handler = args[0] as Handler
        Platform.use(Platform.MacOS) {
            Desktop.getDesktop().setOpenURIHandler { event ->
                handler.handle(event.uri.toUri())
            }
        }
    }
}