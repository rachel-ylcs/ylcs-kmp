package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.uri.Uri

abstract class OSApplication {
    abstract suspend fun startAppIntent(uri: Uri): Boolean
    abstract fun copyText(text: String): Boolean
}

expect fun buildOSApplication(context: Context): OSApplication