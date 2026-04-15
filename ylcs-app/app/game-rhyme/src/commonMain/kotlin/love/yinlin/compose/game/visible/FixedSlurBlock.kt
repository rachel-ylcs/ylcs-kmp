package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockTime
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class FixedSlurBlock(
    position: Offset,
    line: BlockLine,
    time: BlockTime,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block(position, line, time, rawIndex, lineIndex) {
    override fun onUpdate(tick: Int) {

    }

    override fun Drawer.onDraw() {

    }
}