package love.yinlin.compose.game.asset

import androidx.compose.runtime.Stable

@Stable
abstract class AssetLoader<I : Any> {
    /**
     * 资源列表
     */
    internal abstract val assetIDList: List<I>

    /**
     * 并行预加载
     *
     * 是否在不同协程里并行加载资源，例如网络资源可以通过并行加载来提高加载速度
     */
    internal open val parallel: Boolean = false

    /**
     * 加载资源
     */
    internal abstract suspend fun load(raw: Any): Asset

    @Suppress("UNCHECKED_CAST")
    inline fun load(raw: Any, block: (I) -> Asset) = block(raw as I)
}