package love.yinlin.platform

import love.yinlin.Context
import love.yinlin.uri.Uri

abstract class OSApplication {
    abstract suspend fun startAppIntent(uri: Uri): Boolean
    abstract fun copyText(text: String): Boolean
}

expect fun buildOSApplication(context: Context): OSApplication