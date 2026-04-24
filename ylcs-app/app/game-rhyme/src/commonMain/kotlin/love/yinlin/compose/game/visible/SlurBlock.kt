package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import love.yinlin.compose.game.common.BlockLine
import love.yinlin.compose.game.common.BlockStatus
import love.yinlin.data.music.RhymeAction

@Stable
abstract class SlurBlock<BS : BlockStatus>(
    position: Offset,
    line: BlockLine,
    rawIndex: Int,
    lineIndex: Int,
    override val rhymeAction: RhymeAction.Slur,
) : Block<BS>(position, line, rawIndex, lineIndex) {
    companion object {

    }


}