package love.yinlin.compose.game

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.graphics.AnimatedWebp
import love.yinlin.compose.graphics.decode

@Stable
class Asset private constructor(val value: Any, val isLocal: Boolean) {
    companion object {
        fun image(data: ByteArray, isLocal: Boolean = false): Asset? = ImageBitmap.decode(data)?.let { Asset(it, isLocal) }
        fun animation(data: ByteArray, isLocal: Boolean = false): Asset? = AnimatedWebp.decode(data)?.let { Asset(it, isLocal) }
    }
}