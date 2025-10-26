package love.yinlin.screen.music.loader

import love.yinlin.uri.ImplicitUri
import love.yinlin.uri.RegularUri

actual fun processImportMusicDeepLink(deepLink: String): ImplicitUri = RegularUri(deepLink)