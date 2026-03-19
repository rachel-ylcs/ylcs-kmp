package love.yinlin.startup

import kotlinx.coroutines.CoroutineScope
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup

class StartupComposeSwingRender(context: PlatformContextProvider) : SyncStartup(context) {
    override fun init(scope: CoroutineScope, args: StartupArgs) {
        System.setProperty("compose.swing.render.on.graphics", "true")
        System.setProperty("compose.interop.blending", "true")
    }
}