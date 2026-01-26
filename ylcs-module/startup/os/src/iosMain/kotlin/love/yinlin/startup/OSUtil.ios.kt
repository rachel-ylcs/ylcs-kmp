package love.yinlin.startup

import love.yinlin.uri.Uri
import love.yinlin.uri.toNSUrl
import platform.UIKit.UIApplication

internal object OSUtil {
    fun openUri(uri: Uri): Boolean {
        val application = UIApplication.sharedApplication
        val url = uri.toNSUrl()!!
        application.canOpenURL(url)
        return application.openURL(url)
    }
}