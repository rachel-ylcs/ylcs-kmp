package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.Colors
import love.yinlin.compose.game.drawer.Drawer
import love.yinlin.data.music.RhymeAction

@Stable
class NoteBlock(
    position: Offset,
    size: Size,
    timeAppearance: Long,
    timeStart: Long,
    timeEnd: Long,
    override val rhymeAction: RhymeAction.Note,
) : Block(position, size, timeAppearance, timeStart, timeEnd) {
    override fun Drawer.onDraw() {
        rect(Colors.Green4, Offset.Zero, size)
    }
}