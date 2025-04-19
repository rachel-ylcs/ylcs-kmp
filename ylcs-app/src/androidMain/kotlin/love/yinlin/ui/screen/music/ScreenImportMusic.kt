package love.yinlin.ui.screen.music

import love.yinlin.platform.ContentPath
import love.yinlin.platform.ImplicitPath

actual fun processImportMusicDeepLink(deepLink: String): ImplicitPath = ContentPath(deepLink)