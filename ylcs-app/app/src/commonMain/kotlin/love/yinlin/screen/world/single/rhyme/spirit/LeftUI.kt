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
import love.yinlin.compose.game.Spirit
import love.yinlin.compose.game.traits.Dynamic
import love.yinlin.screen.world.single.rhyme.RhymeManager

@Stable
private class Record(
    rhymeManager: RhymeManager,
    private val recordImage: ImageBitmap
) : Spirit(rhymeManager), CircleBody, Dynamic {
    override val preOffset: Offset = Offset(-28f, -44f)
    override val size: Size = Size(236f, 236f)

    // 封面旋转角
    private var angle: Float by mutableFloatStateOf(0f)
    private val anglePerTick = 360f / manager.fps / 18

    override fun onUpdate(tick: Long) {
        angle += anglePerTick
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
) : Spirit(rhymeManager), BoxBody, Dynamic {
    override val preOffset: Offset = Offset(214f, 96f)
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
        roundRect(Colors.Green4, position = Offset.Zero, size = Size(progress * 355, 10f), radius = 5f, alpha = 0.5f)
    }
}

@Stable
class LeftUI(
    rhymeManager: RhymeManager,
    recordImage: ImageBitmap
) : Spirit(rhymeManager), BoxBody, Dynamic {
    override val size: Size = Size(600f, 200f)

    private val backgorund = manager.assets.image("left_ui")!!.image
    private val record = Record(rhymeManager, recordImage)
    private val progress = Progress(rhymeManager)

    override fun onUpdate(tick: Long) {
        record.onUpdate(tick)
        progress.onUpdate(tick)
    }

    override fun Drawer.onDraw() {
        // 画背景
        image(backgorund)
        // 画封面
        record.apply { draw() }
        // 画进度
        progress.apply { draw() }
    }
}