package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.StartupArgs
import love.yinlin.SyncStartup

@Stable
class StartupComposeSwingRender : SyncStartup() {
    override fun init(context: Context, args: StartupArgs) {
        System.setProperty("compose.swing.render.on.graphics", "true")
        System.setProperty("compose.interop.blending", "true")
    }
}