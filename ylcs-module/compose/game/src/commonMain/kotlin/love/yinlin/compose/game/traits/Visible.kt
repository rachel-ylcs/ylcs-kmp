package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.Engine
import love.yinlin.compose.game.common.Drawer
import love.yinlin.compose.game.common.Transform

@Stable
abstract class Visible(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
    zIndex: Int = 0,
    visible: Boolean = true
) : Body {
    override val id: String? = null

    override fun onAttached(engine: Engine) { }
    override fun onDetached(engine: Engine) { }

    override var position: Offset by mutableStateOf(position)
    override var size: Size by mutableStateOf(size)
    override var transform: Transform? by mutableStateOf(null)

    var zIndex by mutableIntStateOf(zIndex)
    var visible by mutableStateOf(visible)

    abstract fun Drawer.onDraw()
}