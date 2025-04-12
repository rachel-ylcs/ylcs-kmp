package love.yinlin.platform

import love.yinlin.data.music.MusicPlaylist

class ActualMusicFactory : MusicFactory() {
    override val isInit: Boolean get() = false

    override suspend fun init() {

    }

    override suspend fun start(playlist: MusicPlaylist, startId: String?) {

    }

    override suspend fun play() {

    }

    override suspend fun pause() {

    }

    override suspend fun stop() {

    }
}