package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup

class StartupComposeSwingRender : SyncStartup() {
    override fun init(context: Context, args: StartupArgs) {
        System.setProperty("compose.swing.render.on.graphics", "true")
        System.setProperty("compose.interop.blending", "true")
    }
}