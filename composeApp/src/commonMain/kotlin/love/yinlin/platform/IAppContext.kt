package love.yinlin.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import love.yinlin.common.Config

@Stable
enum class Platform(name: String) {
	Android("Android"),
	IOS("IOS"),
	Windows("Windows"),
	Linux("Linux"),
	MacOS("MacOS"),
	WebWasm("Web/Wasm"),
}

@Stable
val Platform.isPhone: Boolean get() = this == Platform.Android || this == Platform.IOS
@Stable
val Platform.isDesktop: Boolean get() = this == Platform.Windows || this == Platform.Linux || this == Platform.MacOS
@Stable
val Platform.isWeb: Boolean get() = this == Platform.WebWasm

enum class ThemeMode {
	SYSTEM, LIGHT, DARK
}

val ThemeMode.next: ThemeMode get() = when (this) {
	ThemeMode.SYSTEM -> ThemeMode.LIGHT
	ThemeMode.LIGHT -> ThemeMode.DARK
	ThemeMode.DARK -> ThemeMode.SYSTEM
}

@Stable
abstract class IAppContext {
	// 屏幕宽度
	@Stable
	abstract val screenWidth: Int
	// 屏幕高度
	@Stable
	abstract val screenHeight: Int
	// 字体缩放
	@Stable
	abstract val fontScale: Float
	// 是否竖屏
	@Stable
	val isPortrait: Boolean get() = screenWidth <= screenHeight
	// 设计宽度
	@Stable
	val designWidth: Dp get() = if (isPortrait) 360.dp else 1200.dp
	// 设计高度
	@Stable
	val designHeight: Dp get() = 800.dp

	// 主题
	var theme by mutableStateOf(ThemeMode.SYSTEM)

	val isDarkMode: Boolean @Composable get() = when (theme) {
		ThemeMode.SYSTEM -> isSystemInDarkTheme()
		ThemeMode.LIGHT -> false
		ThemeMode.DARK -> true
	}

	// KV
	abstract val kv: KV
	// Config
	val config by lazy { Config(kv) }
	// HttpClient
	val client: HttpClient = NetClient.common
	val fileClient: HttpClient = NetClient.file

	fun initialize(): IAppContext {
		return this
	}
}

@Stable
var appContext: IAppContext? = null
@Stable
val app: IAppContext get() = appContext!!