package love.yinlin.platform

import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode

class ActualMusicFactory : MusicFactory() {
    override val isInit: Boolean get() = false

    override suspend fun init() {

    }

    override val error: Throwable? = null
    override val playMode: MusicPlayMode get() = MusicPlayMode.ORDER
    override val isPlaying: Boolean = false
    override val currentPosition: Long = 0L
    override val currentDuration: Long = 0L
    override val musicList: List<MusicInfo> = emptyList()

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {
        TODO("Not yet implemented")
    }

    override suspend fun play() {
        TODO("Not yet implemented")
    }

    override suspend fun pause() {
        TODO("Not yet implemented")
    }

    override suspend fun stop() {
        TODO("Not yet implemented")
    }

    override suspend fun gotoPrevious() {
        TODO("Not yet implemented")
    }

    override suspend fun gotoNext() {
        TODO("Not yet implemented")
    }

    override suspend fun seekTo(position: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun prepareMedias(
        medias: List<MusicInfo>,
        startIndex: Int?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun addMedia(media: MusicInfo) {
        TODO("Not yet implemented")
    }

    override suspend fun removeMedia(index: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun moveMedia(start: Int, end: Int) {
        TODO("Not yet implemented")
    }
}