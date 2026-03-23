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
import kotlinx.coroutines.CoroutineScope
import love.yinlin.compose.data.ImageQuality
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.SyncStartup
import love.yinlin.foundation.buildFileClient
import love.yinlin.fs.File
import love.yinlin.platform.Platform
import okio.Path.Companion.toPath

@StartupArg(index = 0, name = "cachePath", type = File::class)
@StartupArg(index = 1, name = "maxCacheSize/MB", type = Int::class)
@StartupArg(index = 2, name = "imageQuality", type = ImageQuality::class)
@Stable
class StartupUrlImage(context: PlatformContextProvider) : SyncStartup(context) {
    private lateinit var sketch: Sketch

    private fun ComponentRegistry.Builder.registerComponent() {
        addFetcher(ComposeResourceUriFetcher.Factory())
        addFetcher(KtorHttpUriFetcher.Factory(KtorStack(client = buildFileClient().delegate)))

        addDecoder(GifDecoder.Factory())
        addDecoder(AnimatedWebpDecoder.Factory())
    }

    override fun init(scope: CoroutineScope, args: StartupArgs) {
        val cachePath: File = args[0]
        val maxCacheSize: Int = args[1]
        val imageQuality: ImageQuality = args[2]
        sketch = buildSketch(context.rawContext).apply {
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
        SingletonSketch.setSafe { sketch }
    }

    fun clearCache() {
        Platform.useNot(*Platform.Web) {
            sketch.downloadCache.clear()
            sketch.resultCache.clear()
        }
    }
}

