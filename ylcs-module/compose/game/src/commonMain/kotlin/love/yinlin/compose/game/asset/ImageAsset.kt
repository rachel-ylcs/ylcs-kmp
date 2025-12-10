package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.graphics.decode

@Stable
class ImageAsset internal constructor(version: Int?) : Asset<ImageBitmap, ByteArray>(version) {
    override val type: String = "webp"
    override suspend fun build(input: ByteArray): ImageBitmap = ImageBitmap.decode(input)!!

    companion object {
        fun buildImmediately(input: ByteArray) = ImageBitmap.decode(input)!!
    }
}