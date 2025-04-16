package love.yinlin.platform

import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode

class ActualMusicFactory : MusicFactory() {
    override var isInit: Boolean = false

    override suspend fun init() {
        isInit = true
    }

    override val error: Throwable? = null
    override val playMode: MusicPlayMode get() = MusicPlayMode.ORDER
    override val musicList: List<MusicInfo> = emptyList()
    override val isReady: Boolean = false
    override val isPlaying: Boolean = false
    override val currentPosition: Long = 0L
    override val currentDuration: Long = 0L

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {

    }

    override suspend fun play() {

    }

    override suspend fun pause() {

    }

    override suspend fun stop() {

    }

    override suspend fun gotoPrevious() {

    }

    override suspend fun gotoNext() {

    }

    override suspend fun seekTo(position: Long) {

    }

    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?) {

    }

    override suspend fun addMedia(media: MusicInfo) {

    }

    override suspend fun removeMedia(index: Int) {

    }

    override suspend fun moveMedia(start: Int, end: Int) {

    }
}