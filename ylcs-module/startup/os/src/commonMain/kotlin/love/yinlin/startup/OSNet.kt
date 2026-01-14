package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.uri.Uri

abstract class OSNet {
    abstract fun openUri(uri: Uri)
}

expect fun buildOSNet(context: Context): OSNet