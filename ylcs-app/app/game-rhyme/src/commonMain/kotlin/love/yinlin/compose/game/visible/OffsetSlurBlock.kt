package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.fastMap
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class OffsetSlurBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<OffsetSlurBlock.Status>(position, line, rawIndex, lineIndex) {
    @Stable
    data class Time(
        override val appearance: Long
    ) : BlockTime

    interface Status : BlockStatus {
        class Prepare : Status, BlockStatus.Prepare
        class Release : Status, BlockStatus.Release {
            override val duration: Int = 500
            override var progress: Float = 0f
            override var tick: Int = 0
        }
    }

    companion object {
        fun buildTime(difficulty: RhymeDifficulty, start: Long, end: Long): Time = Time(start)
    }

    private val scaleIndexs: List<Int> = rhymeAction.scale.fastMap { (it - 1) % 7 + 1 }
    private val scaleLevels: List<Int> = rhymeAction.scale.fastMap { (it - 1) / 7 }

    override val colorList: List<Color> = scaleIndexs.fastMap { ScaleColorList[it] }

    override fun prepareStatus(): Status = Status.Release()

    override fun onUpdate(tick: Int) {

    }

    override fun onInteract(interactStatus: Array<InteractStatus>, currentStatus: BlockStatus.Interact) {

    }

    override fun Drawer.onDraw() {

    }
}