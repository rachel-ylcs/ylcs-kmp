package love.yinlin.platform

class ActualAppContext : AppContext() {
    override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()
}

val appNative: ActualAppContext get() = app as ActualAppContext