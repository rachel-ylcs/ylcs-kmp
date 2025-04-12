package love.yinlin.platform

import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import kotlinx.browser.window

class ActualAppContext : AppContext() {
	override val screenWidth: Int = window.innerWidth
	override val screenHeight: Int = window.innerHeight
	override val fontScale: Float = 1f
	override val kv: KV = KV()

	override fun initializeSketch(): Sketch = Sketch.Builder(PlatformContext.INSTANCE).apply {
		logger(level = Logger.Level.Error)
		globalImageOptions(ImageOptions {
			memoryCachePolicy(CachePolicy.ENABLED)
			sizeMultiplier(ImageQuality.Medium.sizeMultiplier)
		})
	}.build()

	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()
}

val appNative: ActualAppContext get() = app as ActualAppContext