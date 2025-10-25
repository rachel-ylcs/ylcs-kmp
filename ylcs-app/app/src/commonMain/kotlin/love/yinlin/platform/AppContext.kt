package love.yinlin.platform

import androidx.compose.runtime.Stable
import com.github.panpf.sketch.SingletonSketch
import com.github.panpf.sketch.Sketch
import love.yinlin.common.KVConfig

@Stable
abstract class AppContext {
	// KV
	abstract val kv: KV
	lateinit var config: KVConfig
	lateinit var musicFactory: MusicFactory

	// ImageLoader
	abstract fun initializeSketch(): Sketch

	// MusicFactory
	abstract fun initializeMusicFactory(): MusicFactory

	open fun initialize() {
		// 初始化配置
		config = KVConfig(kv)
		// 初始化图片加载器
		SingletonSketch.setSafe { initializeSketch() }
		// 初始化音乐播放器
		musicFactory = initializeMusicFactory()
		musicFactory.initFactory()
	}
}

lateinit var app: AppContext
	internal set