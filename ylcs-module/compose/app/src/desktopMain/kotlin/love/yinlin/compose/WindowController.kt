package love.yinlin.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowPositionProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.v2.WindowState
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.icon.Icons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Stable
@OptIn(ExperimentalComposeUiApi::class)
class WindowController(
    placement: WindowPlacement,
    isMinimized: Boolean,
    position: WindowPosition,
    initSize: DpSize,
    initTitle: String,
    initIcon: DrawableResource?,
    initRoundedCorner: Boolean,
) {
    internal val rawState = WindowState(
        initialPlacement = placement,
        initialBoundsProvider = WindowBoundsProvider(
            positionProvider = when (position) {
                is WindowPosition.Absolute -> WindowPositionProvider.Absolute(position.x, position.y)
                is WindowPosition.Aligned -> WindowPositionProvider.AlignedToScreen(position.alignment)
            },
            sizeProvider = WindowSizeProvider.Fixed(initSize)
        ),
        initiallyMinimized = isMinimized
    )

    private var maximizeState: MaximizeState by mutableRefStateOf(MaximizeState.Normal)

    /**
     * 托盘图标
     */
    val tray: Tray = Tray()

    /**
     * 窗口是否最大化
     */
    val maximize: Boolean get() = maximizeState !is MaximizeState.Normal

    /**
     * 设置窗口最小化状态
     */
    var minimize: Boolean
        get() = rawState.isInitialized && rawState.isMinimized
        set(value) { rawState.requestMinimized(value) }

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
     * 图标绘制
     */
    val iconPainter: Painter @Composable get() = icon?.let { painterResource(it) } ?: rememberVectorPainter(Icons.ComposeMultiplatform)
}
