package love.yinlin.platform

class ActualAppContext : AppContext() {
	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()

	override fun initialize() {
		super.initialize()
		// 创建悬浮歌词
		appNative.musicFactory.floatingLyrics = ActualFloatingLyrics().apply { isAttached = true }
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext