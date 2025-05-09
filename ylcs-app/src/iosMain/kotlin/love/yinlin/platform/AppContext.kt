package love.yinlin.platform

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import love.yinlin.AppModel
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
class ActualAppContext : AppContext() {
    override val kv: KV = KV()
    override var model: AppModel? = null

    override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
        logger(level = Logger.Level.Error)
        downloadCacheOptions {
            DiskCache.Options(
                appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
                maxSize = 400 * 1024 * 1024
            )
        }
        resultCacheOptions {
            DiskCache.Options(
                appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
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

    @OptIn(ExperimentalNativeApi::class)
    override fun initialize() {
        super.initialize()
        // 注册异常回调
        setUnhandledExceptionHook { e ->
            kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${e.stackTraceToString()}")
        }
        val exceptionHandler: CPointer<NSUncaughtExceptionHandler> = staticCFunction { e ->
            e?.let { ex ->
                appNative.kv.set(CRASH_KEY,
                    "${DateEx.CurrentString}\n${ex.reason}\n${ex.callStackSymbols.joinToString(",")}")
            }
        }
        NSSetUncaughtExceptionHandler(exceptionHandler)
    }
}

val appNative: ActualAppContext get() = app as ActualAppContext