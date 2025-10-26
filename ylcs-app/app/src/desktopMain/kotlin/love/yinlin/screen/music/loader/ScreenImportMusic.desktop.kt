package love.yinlin.screen.music.loader

import love.yinlin.common.uri.ImplicitUri
import love.yinlin.common.uri.RegularUri

actual fun processImportMusicDeepLink(deepLink: String): ImplicitUri = RegularUri(deepLink)