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
import love.yinlin.compose.game.Pointer
import love.yinlin.compose.game.traits.Container
import love.yinlin.compose.game.traits.Spirit
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.compose.game.traits.PointerTrigger
import love.yinlin.compose.game.traits.Transform
import love.yinlin.compose.game.traits.Visible
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
private class Record(
    private val rhymeManager: RhymeManager,
    private val recordImage: ImageBitmap
) : Spirit(rhymeManager), CircleBody, Visible, Dynamic, PointerTrigger {
    override val preTransform: List<Transform> = listOf(Transform.Translate(-28f, -44f))
    override val size: Size = Size(236f, 236f)

    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)
    private val anglePerTick = 360f / manager.fps / 18

    override fun onUpdate(tick: Long) {
        angle += anglePerTick
    }

    override fun onPointerEvent(pointer: Pointer): Boolean {
        val result = pointer.position in this
        if (result) rhymeManager.onPause()
        return result
    }

    override fun Drawer.onDraw() {
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
) : Spirit(rhymeManager), BoxBody, Visible, Dynamic {
    override val preTransform: List<Transform> = listOf(Transform.Translate(214f, 96f))
    override val size: Size = Size(355f, 10f)

    // 游戏进度
    private var progress: Float by mutableFloatStateOf(0f)
    private var isDurationUpdate: Boolean by mutableStateOf(false)
    private var duration: Long = 0L

    override fun onUpdate(tick: Long) {
        if (!isDurationUpdate) {
            val newDuration = rhymeManager.duration
            if (newDuration > 0L && newDuration != duration) {
                isDurationUpdate = true
                duration = newDuration
            }
        }
        progress = if (duration == 0L) 0f else (tick / duration.toFloat()).coerceIn(0f, 1f)
    }

    override fun Drawer.onDraw() {
        roundRect(Colors.Cyan2, position = Offset.Zero, size = Size(progress * 355, 10f), radius = 5f, alpha = 0.5f)
    }
}

@Stable
class LeftUI(
    rhymeManager: RhymeManager,
    recordImage: ImageBitmap
) : Container(rhymeManager), BoxBody {
    override val size: Size = Size(600f, 200f)

    override val spirits: List<Spirit> = listOf(
        Record(rhymeManager, recordImage),
        Progress(rhymeManager)
    )

    private val backgorund = manager.assets.image("left_ui")!!.image

    override fun Drawer.preDraw() {
        image(backgorund)
    }
}