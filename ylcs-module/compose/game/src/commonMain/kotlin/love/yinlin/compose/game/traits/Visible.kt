package love.yinlin.compose.game.traits

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import love.yinlin.compose.game.common.Drawer

@Stable
abstract class Visible(
    position: Offset = Offset.Zero,
    size: Size = Size.Zero,
    /**
     * 层级
     */
    val zIndex: Int = 0,
    visible: Boolean = true
) : Body(position, size) {
    /**
     * 参与视口剔除
     */
    open val culling: Boolean = true

    /**
     * 可见
     */
    var visible by mutableStateOf(visible)

    /**
     * 绘制
     */
    abstract fun Drawer.onDraw(viewportSize: Size)
}