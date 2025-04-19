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

actual object Picker {
    actual suspend fun pickPicture(): Source? = TODO()
    actual suspend fun pickPicture(maxNum: Int): Sources<Source>? = TODO()
    actual suspend fun pickFile(mimeType: List<String>, filter: List<String>): Source? = TODO()
    actual suspend fun pickPath(mimeType: List<String>, filter: List<String>): ImplicitPath? = TODO()

    actual suspend fun prepareSavePicture(filename: String): Pair<Any, Sink>? {
        val buffer = Buffer()
        return buffer to buffer
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun actualSavePicture(filename: String, origin: Any, sink: Sink) {
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

    actual suspend fun cleanSavePicture(origin: Any, result: Boolean) = Unit
}