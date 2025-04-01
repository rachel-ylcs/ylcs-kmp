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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import love.yinlin.common.ThemeMode
import love.yinlin.common.KVConfig
import love.yinlin.common.Resource

@Stable
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

	// 主题
	var theme by mutableStateOf(ThemeMode.SYSTEM)

	val isDarkMode: Boolean @Composable get() = when (theme) {
		ThemeMode.SYSTEM -> isSystemInDarkTheme()
		ThemeMode.LIGHT -> false
		ThemeMode.DARK -> true
	}

	// KV
	abstract val kv: KV
	lateinit var config: KVConfig

	// HttpClient
	val client: HttpClient = NetClient.common
	val fileClient: HttpClient = NetClient.file

	open fun initialize(): AppContext {
		// 加载配置
		config = KVConfig(kv)
		CoroutineScope(Dispatchers.Default).launch {
			// 加载资源
			Resource.initialize()
		}
		return this
	}

	companion object {
		const val CRASH_KEY = "crash_key"
	}
}

lateinit var app: AppContext