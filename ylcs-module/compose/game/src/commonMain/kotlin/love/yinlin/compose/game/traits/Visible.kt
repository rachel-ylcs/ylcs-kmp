package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.common.Drawer

@Stable
abstract class Visible(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
    zIndex: Int = 0,
    visible: Boolean = true
) : Body(position, size) {
    var zIndex by mutableIntStateOf(zIndex)
    var visible by mutableStateOf(visible)

    abstract fun Drawer.onDraw()
}