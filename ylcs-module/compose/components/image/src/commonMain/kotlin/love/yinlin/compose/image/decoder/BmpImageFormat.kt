package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object BmpImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.size >= 2 && bytes[0] == 66.toByte() && bytes[1] == 77.toByte()
    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder = openPlatformImageDecoder(bytes, if (options.animated) options.copy(animated = false) else options)
}
