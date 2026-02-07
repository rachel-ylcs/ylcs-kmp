package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.icon.Icons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
class WindowController(
    placement: WindowPlacement,
    isMinimized: Boolean,
    position: WindowPosition,
    initSize: DpSize,
    initTitle: String,
    initIcon: DrawableResource?,
    initRoundedCorner: Boolean,
    initActionAlwaysOnTop: Boolean,
    initActionMinimize: Boolean,
    initActionMaximize: Boolean,
    initActionClose: Boolean,
) {
    internal val rawState = WindowState(placement, isMinimized, position, initSize)

    private var maximizeState: MaximizeState by mutableRefStateOf(MaximizeState.Normal)

    /**
     * 窗口是否最大化
     */
    val maximize: Boolean get() = maximizeState !is MaximizeState.Normal

    /**
     * 切换窗口最大化状态
     */
    fun toggleMaximize() {
        maximizeState = maximizeState.toggle(rawState)
    }

    /**
     * 是否可视
     */
    var visible by mutableStateOf(true)

    /**
     * 是否置顶
     */
    var alwaysOnTop by mutableStateOf(false)

    /**
     * 标题
     */
    var title by mutableStateOf(initTitle)

    /**
     * 图标
     */
    var icon by mutableRefStateOf(initIcon)

    /**
     * 窗口圆角
     */
    var roundedCorner by mutableStateOf(initRoundedCorner)

    /**
     * 窗口置顶按钮
     */
    var actionAlwaysOnTop by mutableStateOf(initActionAlwaysOnTop)

    /**
     * 最小化按钮
     */
    var actionMinimize by mutableStateOf(initActionMinimize)

    /**
     * 最大化按钮
     */
    var actionMaximize by mutableStateOf(initActionMaximize)

    /**
     * 关闭按钮
     */
    var actionClose by mutableStateOf(initActionClose)

    val iconPainter: Painter @Composable get() = icon?.let { painterResource(it) } ?: rememberVectorPainter(Icons.ComposeMultiplatform)
}