package love.yinlin.platform

import androidx.compose.ui.unit.Density
import love.yinlin.Local
import love.yinlin.extension.DateEx
import java.awt.GraphicsEnvironment

class AppContext : AppContextBase() {
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
		@Suppress("KotlinConstantConditions")
		windowWidth = bounds.width * Local.Client.Desktop.SCREEN_PERCENT / (if (Local.Client.Desktop.ALWAYS_PORTRAIT) 4f else 1f)
		windowHeight = bounds.height * Local.Client.Desktop.SCREEN_PERCENT
		screenWidth = (windowWidth * scaleX).toInt()
		screenHeight = (windowHeight * scaleY).toInt()
	}

	override fun initialize(): AppContext {
		super.initialize()
		// 注册异常回调
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${e.stackTraceToString()}")
		}
        return this
	}
}

val appNative: AppContext get() = app as AppContext