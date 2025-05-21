@file:JvmName("AppContextDesktop")
package love.yinlin.platform

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.cache.DiskCache
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import love.yinlin.AppModel
import love.yinlin.common.Weak
import love.yinlin.extension.DateEx
import okio.Path.Companion.toPath
import java.io.File

class ActualAppContext : AppContext() {
	companion object {
		init {
			// 本机库
			System.loadLibrary("ylcs_native")
			// VLC
			System.setProperty("jna.library.path", "${System.getProperty("user.dir")}${File.separator}vlc")
		}
	}

	override val kv: KV = KV()
	override var model: AppModel? by Weak()

	override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
		logger(level = Logger.Level.Error)
		downloadCacheOptions {
			DiskCache.Options(
				appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
				maxSize = 1024 * 1024 * 1024
			)
		}
		resultCacheOptions {
			DiskCache.Options(
				appCacheDirectory = OS.Storage.cachePath.toString().toPath(),
				maxSize = 1024 * 1024 * 1024
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
		// 创建悬浮歌词
		appNative.musicFactory.floatingLyrics = ActualFloatingLyrics().apply { attach() }
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext