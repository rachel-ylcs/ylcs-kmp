package love.yinlin.platform

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ActualAppContext : AppContext() {
    var windowVisible by mutableStateOf(true)

	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()

	override fun initialize() {
		super.initialize()
		// 创建悬浮歌词
		appNative.musicFactory.floatingLyrics = ActualFloatingLyrics().apply { isAttached = true }
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext