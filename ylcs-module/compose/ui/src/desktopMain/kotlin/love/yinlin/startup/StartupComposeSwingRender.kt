package love.yinlin.startup

import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

class StartupComposeSwingRender : SyncStartup {
    override fun init(context: PlatformContext, args: Array<Any?>) {
        System.setProperty("compose.swing.render.on.graphics", "true")
        System.setProperty("compose.interop.blending", "true")
    }
}