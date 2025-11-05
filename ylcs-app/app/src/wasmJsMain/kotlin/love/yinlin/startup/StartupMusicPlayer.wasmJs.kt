package love.yinlin.startup

import androidx.compose.runtime.Stable
import kotlinx.io.files.Path
import love.yinlin.Context
import love.yinlin.StartupFetcher
import love.yinlin.data.music.MusicInfo
import love.yinlin.data.music.MusicPlayMode

@StartupFetcher(index = 0, name = "rootPath", returnType = Path::class)
@Stable
actual fun buildMusicPlayer(): StartupMusicPlayer = object : StartupMusicPlayer() {
    override val isInit: Boolean = false
    override val error: Throwable? = null
    override val playMode: MusicPlayMode = MusicPlayMode.ORDER
    override val musicList: List<MusicInfo> = emptyList()
    override val isReady: Boolean = false
    override val isPlaying: Boolean = false
    override val currentDuration: Long = 0L
    override val currentPosition: Long = 0L
    override val currentMusic: MusicInfo? = null

    override suspend fun updatePlayMode(musicPlayMode: MusicPlayMode) { }
    override suspend fun play() { }
    override suspend fun pause() { }
    override suspend fun stop() { }
    override suspend fun gotoPrevious() { }
    override suspend fun gotoNext() { }
    override suspend fun gotoIndex(index: Int) { }
    override suspend fun seekTo(position: Long) { }
    override suspend fun prepareMedias(medias: List<MusicInfo>, startIndex: Int?, playing: Boolean) { }
    override suspend fun addMedias(medias: List<MusicInfo>) { }
    override suspend fun removeMedia(index: Int) { }
    override suspend fun initController(context: Context) { }
}