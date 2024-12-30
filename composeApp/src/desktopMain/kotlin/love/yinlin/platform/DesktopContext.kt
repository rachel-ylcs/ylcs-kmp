package love.yinlin.platform

import java.awt.GraphicsEnvironment

class DesktopContext : AppContext() {
	companion object {
		const val APP_RATIO = 0.85f
	}

	val windowWidth: Float
	val windowHeight: Float
	override val screenWidth: Int
	override val screenHeight: Int
	override val fontScale: Float = 0.8f

	init {
		val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
		val bounds = ge.maximumWindowBounds
		val transform = ge.defaultScreenDevice.defaultConfiguration.defaultTransform
		val scaleX = transform.scaleX.toFloat()
		val scaleY = transform.scaleY.toFloat()
		windowWidth = bounds.width * APP_RATIO
		windowHeight = bounds.height * APP_RATIO
		screenWidth = (windowWidth * scaleX).toInt()
		screenHeight = (windowHeight * scaleY).toInt()
	}
}

actual val platform: Platform = run {
	val name = System.getProperty("os.name")
	when {
		name.startsWith("Windows") -> Platform.Windows
		name.startsWith("Mac") -> Platform.MacOS
		else -> Platform.Linux
	}
}