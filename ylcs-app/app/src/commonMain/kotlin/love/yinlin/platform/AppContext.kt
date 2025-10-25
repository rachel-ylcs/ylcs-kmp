package love.yinlin.platform

import androidx.compose.runtime.Stable
import love.yinlin.common.KVConfig
import love.yinlin.service

@Stable
abstract class AppContext {
	// KV
	lateinit var config: KVConfig
	lateinit var musicFactory: MusicFactory

	// MusicFactory
	abstract fun initializeMusicFactory(): MusicFactory

	open fun initialize() {
		// 初始化配置
		config = KVConfig(service.kv)
		// 初始化音乐播放器
		musicFactory = initializeMusicFactory()
		musicFactory.initFactory()
	}
}

lateinit var app: AppContext
	internal set