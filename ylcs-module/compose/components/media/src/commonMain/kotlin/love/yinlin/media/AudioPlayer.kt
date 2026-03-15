package love.yinlin.media

import androidx.compose.runtime.Stable
import love.yinlin.foundation.Context
import love.yinlin.fs.File

@Stable
abstract class AudioPlayer(val context: Context, val onEndListener: () -> Unit) {
    /**
     * 是否初始化
     */
    abstract val isInit: Boolean

    /**
     * 是否播放
     */
    abstract val isPlaying: Boolean

    /**
     * 当前位置
     */
    abstract val position: Long

    /**
     * 时长
     */
    abstract val duration: Long

    /**
     * 初始化
     */
    abstract suspend fun init()

    /**
     * 释放
     */
    abstract fun release()

    /**
     * 加载
     */
    abstract suspend fun load(path: File, playing: Boolean)

    /**
     * 播放
     */
    abstract fun play()

    /**
     * 暂停
     */
    abstract fun pause()

    /**
     * 停止
     */
    abstract fun stop()

    /**
     * 调整进度
     */
    abstract fun seekTo(position: Long)
}

expect fun buildAudioPlayer(context: Context, onEndListener: () -> Unit): AudioPlayer