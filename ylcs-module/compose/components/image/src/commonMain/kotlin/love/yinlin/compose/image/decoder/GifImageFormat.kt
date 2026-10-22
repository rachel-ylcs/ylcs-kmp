package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
object GifImageFormat : ImageFormat {
    override fun matches(bytes: ByteArray): Boolean = bytes.matchesImageSignature(0, "GIF87a") || bytes.matchesImageSignature(0, "GIF89a")
    override fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder = openPlatformImageDecoder(bytes, options)
}
