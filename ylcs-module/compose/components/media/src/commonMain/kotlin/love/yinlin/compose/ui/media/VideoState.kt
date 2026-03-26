package love.yinlin.compose.ui.media

import androidx.compose.runtime.*

@Stable
abstract class VideoState {
    var url: String? by mutableStateOf(null)
        protected set

    var isPlaying by mutableStateOf(false)
        protected set

    var position: Long by mutableLongStateOf(0L)
        protected set

    var duration by mutableLongStateOf(0L)
        protected set

    var error: Throwable? by mutableStateOf(null)
        protected set

    abstract fun load(path: String)
    abstract fun play()
    abstract fun pause()
    abstract fun stop()
    abstract fun seek(position: Long)
}