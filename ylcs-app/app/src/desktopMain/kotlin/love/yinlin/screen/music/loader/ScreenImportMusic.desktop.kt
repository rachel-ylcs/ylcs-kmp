package love.yinlin.screen.music.loader

import love.yinlin.platform.ImplicitPath
import love.yinlin.platform.NormalPath

actual fun processImportMusicDeepLink(deepLink: String): ImplicitPath = NormalPath(deepLink)