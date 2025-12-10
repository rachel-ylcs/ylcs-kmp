package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable
import love.yinlin.compose.graphics.AnimatedWebp

@Stable
class AnimationAsset internal constructor(version: Int?) : Asset<AnimatedWebp, ByteArray>(version) {
    override val type: String = "webp"
    override suspend fun build(input: ByteArray): AnimatedWebp = AnimatedWebp.decode(input)!!

    companion object {
        fun buildImmediately(input: ByteArray) = AnimatedWebp.decode(input)!!
    }
}