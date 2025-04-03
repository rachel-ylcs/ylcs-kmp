package love.yinlin.platform

import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
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

actual val osStorageCachePath: String get() = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String