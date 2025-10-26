package love.yinlin.platform

import love.yinlin.uri.Uri
import love.yinlin.service.PlatformContext

abstract class OSApplication {
    abstract suspend fun startAppIntent(uri: Uri): Boolean
    abstract fun copyText(text: String): Boolean
}

expect fun buildOSApplication(context: PlatformContext): OSApplication