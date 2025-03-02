package love.yinlin.platform

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import java.awt.GraphicsEnvironment

class AppContext : IAppContext() {
	companion object {
		const val DEBUG_PORTRAIT = true

		const val SCREEN_PERCENT = 0.85f
	}

	var rawDensity: Density? = null
	val windowWidth: Float
	val windowHeight: Float
	override val screenWidth: Int
	override val screenHeight: Int
	override val fontScale: Float = 1f
	override val kv: KV = KV()

	init {
		val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
		val bounds = ge.maximumWindowBounds
		val transform = ge.defaultScreenDevice.defaultConfiguration.defaultTransform
		val scaleX = transform.scaleX.toFloat()
		val scaleY = transform.scaleY.toFloat()
		windowWidth = bounds.width * SCREEN_PERCENT / (if (DEBUG_PORTRAIT) 4f else 1f)
		windowHeight = bounds.height * SCREEN_PERCENT
		screenWidth = (windowWidth * scaleX).toInt()
		screenHeight = (windowHeight * scaleY).toInt()
	}
}

@Stable
val appNative: AppContext get() = app as AppContext