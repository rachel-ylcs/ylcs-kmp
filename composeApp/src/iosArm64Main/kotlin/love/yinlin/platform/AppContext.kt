package love.yinlin.platform

import androidx.compose.runtime.Stable
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.UIKit.UIScreen

@OptIn(ExperimentalForeignApi::class)
class AppContext : IAppContext() {
	private val screenBounds = UIScreen.mainScreen.bounds

	override val screenWidth: Int = CGRectGetWidth(screenBounds).toInt()
	override val screenHeight: Int = CGRectGetHeight(screenBounds).toInt()
	override val fontScale: Float = 1f
	override val kv: KV = KV()
}

@Stable
val appNative: AppContext get() = app as AppContext