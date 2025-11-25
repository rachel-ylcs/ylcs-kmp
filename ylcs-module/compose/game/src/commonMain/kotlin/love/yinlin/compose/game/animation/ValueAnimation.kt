package love.yinlin.compose.game.animation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class ValueAnimation(
    progressStep: Float,
    private val isInfinite: Boolean = true
) {
    private var step by mutableStateOf(progressStep)

    var progress by mutableFloatStateOf(0f)
        private set

    fun update() {
        if (progress < 1f) progress += step
        else progress = if (isInfinite) 0f else 1f
    }

    fun reset(progressStep: Float? = null) {
        if (progressStep != null) step = progressStep
        progress = 0f
    }
}