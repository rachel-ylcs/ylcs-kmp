package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.io.files.Path
import love.yinlin.common.uri.Uri
import love.yinlin.common.toNSUrl
import love.yinlin.extension.catching
import love.yinlin.extension.catchingDefault
import platform.Foundation.*
import platform.UIKit.UIApplication
import platform.UIKit.UIPasteboard

actual suspend fun osApplicationStartAppIntent(uri: Uri): Boolean = catchingDefault(false) {
    val application = UIApplication.sharedApplication
    val url = uri.toNSUrl()
    require(application.canOpenURL(url))
    application.openURL(url)
}

actual fun osApplicationCopyText(text: String): Boolean {
    UIPasteboard.generalPasteboard.setString(text)
    return true
}

actual fun osNetOpenUrl(url: String) = catching {
    NSURL.URLWithString(url)?.let {
        UIApplication.sharedApplication.openURL(it)
    }
}

fun osStorageSearchPath(directory: NSSearchPathDirectory): Path {
    val paths = NSSearchPathForDirectoriesInDomains(directory, NSUserDomainMask, true)
    return Path(paths[0]!! as String)
}

actual val osStorageDataPath: Path get() = osStorageSearchPath(NSDocumentDirectory)

actual val osStorageCachePath: Path get() = osStorageSearchPath(NSCachesDirectory)

val osStorageTempPath: Path get() = Path(NSTemporaryDirectory())

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