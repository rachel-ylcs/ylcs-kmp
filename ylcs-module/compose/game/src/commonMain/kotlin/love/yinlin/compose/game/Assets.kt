package love.yinlin.compose.game

class Assets {
    private val items = mutableMapOf<String, Asset>()

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
    operator fun set(key: String, asset: Asset) { items[key] = asset }
    operator fun get(key: String): Asset? = items[key]

    fun image(key: String): Asset.Image? = items[key] as? Asset.Image
    fun animation(key: String): Asset.Animation? = items[key] as? Asset.Animation
}