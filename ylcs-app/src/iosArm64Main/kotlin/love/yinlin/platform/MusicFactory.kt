package love.yinlin.platform

class ActualMusicFactory : MusicFactory() {
    override val isInit: Boolean get() = false

    override suspend fun init() {

    }
}