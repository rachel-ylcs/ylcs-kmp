package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.ui.component.screen.DialogProgressState
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

actual fun osOpenUrl(url: String) {
    try {
        val application = UIApplication.sharedApplication
        val uri = NSURL(string = url)
        if (application.canOpenURL(uri)) {
            application.openURL(uri)
        }
    }
    catch (_: Throwable) { }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun osDownloadImage(url: String, state: DialogProgressState) {
    Coroutines.io {
        val data = NSData.dataWithContentsOfURL(url = NSURL(string = url))
        if (data != null) {
            val image = UIImage.imageWithData(data = data)
            if (image != null) {
                UIImageWriteToSavedPhotosAlbum(
                    image = image,
                    completionTarget = null,
                    completionSelector = null,
                    contextInfo = null
                )
            }
        }
    }
}