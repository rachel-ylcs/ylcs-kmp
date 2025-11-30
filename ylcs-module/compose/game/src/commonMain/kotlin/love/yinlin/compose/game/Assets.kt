package love.yinlin.compose.game

import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.graphics.AnimatedWebp

class Assets {
    private val items = mutableMapOf<String, Asset>()

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
    operator fun set(key: String, asset: Asset) { items[key] = asset }
    operator fun get(key: String): Asset? = items[key]

    fun image(key: String): ImageBitmap = items[key]!!.value as ImageBitmap
    fun animation(key: String): AnimatedWebp = items[key]!!.value as AnimatedWebp
}