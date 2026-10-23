package love.yinlin.compose.image.cache

import love.yinlin.compose.image.data.ImageResult
import love.yinlin.compose.image.data.ImageRequest

internal class ImageMemoryCache(private val maxBytes: Long) {
    private val entries = LinkedHashMap<String, ImageMemoryCacheEntry>()
    private val groups = mutableMapOf<String, MutableSet<String>>()
    var size: Long = 0L
        private set

    fun get(key: String, group: String, request: ImageRequest): ImageResult? {
        var selected = key
        var entry = entries[key]

        if (entry == null) {
            for (candidate in groups[group] ?: return null) {
                val cached = entries[candidate] ?: continue
                if (request.canReuse(cached.result) && (entry == null || cached.result.byteCount < entry.result.byteCount)) {
                    selected = candidate
                    entry = cached
                }
            }
        }

        val found = entry ?: return null
        entries.remove(selected)
        entries[selected] = found

        return found.memoryResult
    }

    private fun remove(key: String) {
        val entry = entries.remove(key) ?: return
        size -= entry.result.byteCount

        groups[entry.group]?.let {
            it.remove(key)
            if (it.isEmpty()) groups.remove(entry.group)
        }
    }

    fun put(key: String, group: String, result: ImageResult) {
        remove(key)

        if (result.byteCount > maxBytes) return
        entries[key] = ImageMemoryCacheEntry(group, result)
        groups.getOrPut(group) { mutableSetOf() }.add(key)
        size += result.byteCount

        while (size > maxBytes) {
            val eldest = entries.entries.first()
            remove(eldest.key)
        }
    }

    fun clear() {
        entries.clear()
        groups.clear()
        size = 0L
    }
}
