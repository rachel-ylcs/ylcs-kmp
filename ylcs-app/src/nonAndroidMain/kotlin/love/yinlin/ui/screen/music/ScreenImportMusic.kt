package love.yinlin.ui.screen.music

import love.yinlin.platform.ImplicitPath
import love.yinlin.platform.NormalPath

actual fun processImportMusicDeepLink(deepLink: String): ImplicitPath = NormalPath(deepLink)