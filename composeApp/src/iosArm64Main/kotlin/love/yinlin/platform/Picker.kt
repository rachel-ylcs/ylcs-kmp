package love.yinlin.platform

import io.ktor.utils.io.core.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.pin
import kotlinx.io.Buffer
import kotlinx.io.Sink
import kotlinx.io.Source
import love.yinlin.extension.Sources
import platform.Foundation.NSData
import platform.Foundation.dataWithBytesNoCopy
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum

actual object PicturePicker {
    actual suspend fun pick(): Source? = null
    actual suspend fun pick(maxNum: Int): Sources<Source>? = null

    actual suspend fun prepareSave(filename: String): Pair<Any, Sink>? {
        val buffer = Buffer()
        return buffer to buffer
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun actualSave(filename: String, origin: Any, sink: Sink) {
        Coroutines.io {
            val bytes = (origin as Buffer).readBytes()
            val data = NSData.dataWithBytesNoCopy(bytes.pin().addressOf(0), bytes.size.toULong())
            UIImage.imageWithData(data = data)
        }?.let {
            UIImageWriteToSavedPhotosAlbum(
                image = it,
                completionTarget = null,
                completionSelector = null,
                contextInfo = null
            )
        }
    }

    actual suspend fun cleanSave(origin: Any, result: Boolean) = Unit
}