package love.yinlin.platform

import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode

class ActualMusicFactory : MusicFactory() {
    override val isInit: Boolean get() = false
    override suspend fun init() {}
    override val error: Throwable? = null
    override val playMode: MusicPlayMode = MusicPlayMode.ORDER
    override val musicList: List<MusicInfo> = emptyList()
    override val isReady: Boolean = false
    override val isPlaying: Boolean = false
    override val currentPosition: Long = 0L
    override val currentDuration: Long = 0L
    override val currentMusic: MusicInfo? = null
    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) {}
    override suspend fun play() {}
    override suspend fun pause() {}
    override suspend fun stop() {}
    override suspend fun gotoPrevious() {}
    override suspend fun gotoNext() {}
    override suspend fun gotoIndex(index: Int) {}
    override suspend fun seekTo(position: Long) {}
    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?) {}
    override suspend fun addMedias(medias: List<MusicInfo>) {}
    override suspend fun removeMedia(index: Int) {}
}