package love.yinlin.platform

class ActualAppContext : AppContext() {
    override val kv: KV = KV()

    override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory()
}

val appNative: ActualAppContext get() = app as ActualAppContext