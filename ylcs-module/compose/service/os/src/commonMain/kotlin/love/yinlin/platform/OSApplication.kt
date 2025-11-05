package love.yinlin.platform

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.uri.Uri

@Stable
abstract class OSApplication {
    abstract suspend fun startAppIntent(uri: Uri): Boolean
    abstract fun copyText(text: String): Boolean
}

@Stable
expect fun buildOSApplication(context: Context): OSApplication