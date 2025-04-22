package love.yinlin.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import io.ktor.client.*
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.ThemeMode
import love.yinlin.common.KVConfig
import love.yinlin.common.Resource

@Stable
abstract class AppContext {
	companion object {
		const val CRASH_KEY = "crash_key"
	}

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
	lateinit var musicFactory: MusicFactory

	// HttpClient
	val client: HttpClient = NetClient.common
	val fileClient: HttpClient = NetClient.file

	// ImageLoader
	abstract fun initializeSketch(): Sketch

	// MusicFactory
	abstract fun initializeMusicFactory(): MusicFactory

	open fun initialize() {
		// 初始化目录
		initializePath()
		// 初始化配置
		config = KVConfig(kv)
		// 初始化资源
		Coroutines.startIO { Resource.initialize() }
		// 初始化图片加载器
		SingletonSketch.setSafe { initializeSketch() }
		// 初始化音乐播放器
		musicFactory = initializeMusicFactory()
		Coroutines.startCPU {
			if (!musicFactory.isInit) {
				musicFactory.initLibrary()
				musicFactory.init()
			}
		}
	}

	private fun initializePath() {
		OS.ifNotPlatform(Platform.WebWasm) {
			SystemFileSystem.createDirectories(OS.Storage.dataPath)
			SystemFileSystem.createDirectories(OS.Storage.cachePath)

			SystemFileSystem.createDirectories(OS.Storage.musicPath)
		}
	}
}

lateinit var app: AppContext