package love.yinlin.platform

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.uri.Uri

@Stable
abstract class OSNet {
    abstract fun openUri(uri: Uri)
}

@Stable
expect fun buildOSNet(context: Context): OSNet