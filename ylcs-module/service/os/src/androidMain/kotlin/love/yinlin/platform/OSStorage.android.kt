package love.yinlin.platform

import kotlinx.io.files.Path
import love.yinlin.service.PlatformContext

actual fun buildOSStorage(context: PlatformContext, appName: String): OSStorage = object : OSStorage() {
    override val dataPath: Path = Path(context.filesDir.absolutePath)

    override val cachePath: Path = Path(context.cacheDir.absolutePath)

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