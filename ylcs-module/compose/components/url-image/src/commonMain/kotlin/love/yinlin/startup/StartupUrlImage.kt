package love.yinlin.startup

import androidx.compose.runtime.Stable
import com.github.panpf.sketch.ComponentRegistry
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.decode.AnimatedWebpDecoder
import com.github.panpf.sketch.decode.GifDecoder
import com.github.panpf.sketch.fetch.ComposeResourceUriFetcher
import com.github.panpf.sketch.fetch.KtorHttpUriFetcher
import com.github.panpf.sketch.http.KtorStack
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.compose.data.ImageQuality
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.SyncStartupFactory
import love.yinlin.foundation.buildFileClient
import love.yinlin.fs.File
import love.yinlin.platform.Platform
import okio.Path.Companion.toPath

@Stable
class StartupUrlImage(
    pool: StartupPool,
    cachePath: File,
    maxCacheSize: Int,
    imageQuality: ImageQuality
) : SyncStartup(pool) {
    class Factory(
        private val cachePath: File,
        private val maxCacheSize: Int,
        private val imageQuality: ImageQuality
    ) : SyncStartupFactory<StartupUrlImage>() {
        override val id: String = StartupID<StartupUrlImage>()
        override fun build(pool: StartupPool): StartupUrlImage = StartupUrlImage(pool, cachePath, maxCacheSize, imageQuality)
    }

    private fun ComponentRegistry.Builder.registerComponent() {
        addFetcher(ComposeResourceUriFetcher.Factory())
        addFetcher(KtorHttpUriFetcher.Factory(KtorStack(client = buildFileClient().delegate)))

        addDecoder(GifDecoder.Factory())
        addDecoder(AnimatedWebpDecoder.Factory())
    }

    private val sketch: Sketch = buildSketch(pool.rawContext).apply {
        logger(level = Logger.Level.Error)
        componentLoaderEnabled(false)
        components {
            registerComponent()
        }
        Platform.useNot(*Platform.Web) {
            downloadCacheOptions {
                DiskCache.Options(
                    appCacheDirectory = cachePath.path.toPath(),
                    maxSize = maxCacheSize * 1024 * 1024L
                )
            }
            resultCacheOptions {
                DiskCache.Options(
                    appCacheDirectory = cachePath.path.toPath(),
                    maxSize = maxCacheSize * 1024 * 1024L
                )
            }
        }
        globalImageOptions(ImageOptions {
            downloadCachePolicy(CachePolicy.ENABLED)
            resultCachePolicy(CachePolicy.ENABLED)
            memoryCachePolicy(CachePolicy.ENABLED)
            sizeMultiplier(imageQuality.sizeMultiplier)
        })
    }.build().also { rawSketch ->
        SingletonSketch.setSafe { rawSketch }
    }

    fun clearCache() {
        Platform.useNot(*Platform.Web) {
            sketch.downloadCache.clear()
            sketch.resultCache.clear()
        }
    }
}