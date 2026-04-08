package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class FixedSlurBlock(
    position: Offset,
    size: Size,
    timeAppearance: Long,
    timeStart: Long,
    timeEnd: Long,
    override val rhymeAction: RhymeAction.Slur,
) : Block(position, size, timeAppearance, timeStart, timeEnd) {
    override fun Drawer.onDraw() {

    }
}