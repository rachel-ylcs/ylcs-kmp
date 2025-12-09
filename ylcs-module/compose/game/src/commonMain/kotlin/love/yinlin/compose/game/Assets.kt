package love.yinlin.compose.game

import love.yinlin.extension.lazyName

class Assets {
    private val items = mutableMapOf<String, Asset>()

    val size: Int get() = items.size
    val isEmpty: Boolean get() = items.isEmpty()
    operator fun set(key: String, asset: Asset) { items[key] = asset }
    @Suppress("unchecked_cast")
    operator fun <T : Any> get(key: String) = items[key]!!.value as T
    @Suppress("unchecked_cast")
    operator fun <T : Any> invoke() = lazyName { key -> items[key]!!.value as T }
    fun clearLocal() = items.entries.removeAll { it.value.isLocal }
}