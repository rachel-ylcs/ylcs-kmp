package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable

@Stable
class AssetDelegate<T : Any> {
    internal var instance: T? = null
    internal var name: String? = null

    operator fun invoke(): T = instance!!
}