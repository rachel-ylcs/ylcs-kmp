package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.common.Uri
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual val osPlatform: Platform = Platform.IOS

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean = false

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

actual val osStorageDataPath: Path get() = Path(NSHomeDirectory(), "Documents")

actual val osStorageCachePath: Path get() = Path(NSHomeDirectory(), "tmp")