package love.yinlin.ui.screen.music.loader

import love.yinlin.platform.ImplicitPath
import love.yinlin.platform.SandboxPath
import platform.Foundation.NSURL

actual fun processImportMusicDeepLink(deepLink: String): ImplicitPath = SandboxPath(NSURL(string = deepLink))