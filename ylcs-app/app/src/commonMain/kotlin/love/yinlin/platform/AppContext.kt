package love.yinlin.platform

import androidx.compose.runtime.Stable

@Stable
abstract class AppContext {
	lateinit var musicFactory: MusicFactory

	// MusicFactory
	abstract fun initializeMusicFactory(): MusicFactory

	open fun initialize() {
		// 初始化音乐播放器
		musicFactory = initializeMusicFactory()
		musicFactory.initFactory()
	}
}

lateinit var app: AppContext
	internal set