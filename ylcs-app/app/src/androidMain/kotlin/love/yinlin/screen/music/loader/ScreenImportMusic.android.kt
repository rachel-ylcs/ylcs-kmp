package love.yinlin.screen.music.loader

import love.yinlin.app
import love.yinlin.uri.ContentUri
import love.yinlin.uri.ImplicitUri

actual fun processImportMusicDeepLink(deepLink: String): ImplicitUri = ContentUri(app.context.application, deepLink)