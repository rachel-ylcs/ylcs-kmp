package love.yinlin.compose.image.cache

import love.yinlin.compose.image.data.ImageResult

internal class ImageMemoryCache(private val maxBytes: Long) {
    private val entries = LinkedHashMap<String, ImageResult>()
    var size: Long = 0L
        private set

    fun get(key: String): ImageResult? {
        val result = entries.remove(key) ?: return null
        entries[key] = result
        return result.fromMemory()
    }

    fun put(key: String, result: ImageResult) {
        entries.remove(key)?.let { size -= it.byteCount }
        if (result.byteCount > maxBytes) return
        entries[key] = result
        size += result.byteCount

        while (size > maxBytes) {
            val eldest = entries.entries.first()
            size -= eldest.value.byteCount
            entries.remove(eldest.key)
        }
    }

    fun clear() {
        entries.clear()
        size = 0L
    }
}
