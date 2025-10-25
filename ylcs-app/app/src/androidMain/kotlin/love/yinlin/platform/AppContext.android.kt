package love.yinlin.platform

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.AppService
import love.yinlin.compose.data.ImageQuality
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath

class ActualAppContext(val context: Context) : AppContext() {
	override val kv: KV = KV(context)

    var activity: Activity? = null
	var activityResultRegistry: ActivityResultRegistry? = null

	override fun initializeSketch(): Sketch = Sketch.Builder(context).apply {
		logger(level = Logger.Level.Error)
		downloadCacheOptions {
			DiskCache.Options(
				appCacheDirectory = AppService.os.storage.cachePath.toString().toPath(),
				maxSize = 400 * 1024 * 1024
			)
		}
		resultCacheOptions {
			DiskCache.Options(
				appCacheDirectory = AppService.os.storage.cachePath.toString().toPath(),
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

	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory(context)

	override fun initialize() {
		super.initialize()
		// 注册异常回调
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${e.stackTraceToString()}")
		}
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext