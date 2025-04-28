@file:JvmName("AppContextDesktop")
package love.yinlin.platform

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.Local
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath
import java.awt.GraphicsEnvironment
import java.io.File

class ActualAppContext : AppContext() {
	val rawDensity: Float
	val windowWidth: Float
	val windowHeight: Float
	override val fontScale: Float = 1f
	override val kv: KV = KV()

	init {
		val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
		val bounds = ge.maximumWindowBounds
		val transform = ge.defaultScreenDevice.defaultConfiguration.defaultTransform
		val scaleX = transform.scaleX.toFloat()
		val scaleY = transform.scaleY.toFloat()

		rawDensity = scaleX
		@Suppress("KotlinConstantConditions")
		windowWidth = bounds.width * Local.Client.Desktop.SCREEN_PERCENT / (if (Local.Client.Desktop.ALWAYS_PORTRAIT) 4f else 1f)
		windowHeight = bounds.height * Local.Client.Desktop.SCREEN_PERCENT
		screenWidth = (windowWidth * scaleX).toInt()
		screenHeight = (windowHeight * scaleY).toInt()

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