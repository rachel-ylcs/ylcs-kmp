package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.uri.Uri

actual fun buildOSNet(context: Context): OSNet = object : OSNet() {
    override fun openUri(uri: Uri) {
        OSUtil.openUri(uri)
    }
}