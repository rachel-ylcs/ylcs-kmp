package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
abstract class FrameAnimation(
    totalFrame: Int,
    private val isInfinite: Boolean,
) {
    companion object {
        private const val END = -1
    }

    protected abstract fun calcProgress(t: Int, f: Int): Float

    private var step = 0

    var total by mutableIntStateOf(totalFrame)
        private set

    var frame by mutableIntStateOf(END)
        private set

    val isCompleted by derivedStateOf { frame == END }

    val progress: Float by derivedStateOf {
        val currentFrame = frame
        if (currentFrame == END) 0f else calcProgress(total, currentFrame).coerceIn(0f, 1f)
    }

    inline fun withProgress(block: (Float) -> Unit) {
        val currentProgress = progress
        if (currentProgress > 0f) block(currentProgress)
    }

    inline fun withProgress(transform: (Boolean, Float) -> Float, block: (Float) -> Unit) {
        block(transform(isCompleted, progress))
    }

    fun update(): Boolean {
        val currentFrame = frame
        if (currentFrame != END) {
            if (currentFrame >= total - 1) {
                frame = if (isInfinite) 0 else END
                return false
            }
            else ++frame
        }
        return true
    }

    fun start(totalFrame: Int? = null) {
        if (totalFrame != null) total = totalFrame
        step = 0
        frame = 0
    }

    fun reset(totalFrame: Int? = null) {
        if (totalFrame != null) total = totalFrame
        step = 0
        frame = END
    }
}