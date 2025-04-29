@file:JvmName("AppContextDesktop")
package love.yinlin.platform

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath
import java.awt.GraphicsEnvironment
import java.io.File

class ActualAppContext : AppContext(run {
	val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
	val bounds = ge.defaultScreenDevice.displayMode
	val density = ge.defaultScreenDevice.defaultConfiguration.defaultTransform.scaleX.toFloat()
	PhysicalGraphics(width = bounds.width, height = bounds.height, density = Density(density, 1f))
}) {
	override val kv: KV = KV()

	override fun densityWrapper(newWidth: Dp, newHeight: Dp, oldDensity: Density): Density = oldDensity

	init {
		// 本机库
		System.loadLibrary("ylcs")

		// VLC
		System.setProperty("jna.library.path", "${System.getProperty("user.dir")}${File.separator}vlc")
	}

	override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
		logger(level = Logger.Level.Error)
		downloadCacheOptions {
			DiskCache.Options(
				appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
				maxSize = 500 * 1024 * 1024
			)
		}
		resultCacheOptions {
			DiskCache.Options(
				appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
				maxSize = 500 * 1024 * 1024
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
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${e.stackTraceToString()}")
		}
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext