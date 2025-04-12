package love.yinlin.platform

import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.get

actual val osStorageCacheSize: Long get() {
    val sketch = SingletonSketch.get()
    return sketch.downloadCache.size + sketch.resultCache.size
}

actual fun osStorageClearCache() {
    val sketch = SingletonSketch.get()
    sketch.downloadCache.clear()
    sketch.resultCache.clear()
}