package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object JpegImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte()
    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder = openPlatformImageDecoder(bytes, options.staticOptions())
}
