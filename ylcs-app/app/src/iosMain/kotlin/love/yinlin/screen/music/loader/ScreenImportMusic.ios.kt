package love.yinlin.screen.music.loader

import love.yinlin.common.uri.ImplicitUri
import love.yinlin.common.uri.SandboxUri
import platform.Foundation.NSURL

actual fun processImportMusicDeepLink(deepLink: String): ImplicitUri = SandboxUri(NSURL(string = deepLink))