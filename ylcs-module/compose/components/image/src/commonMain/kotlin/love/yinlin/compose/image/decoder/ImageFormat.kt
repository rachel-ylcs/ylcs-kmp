package love.yinlin.compose.image.decoder

import androidx.compose.runtime.Stable
import love.yinlin.compose.image.data.ImageDecodeOptions

@Stable
interface ImageFormat {
    fun matches(bytes: ByteArray): Boolean
    fun open(bytes: ByteArray, options: ImageDecodeOptions): ImageDecoder
}
