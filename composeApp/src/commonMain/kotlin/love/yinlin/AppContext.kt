package love.yinlin

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import love.yinlin.common.Config
import love.yinlin.platform.KV

enum class Platform(name: String) {
	Android("Android"),
	IOS("IOS"),
	Windows("Windows"),
	Linux("Linux"),
	MacOS("MacOS"),
	WebWasm("Web/Wasm"),
}

val Platform.isPhone: Boolean get() = this == Platform.Android || this == Platform.IOS
val Platform.isDesktop: Boolean get() = this == Platform.Windows || this == Platform.Linux || this == Platform.MacOS
val Platform.isWeb: Boolean get() = this == Platform.WebWasm

// 平台
expect val platform: Platform

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
	val designWidth: Dp get() = if (isPortrait) 360.dp else 1200.dp
	// 设计高度
	val designHeight: Dp get() = 800.dp
	// KV
	abstract val kv: KV
	// Config
	val config by lazy { Config(kv) }
	// HttpClient
	abstract val client: HttpClient
	abstract val fileClient: HttpClient

	fun initialize(): AppContext {
		return this
	}
}

var appContext: AppContext? = null
val app: AppContext get() = appContext!!