package love.yinlin.platform

import love.yinlin.uri.Uri
import love.yinlin.service.PlatformContext

abstract class OSNet {
    abstract fun openUri(uri: Uri)
}

expect fun buildOSNet(context: PlatformContext): OSNet