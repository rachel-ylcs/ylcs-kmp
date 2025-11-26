package love.yinlin.screen.world.single.rhyme.spirit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import love.yinlin.compose.Colors
import love.yinlin.compose.game.traits.BoxBody
import love.yinlin.compose.game.traits.CircleBody
import love.yinlin.compose.game.Drawer
import love.yinlin.compose.game.traits.Container
import love.yinlin.compose.game.traits.Event
import love.yinlin.compose.game.traits.PointerEvent
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Soul
import love.yinlin.compose.game.traits.Transform
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
private class Record(
    private val rhymeManager: RhymeManager,
    private val recordImage: ImageBitmap
) : Spirit(rhymeManager), CircleBody {
    override val preTransform: List<Transform> = listOf(Transform.Translate(-28f, -44f))
    override val size: Size = Size(236f, 236f)

    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)
    private val anglePerTick = 360f / manager.fps / 18

    override fun onClientUpdate(tick: Long) {
        var newAngle = angle + anglePerTick
        if (newAngle >= 360f) newAngle -= 360f
        angle = newAngle
    }

    override fun onClientEvent(event: Event): Boolean {
        return when (event) {
            is PointerEvent -> {
                val pointer = event.pointer
                val result = pointer.isUp && pointer.position in this
                if (result) rhymeManager.onPause()
                result
            }
        }
    }

    override fun Drawer.onClientDraw() {
        rotate(angle) {
            // 画封面
            circleImage(recordImage)
            // 画挖孔
            circle(Colors.Black, center, 15f)
            circle(Colors.Gray4, center, 15f, style = Stroke(width = 2f))
        }
    }
}

@Stable
private class Progress(
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

@Stable
class LeftUI(
    rhymeManager: RhymeManager,
    recordImage: ImageBitmap
) : Container(rhymeManager), BoxBody {
    override val size: Size = Size(700f, 200f)

    override val souls: List<Soul> = listOf(
        Record(rhymeManager, recordImage),
        Progress(rhymeManager)
    )

    private val backgorund = manager.assets.image("left_ui")!!.image

    override fun Drawer.onClientPreDraw() {
        image(backgorund)
    }
}