package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
abstract class FrameAnimation(
    totalFrame: Int,
    private val isInfinite: Boolean
) {
    companion object {
        private const val END = -1
    }

    protected var total by mutableIntStateOf(totalFrame)

    var frame by mutableIntStateOf(END)
        protected set

    val isCompleted by derivedStateOf { frame == END }

    abstract val progress: Float

    fun update(): Boolean {
        if (frame != END) {
            if (frame >= total - 1) {
                frame = if (isInfinite) 0 else END
                return false
            }
            else frame++
        }
        return true
    }

    fun start(totalFrame: Int? = null) {
        if (totalFrame != null) total = totalFrame
        frame = 0
    }
}