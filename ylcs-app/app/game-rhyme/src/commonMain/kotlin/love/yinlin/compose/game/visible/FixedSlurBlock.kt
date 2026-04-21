package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.common.InteractStatus
import love.yinlin.compose.game.data.RhymeDifficulty
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class FixedSlurBlock(
    position: Offset,
    line: BlockLine,
    override val time: Time,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<FixedSlurBlock.Status>(position, line, rawIndex, lineIndex) {
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

    private val scaleIndex: Int = (rhymeAction.scale.first() - 1) % 7 + 1
    private val scaleLevel: Int = (rhymeAction.scale.first() - 1) / 7
    private val mainColor: Color = ScaleColorList[scaleIndex]

    override val colorList: List<Color> = listOf(mainColor)

    override fun prepareStatus(): Status = Status.Release()

    override fun onUpdate(tick: Int) {

    }

    override fun onInteract(interactStatus: Array<InteractStatus>, currentStatus: BlockStatus.Interact) {

    }

    override fun Drawer.onDraw() {

    }
}