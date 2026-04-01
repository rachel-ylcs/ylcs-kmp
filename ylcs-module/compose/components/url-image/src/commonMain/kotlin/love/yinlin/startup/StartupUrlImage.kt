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
import love.yinlin.foundation.Startup
import love.yinlin.foundation.StartupFactory
import love.yinlin.foundation.StartupID
import love.yinlin.foundation.StartupPool
import love.yinlin.foundation.buildFileClient
import love.yinlin.fs.File
import love.yinlin.platform.Platform
import okio.Path.Companion.toPath

@Stable
class StartupUrlImage(
    pool: StartupPool,
    private val cachePath: File,
    private val maxCacheSize: Int,
    private val imageQuality: ImageQuality
) : Startup(pool) {
    class Factory(
        private val cachePath: File,
        private val maxCacheSize: Int,
        private val imageQuality: ImageQuality
    ) : StartupFactory<StartupUrlImage> {
        override val id: String = StartupID<StartupUrlImage>()
        override val dependencies: List<String> = emptyList()
        override fun build(pool: StartupPool): StartupUrlImage = StartupUrlImage(pool, cachePath, maxCacheSize, imageQuality)
    }

    private var sketch: Sketch? = null

    private fun ComponentRegistry.Builder.registerComponent() {
        addFetcher(ComposeResourceUriFetcher.Factory())
        addFetcher(KtorHttpUriFetcher.Factory(KtorStack(client = buildFileClient().delegate)))

        addDecoder(GifDecoder.Factory())
        addDecoder(AnimatedWebpDecoder.Factory())
    }

    override suspend fun init() {
        val newSketch = buildSketch(pool.rawContext).apply {
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
        }.build()
        sketch = newSketch
        SingletonSketch.setSafe { newSketch }
    }

    fun clearCache() {
        Platform.useNot(*Platform.Web) {
            sketch?.let {
                it.downloadCache.clear()
                it.resultCache.clear()
            }
        }
    }
}