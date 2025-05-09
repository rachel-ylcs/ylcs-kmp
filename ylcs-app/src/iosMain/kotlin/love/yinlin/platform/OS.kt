package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import love.yinlin.common.Uri
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSFileManager
import platform.Foundation.temporaryDirectory
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

@OptIn(ExperimentalForeignApi::class)
fun copyToTempDir(url: NSURL?): NSURL? {
    if (url == null)
        return null
    val fileManager = NSFileManager.defaultManager
    val tempPath = fileManager.temporaryDirectory.pathComponents?.plus(url.lastPathComponent)
        ?: return null
    val tempUrl = NSURL.fileURLWithPathComponents(tempPath)
        ?: return null
    fileManager.removeItemAtURL(tempUrl, null)
    return if (fileManager.copyItemAtURL(url, tempUrl, null)) tempUrl else null
}