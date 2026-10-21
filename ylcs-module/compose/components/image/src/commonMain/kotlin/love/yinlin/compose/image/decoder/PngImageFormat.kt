package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object PngImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes.decodeToString(1, 4) == "PNG" && bytes[4] == 13.toByte() && bytes[5] == 10.toByte() && bytes[6] == 26.toByte() && bytes[7] == 10.toByte()
    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder = openPlatformImageDecoder(bytes, options.copy(animated = false))
}
