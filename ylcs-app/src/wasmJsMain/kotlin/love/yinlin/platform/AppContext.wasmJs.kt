@file:OptIn(ExperimentalWasmJsInterop::class)
package love.yinlin.platform

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import kotlinx.browser.window
import love.yinlin.extension.DateEx

class ActualAppContext : AppContext() {
	override val kv: KV = KV()

	override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
		logger(level = Logger.Level.Error)
		globalImageOptions(ImageOptions {
			memoryCachePolicy(CachePolicy.ENABLED)
			sizeMultiplier(ImageQuality.Medium.sizeMultiplier)
		})
	}.build()

	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()

    override fun initialize() {
		super.initialize()
		// 注册异常回调
		window.onerror = { message, source, lineno, colno, error ->
			val errorString = """
				${DateEx.CurrentString}
				$source - $lineno - $colno
				$message
				${error?.toThrowableOrNull()}
			""".trimIndent()
			kv.set(CRASH_KEY, errorString)
			println(errorString)
			false.toJsBoolean()
		}
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext