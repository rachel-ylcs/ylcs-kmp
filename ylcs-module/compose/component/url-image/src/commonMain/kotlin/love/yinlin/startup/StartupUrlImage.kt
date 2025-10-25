package love.yinlin.startup

import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.request.PauseLoadWhenScrollingDecodeInterceptor
import com.github.panpf.sketch.util.Logger
import kotlinx.io.files.Path
import love.yinlin.compose.data.ImageQuality
import love.yinlin.platform.Platform
import love.yinlin.service.*
import okio.Path.Companion.toPath

@StartupFetcher(index = 0, name = "cachePath", returnType = Path::class)
@StartupArg(index = 1, name = "maxCacheSize/MB", type = Int::class)
@StartupArg(index = 2, name = "imageQuality", type = ImageQuality::class)
class StartupUrlImage : SyncStartup {
    override fun init(context: PlatformContext, args: StartupArgs) {
        val cachePath: Path = args.fetch(0)
        val maxCacheSize: Int = args[1]
        val imageQuality: ImageQuality = args[2]
        SingletonSketch.setSafe { buildSketch(context).apply {
            logger(level = Logger.Level.Error)
            components {
                addDecodeInterceptor(PauseLoadWhenScrollingDecodeInterceptor())
            }
            Platform.useNot(Platform.WebWasm) {
                downloadCacheOptions {
                    DiskCache.Options(
                        appCacheDirectory = cachePath.toString().toPath(),
                        maxSize = maxCacheSize * 1024 * 1024L
                    )
                }
                resultCacheOptions {
                    DiskCache.Options(
                        appCacheDirectory = cachePath.toString().toPath(),
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
        }.build() }
    }
}

