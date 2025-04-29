package love.yinlin.platform

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import io.ktor.client.*
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.KVConfig
import love.yinlin.common.Resource
import love.yinlin.common.ThemeMode

@Stable
data class PhysicalGraphics(val width: Int, val height: Int, val density: Density)

@Stable
abstract class AppContext(val physics: PhysicalGraphics) {
	companion object {
		const val CRASH_KEY = "crash_key"
	}

	abstract fun densityWrapper(newWidth: Dp, newHeight: Dp, oldDensity: Density): Density

	// 主题

	val isDarkMode: Boolean @Composable get() = when (config.themeMode) {
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

	// 初始化部分

	private fun initializePath() {
		OS.ifNotPlatform(Platform.WebWasm) {
			SystemFileSystem.createDirectories(OS.Storage.dataPath)
			SystemFileSystem.createDirectories(OS.Storage.cachePath)

			SystemFileSystem.createDirectories(OS.Storage.musicPath)
		}
	}

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
		Resource.initialize()
		// 初始化图片加载器
		SingletonSketch.setSafe { initializeSketch() }
		// 初始化音乐播放器
		musicFactory = initializeMusicFactory()
		musicFactory.initFactory()
	}
}

lateinit var app: AppContext
	internal set