package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.Context

actual fun buildOSStorage(context: Context, appName: String): OSStorage = object : OSStorage() {
    override val dataPath: Path = Path(context.application.filesDir.absolutePath)

    override val cachePath: Path = Path(context.application.cacheDir.absolutePath)

    override val cacheSize: Long get() {
        return 0L
        // TODO:
        //	val sketch = SingletonSketch.get(appNative.context)
        //	return sketch.downloadCache.size + sketch.resultCache.size
    }

    override fun clearCache() {
        //	val sketch = SingletonSketch.get(appNative.context)
        //	sketch.downloadCache.clear()
        //	sketch.resultCache.clear()
    }
}