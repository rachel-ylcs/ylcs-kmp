package love.yinlin.common.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.Colors
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Transform
import love.yinlin.common.rhyme.RhymeManager

@Stable
class ProgressBar(
    private val rhymeManager: RhymeManager,
) : Spirit(rhymeManager), BoxBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(214f, 96f))
    override val size: Size = Size(455f, 10f)

    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)
    private var isDurationUpdate: Boolean by mutableStateOf(false)
    private var duration: Long = 0L

    override fun onClientUpdate(tick: Long) {
        if (!isDurationUpdate) {
            val newDuration = rhymeManager.duration
            if (newDuration > 0L && newDuration != duration) {
                isDurationUpdate = true
                duration = newDuration
            }
        }
        progress = if (duration == 0L) 0f else (tick / duration.toFloat()).coerceIn(0f, 1f)
    }

    override fun Drawer.onClientDraw() {
        roundRect(Colors.Cyan2, position = Offset.Zero, size = Size(size.width * progress, size.height), radius = 5f)
    }
}