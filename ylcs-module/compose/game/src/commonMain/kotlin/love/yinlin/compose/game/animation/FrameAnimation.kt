package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

@Stable
class FrameAnimation(
    totalFrame: Int,
    private val isInfinite: Boolean = false
) {
    companion object {
        private const val END = -1
    }

    private var total by mutableIntStateOf(totalFrame)

    var frame by mutableIntStateOf(END)
        private set

    val progress by derivedStateOf { (frame + 1f) / total }

    val isCompleted by derivedStateOf { frame == END }

    fun update() {
        if (frame != END) {
            if (frame >= total - 1) frame = if (isInfinite) 0 else END
            else frame++
        }
    }

    fun start(totalFrame: Int? = null) {
        if (totalFrame != null) total = totalFrame
        frame = 0
    }

    fun reset() {
        frame = 0
    }
}