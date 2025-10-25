package love.yinlin.platform

import love.yinlin.common.uri.Uri
import love.yinlin.common.uri.toNSUrl
import platform.UIKit.UIApplication

internal data object OSUtil {
    fun openUri(uri: Uri): Boolean {
        val application = UIApplication.sharedApplication
        val url = uri.toNSUrl()!!
        application.canOpenURL(url)
        return application.openURL(url)
    }
}