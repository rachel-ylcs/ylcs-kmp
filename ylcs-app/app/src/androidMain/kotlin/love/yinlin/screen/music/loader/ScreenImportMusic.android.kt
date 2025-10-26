package love.yinlin.screen.music.loader

import love.yinlin.common.uri.ContentUri
import love.yinlin.common.uri.ImplicitUri

actual fun processImportMusicDeepLink(deepLink: String): ImplicitUri = ContentUri(deepLink)