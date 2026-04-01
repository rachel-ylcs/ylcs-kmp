package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable

@Stable
abstract class AssetProvider internal constructor() {
    abstract fun getAsset(id: Any): Asset?

    inline operator fun <reified T : Any> get(id: Any): T? = getAsset(id)?.delegate as? T

    @Stable
    data object Default : AssetProvider() {
        override fun getAsset(id: Any): Asset? = null
    }
}