package love.yinlin.compose.game.visible

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.traits.Visible
import love.yinlin.data.music.RhymeAction

@Stable
sealed class Block(
    position: Offset,
    size: Size,
    val timeAppearance: Long,
    val timeStart: Long,
    val timeEnd: Long
) : Visible(position, size) {
    abstract val rhymeAction: RhymeAction
}