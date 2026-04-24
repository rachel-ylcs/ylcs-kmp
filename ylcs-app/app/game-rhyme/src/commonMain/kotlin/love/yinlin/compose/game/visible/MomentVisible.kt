package love.yinlin.compose.game.visible

import androidx.compose.ui.util.fastCoerceIn
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.Visible

abstract class MomentVisible(private val duration: Int) : Visible(), Dynamic {
    private var currentTick: Int = 0
    private var isRemove: Boolean = false

    abstract fun onUpdateMoment(progress: Float)

    final override fun onUpdate(tick: Int) {
        if (currentTick < duration) {
            currentTick += tick
            onUpdateMoment((currentTick / duration.toFloat()).fastCoerceIn(0f, 1f))
            updateDirty()
        }
        else if (!isRemove) {
            isRemove = true
            layer?.removeAfterUpdate(this)
        }
    }
}