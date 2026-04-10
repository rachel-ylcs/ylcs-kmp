package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction
import kotlin.math.abs

@Stable
class NoteBlock(
    position: Offset,
    line: BlockLine,
    time: BlockTime,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Note,
) : Block(position, line, time, rawIndex, lineIndex) {
    override fun onUpdate(tick: Int) {
        withMapLayer { _, audioTick ->
            when (val currentStatus = status) {
                is BlockStatus.None -> false // 未出现不处理
                is BlockStatus.Prepare -> {
                    if (currentStatus.tick >= time.missStart) status = BlockStatus.Interact(0)
                    else currentStatus.tick = audioTick
                    true
                }
                is BlockStatus.Interact -> {
                    when {
                        currentStatus.tick >= time.missEnd -> status = BlockStatus.End(0)
                        currentStatus.tick >= time.badStart -> status = BlockStatus.Score(0)
                        else -> currentStatus.tick = audioTick
                    }
                    true
                }
                is BlockStatus.Score -> {
                    if (currentStatus.tick >= time.badEnd) status = BlockStatus.Interact(0)
                    else currentStatus.tick = audioTick
                    true
                }
                is BlockStatus.End -> {
                    currentStatus.tick += audioTick
                    false
                }
            }
        }
    }

    override fun Drawer.onDraw() {
        when (val currentStatus = status) {
            is BlockStatus.None -> return
            is BlockStatus.Prepare -> {
                val progress = (currentStatus.tick / time.missStart.toFloat()).coerceIn(0f, 1f)
                withBlockScale { drawCommonPrepare(progress) }
            }
            is BlockStatus.Interact -> {
                val standard = time.standard
                val progress = (abs(currentStatus.tick - standard) / (standard - time.missStart).toFloat()).coerceIn(0f, 1f)
                withBlockScale {
                    rect(InteractColor, Offset.Zero, size, alpha = progress)
                    drawCommonPrepare(1f)
                }
            }
            is BlockStatus.Score -> {

            }
            is BlockStatus.End -> {
                withBlockScale {
                    drawCommonPrepare(1f)
                }
            }
        }
    }
}