package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object WebpImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.size >= 12 && bytes.decodeToString(0, 4) == "RIFF" && bytes.decodeToString(8, 12) == "WEBP"
    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder = openPlatformImageDecoder(bytes, options)
}
