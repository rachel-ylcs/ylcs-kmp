package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.common.Transform

@Stable
interface Body : Entity {
    var position: Offset
    var size: Size
    var transform: Transform?
    operator fun contains(point: Offset): Boolean
}