package love.yinlin.platform

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.uri.Uri

@Stable
actual fun buildOSNet(context: Context): OSNet = object : OSNet() {
    override fun openUri(uri: Uri) = OSUtil.openUri(uri)
}