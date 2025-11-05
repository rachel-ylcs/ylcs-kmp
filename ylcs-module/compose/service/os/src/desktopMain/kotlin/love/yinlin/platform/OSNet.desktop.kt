package love.yinlin.platform

import love.yinlin.Context
import love.yinlin.uri.Uri

actual fun buildOSNet(context: Context): OSNet = object : OSNet() {
    override fun openUri(uri: Uri) = OSUtil.openUri(uri)
}