package love.yinlin.media

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import love.yinlin.compose.data.media.MediaPlayMode
import love.yinlin.foundation.Context

@Stable
abstract class MusicPlayer(protected val fetcher: MediaMetadataFetcher) {
    /**
     * 是否初始化
     */
    var isInit: Boolean by mutableStateOf(false)
        protected set

    /**
     * 当前错误
     */
    var error: Throwable? by mutableStateOf(null)
        protected set

    /**
     * 播放模式
     */
    var playMode: MediaPlayMode by mutableStateOf(MediaPlayMode.Default)
        protected set

    /**
     * 播放列表
     */
    val musicList = mutableStateListOf<String>()

    /**
     * 是否就绪
     */
    val isReady by derivedStateOf { musicList.isNotEmpty() }

    /**
     * 是否播放
     */
    var isPlaying by mutableStateOf(false)
        protected set

    /**
     * 时长
     */
    var duration by mutableLongStateOf(0L)
        protected set

    /**
     * 进度
     */
    var position by mutableLongStateOf(0L)
        protected set

    /**
     * 当前音乐ID
     */
    var currentId: String? by mutableStateOf(null)
        protected set

    /**
     * 回调
     */
    var listener: MusicPlayerListener? = null

    /**
     * 初始化
     */
    abstract suspend fun init(context: Context)

    /**
     * 释放
     */
    abstract fun release()

    /**
     * 切换模式
     */
    abstract suspend fun updatePlayMode(mode: MediaPlayMode)

    /**
     * 播放
     */
    abstract suspend fun play()

    /**
     * 暂停
     */
    abstract suspend fun pause()

    /**
     * 停止
     */
    abstract suspend fun stop()

    /**
     * 上一首
     */
    abstract suspend fun gotoPrevious()

    /**
     * 下一首
     */
    abstract suspend fun gotoNext()

    /**
     * 切换
     */
    abstract suspend fun gotoIndex(index: Int)

    /**
     * 调整进度
     */
    abstract suspend fun seekTo(position: Long)

    /**
     * 加载媒体
     */
    abstract suspend fun prepareMedias(medias: List<String>, startIndex: Int?, playing: Boolean)

    /**
     * 添加媒体
     */
    abstract suspend fun addMedias(medias: List<String>)

    /**
     * 移除媒体
     */
    abstract suspend fun removeMedia(index: Int)
}

expect fun buildMusicPlayer(fetcher: MediaMetadataFetcher): MusicPlayer