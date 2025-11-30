package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.graphics.decode

@Stable
class Asset private constructor(val value: Any) {
    companion object {
        fun decodeImage(data: ByteArray): Asset? = ImageBitmap.decode(data)?.let { Asset(it) }
        fun decodeAnimation(data: ByteArray): Asset? = AnimatedWebp.decode(data)?.let { Asset(it) }
    }
}