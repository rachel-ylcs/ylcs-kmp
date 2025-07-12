package love.yinlin.platform

import androidx.compose.runtime.Stable
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import io.ktor.client.HttpClient
import kotlinx.io.files.SystemFileSystem
import love.yinlin.common.KVConfig
import love.yinlin.common.Resource

@Stable
abstract class AppContext {
	companion object {
		const val CRASH_KEY = "crash_key"
	}

	// KV
	abstract val kv: KV
	lateinit var config: KVConfig
	lateinit var musicFactory: MusicFactory

	// HttpClient
	val client: HttpClient by lazy { NetClient.common }
	val fileClient: HttpClient by lazy { NetClient.file }
	val socketsClient: HttpClient by lazy { NetClient.sockets }

	// 初始化部分

	private fun initializePath() {
		OS.ifNotPlatform(WebWasm) {
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