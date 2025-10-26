package love.yinlin.screen.music.loader

import love.yinlin.platform.ContentPath
import love.yinlin.platform.ImplicitPath

actual fun processImportMusicDeepLink(deepLink: String): ImplicitPath = ContentPath(deepLink)