package love.yinlin

import androidx.compose.ui.unit.Density
import io.ktor.client.HttpClient
import love.yinlin.platform.DesktopClient
import love.yinlin.platform.KV
import java.awt.GraphicsEnvironment

const val isPhoneDebug: Boolean = false

actual val platform: Platform = System.getProperty("os.name").let {
	when {
		it.startsWith("Windows") -> Platform.Windows
		it.startsWith("Mac") -> Platform.MacOS
		else -> Platform.Linux
	}
}

class DesktopContext : AppContext() {
	companion object {
		const val SCREEN_PERCENT = 0.85f
	}

	var rawDensity: Density? = null
	val windowWidth: Float
	val windowHeight: Float
	override val screenWidth: Int
	override val screenHeight: Int
	override val fontScale: Float = 1f
	override val kv: KV = KV()
	override val client: HttpClient = DesktopClient.common
	override val fileClient: HttpClient = DesktopClient.file

	init {
		val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
		val bounds = ge.maximumWindowBounds
		val transform = ge.defaultScreenDevice.defaultConfiguration.defaultTransform
		val scaleX = transform.scaleX.toFloat()
		val scaleY = transform.scaleY.toFloat()
		windowWidth = bounds.width * SCREEN_PERCENT / (if (isPhoneDebug) 4f else 1f)
		windowHeight = bounds.height * SCREEN_PERCENT
		screenWidth = (windowWidth * scaleX).toInt()
		screenHeight = (windowHeight * scaleY).toInt()
	}
}