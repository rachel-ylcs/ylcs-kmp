package love.yinlin.screen.music.loader

import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri

actual fun processImportMusicDeepLink(deepLink: String): ImplicitUri = ContentUri(deepLink)