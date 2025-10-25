package love.yinlin.platform

import love.yinlin.common.uri.Uri
import love.yinlin.service.PlatformContext

actual fun buildOSNet(context: PlatformContext): OSNet = object : OSNet() {
    override fun openUri(uri: Uri) {
        OSUtil.openUri(uri)
    }
}