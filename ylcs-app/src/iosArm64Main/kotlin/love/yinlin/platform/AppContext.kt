package love.yinlin.platform

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSException
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
class ActualAppContext : AppContext(run {
	val screenBounds = UIScreen.mainScreen.bounds
	PhysicalGraphics(
		width = CGRectGetWidth(screenBounds).toInt(),
		height = CGRectGetHeight(screenBounds).toInt(),
		density = Density(
			density = UIScreen.mainScreen.scale.toFloat(),
			fontScale = 1f
		)
	)
}) {
	companion object {
		val DesignWidth = 360.dp
		val DesignHeight = 800.dp
	}

	override fun densityWrapper(newWidth: Dp, newHeight: Dp, oldDensity: Density): Density = Density(
		if (newWidth <= newHeight) physics.width / DesignWidth.value else physics.height / DesignHeight.value,
		oldDensity.fontScale
	)

	override val kv: KV = KV()

	@OptIn(ExperimentalForeignApi::class)
	private val exceptionHandler = NSUncaughtExceptionHandler({ e: NSException? ->
		e?.let { ex ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${ex.reason}\n${ex.callStackSymbols.joinToString(",")}")
		}
	}.objcPtr())

	override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
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

	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()

	override fun initialize() {
		super.initialize()
		// 注册异常回调
		NSSetUncaughtExceptionHandler(exceptionHandler.ptr)
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext