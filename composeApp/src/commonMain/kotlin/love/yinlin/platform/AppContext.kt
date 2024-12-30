package love.yinlin.platform

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

abstract class AppContext {
	// 屏幕宽度
	abstract val screenWidth: Int
	// 屏幕高度
	abstract val screenHeight: Int
	// 字体缩放
	abstract val fontScale: Float
	// 是否竖屏
	val isPortrait: Boolean get() = screenWidth <= screenHeight
	// 设计宽度
	val designWidth: Dp get() = if (isPortrait) 360.dp else 800.dp
	// 设计高度
	val designHeight: Dp get() = if (isPortrait) 800.dp else 450.dp
}

enum class Platform(name: String) {
	Android("Android"),
	IOS("IOS"),
	Windows("Windows"),
	Linux("Linux"),
	MacOS("MacOS"),
	WebWasm("Web/Wasm"),
}

val Platform.isDesktop: Boolean get() = this == Platform.Windows || this == Platform.Linux || this == Platform.MacOS

expect val platform: Platform