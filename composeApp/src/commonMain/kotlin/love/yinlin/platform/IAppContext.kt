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
import love.yinlin.ThemeMode
import love.yinlin.common.KVConfig

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
	// HttpClient
	val client: HttpClient = NetClient.common
	val fileClient: HttpClient = NetClient.file

	fun initialize(): IAppContext {
		config = KVConfig(kv)
		return this
	}
}

@Stable
lateinit var app: IAppContext

lateinit var config: KVConfig