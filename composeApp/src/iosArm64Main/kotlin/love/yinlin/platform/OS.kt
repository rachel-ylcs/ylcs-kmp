package love.yinlin.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual val osPlatform: Platform = Platform.IOS

actual fun osNetOpenUrl(url: String) {
    try {
        val application = UIApplication.sharedApplication
        val uri = NSURL(string = url)
        if (application.canOpenURL(uri)) {
            application.openURL(uri)
        }
    }
    catch (_: Throwable) { }
}