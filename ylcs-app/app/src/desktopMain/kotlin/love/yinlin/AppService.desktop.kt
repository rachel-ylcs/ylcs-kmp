package love.yinlin

import love.yinlin.compose.screen.DeepLink
import love.yinlin.platform.Platform
import love.yinlin.platform.platform
import love.yinlin.startup.StartupComposeSwingRender
import love.yinlin.startup.StartupMacOSDeepLink
import love.yinlin.startup.StartupSingleInstance
import kotlin.io.path.Path

actual val service = object : AppService() {
    val loadNativeLibrary by sync { System.loadLibrary("ylcs_native") }

    val setupVLC by sync {
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

    val singleInstance by service(factory = ::StartupSingleInstance)

    val setComposeRender by service(factory = ::StartupComposeSwingRender)

    val setupMacOSDeepLink by service(StartupMacOSDeepLink.Handler { uri -> DeepLink.openUri(uri) }, factory = ::StartupMacOSDeepLink)
}