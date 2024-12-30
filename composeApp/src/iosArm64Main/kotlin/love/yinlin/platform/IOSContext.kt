package love.yinlin.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
class IOSContext : AppContext() {
	private val screenBounds = UIScreen.mainScreen.bounds

	override val screenWidth: Int = CGRectGetWidth(screenBounds).toInt()
	override val screenHeight: Int = CGRectGetHeight(screenBounds).toInt()
	override val fontScale: Float = 1f
}

actual val platform: Platform = Platform.IOS