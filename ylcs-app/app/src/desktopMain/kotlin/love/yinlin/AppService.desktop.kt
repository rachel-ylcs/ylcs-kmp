package love.yinlin

import love.yinlin.compose.screen.DeepLink
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import love.yinlin.service.StartupDelegate
import love.yinlin.startup.StartupComposeSwingRender
import love.yinlin.startup.StartupMacOSDeepLink
import love.yinlin.startup.StartupSingleInstance
import kotlin.io.path.Path

actual val service = object : AppService() {
    private val loadNativeLibrary by sync(order = StartupDelegate.DEFAULT_ORDER - 1) { System.loadLibrary("ylcs_native") }

    private val setupVLC by sync {
        val vlcPath = Path(System.getProperty("compose.application.resources.dir")).parent.parent.let {
            when (platform) {
                Platform.Windows -> it.resolve("vlc")
                Platform.Linux -> it.resolve("bin/vlc")
                Platform.MacOS -> it.resolve("MacOS/vlc")
                else -> it
            }
        }
        System.setProperty("jna.library.path", vlcPath.toString())
    }

    private val singleInstance by service(factory = ::StartupSingleInstance)

    private val setComposeRender by service(factory = ::StartupComposeSwingRender)

    private val setupMacOSDeepLink by service(
        StartupMacOSDeepLink.Handler { uri ->
            DeepLink.openUri(uri)
        },
        factory = ::StartupMacOSDeepLink
    )
}