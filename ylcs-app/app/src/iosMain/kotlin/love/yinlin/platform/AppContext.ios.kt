package love.yinlin.platform

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.compose.data.ImageQuality
import love.yinlin.service
import okio.Path.Companion.toPath

class ActualAppContext : AppContext() {
    override val kv: KV = KV()

    override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
        logger(level = Logger.Level.Error)
        downloadCacheOptions {
            DiskCache.Options(
                appCacheDirectory = service.os.storage.cachePath.toString().toPath(),
                maxSize = 400 * 1024 * 1024
            )
        }
        resultCacheOptions {
            DiskCache.Options(
                appCacheDirectory = service.os.storage.cachePath.toString().toPath(),
                maxSize = 400 * 1024 * 1024
            )
        }
        globalImageOptions(ImageOptions {
            downloadCachePolicy(CachePolicy.ENABLED)
            resultCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
            sizeMultiplier(ImageQuality.Medium.sizeMultiplier)
        })
    }.build()

    override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()
}

val appNative: ActualAppContext get() = app as ActualAppContext