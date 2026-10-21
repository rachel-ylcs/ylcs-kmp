package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object GifImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.size >= 6 && bytes.decodeToString(0, 6) in ["GIF87a", "GIF89a"]
    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder = openPlatformImageDecoder(bytes, options)
}
