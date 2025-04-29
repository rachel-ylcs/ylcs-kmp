@file:JvmName("AppContextAndroid")
package love.yinlin.platform

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath

class ActualAppContext(val context: Context) : AppContext() {
	companion object {
		val DesignWidth = 360.dp
		val DesignHeight = 800.dp
	}

	override val kv: KV = KV(context)

	override fun densityWrapper(newWidth: Dp, newHeight: Dp, oldDensity: Density): Density {
		val metrics = context.resources.displayMetrics
		return Density(
			density = (if (newWidth <= newHeight) metrics.widthPixels else metrics.heightPixels) / DesignWidth.value,
			fontScale = oldDensity.fontScale
		)
	}

	var activityResultRegistry: ActivityResultRegistry? = null

	override fun initializeSketch(): Sketch = Sketch.Builder(context).apply {
		logger(level = Logger.Level.Error)
		downloadCacheOptions {
			DiskCache.Options(
				appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
				maxSize = 300 * 1024 * 1024
			)
		}
		resultCacheOptions {
			DiskCache.Options(
				appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
				maxSize = 300 * 1024 * 1024
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