package love.yinlin.platform

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.PlatformContext
import com.github.panpf.sketch.Sketch
import com.github.panpf.sketch.cache.CachePolicy
import com.github.panpf.sketch.request.ImageOptions
import com.github.panpf.sketch.util.Logger
import kotlinx.browser.window

class ActualAppContext : AppContext(run {
	PhysicalGraphics(
		width = window.innerWidth,
		height = window.innerHeight,
		density = Density(
			density = window.devicePixelRatio.toFloat(),
			fontScale = 1f
		)
	)
}) {
	override fun densityWrapper(newWidth: Dp, newHeight: Dp, oldDensity: Density): Density = oldDensity

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