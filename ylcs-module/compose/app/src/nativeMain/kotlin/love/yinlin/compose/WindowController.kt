package love.yinlin.compose

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.compose.ui.icon.Icons
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowScope
import org.jetbrains.skiko.window.NativeWindowState
import kotlin.math.roundToInt

@Stable
class WindowController(
    initSize: DpSize,
    initTitle: String,
    isMinimized: Boolean = false,
    initIcon: DrawableResource? = null,
    initRoundedCorner: Boolean = true,
    initUndecorated: Boolean = true,
    private val minSize: DpSize = DpSize(360.dp, 640.dp),
    placement: WindowPlacement = WindowPlacement.Floating,
    position: WindowPosition = WindowPosition.Aligned(Alignment.Center),
    initFocusable: Boolean = true,
    initResizable: Boolean = true,
    initClickThrough: Boolean = false,
) {
    private var window by mutableRefStateOf<NativeWindowScope?>(null)
    private var active by mutableStateOf(false)
    private var minimized by mutableStateOf(isMinimized)
    private var windowPlacement by mutableStateOf(placement)
    private var windowPosition by mutableStateOf(position)
    private var shown by mutableStateOf(true)
    private var topmost by mutableStateOf(false)
    private var windowTitle by mutableStateOf(initTitle)
    private var windowSize by mutableStateOf(initSize)
    private var windowUndecorated by mutableStateOf(initUndecorated)
    private var windowFocusable by mutableStateOf(initFocusable)
    private var windowResizable by mutableStateOf(initResizable)
    private var windowClickThrough by mutableStateOf(initClickThrough)
    private var pendingPosition: WindowPosition? = null
    private var placementAfterFullscreen: WindowPlacement? = null
    internal val windowScope: NativeWindowScope? get() = window

    var icon by mutableRefStateOf(initIcon)
    var roundedCorner by mutableStateOf(initRoundedCorner)
    val tray = Tray()

    val iconPainter: Painter @Composable get() = icon?.let { painterResource(it) } ?: rememberVectorPainter(Icons.ComposeMultiplatform)

    init {
        validateTitle(initTitle)
        validateSize(initSize)
        validateSize(minSize)
        validatePosition(position)
        require(!initClickThrough || initUndecorated) { "Mouse passthrough requires an undecorated window" }
        require(placement != WindowPlacement.Maximized || initResizable) { "A non-resizable window cannot be maximized" }
    }

    val isActive: Boolean get() = active
    val maximize: Boolean get() = windowPlacement != WindowPlacement.Floating

    /** 连接窗口后可查询。Wayland 不支持读取或设置全局窗口坐标。 */
    val isPositionSupported: Boolean get() = window?.isPositionSupported == true

    /** SDL 状态变更可能异步完成，系统事件会回写最终 placement。 */
    var placement: WindowPlacement
        get() = windowPlacement
        set(value) {
            if (windowPlacement == value) return
            require(value != WindowPlacement.Maximized || windowResizable) { "A non-resizable window cannot be maximized" }
            window?.let { updatePlacement(it, value) }
            windowPlacement = value
            if (value != WindowPlacement.Floating) minimized = false
            else window?.let(::applyPendingPosition)
        }

    /** 屏幕 dp 坐标或当前屏幕工作区内的对齐；最大化／全屏期间的请求在还原时应用。 */
    var position: WindowPosition
        get() = windowPosition
        set(value) {
            if (windowPosition == value) return
            validatePosition(value)
            val window = window
            if (window != null) {
                require(value != WindowPosition.PlatformDefault) { "PlatformDefault is only valid before window attachment" }
                if (!window.isPositionSupported) return
                val state = window.state
                if (state.fullscreen || state.maximized || state.minimized) pendingPosition = value
                else if (!move(window, value)) return
            }
            windowPosition = value
        }

    var minimize: Boolean
        get() = minimized
        set(value) {
            if (minimized == value) return
            window?.setMinimized(value)
            minimized = value
        }

    fun toggleMaximize() {
        if (windowPlacement == WindowPlacement.Floating && !windowResizable) return
        placement = if (windowPlacement == WindowPlacement.Floating) WindowPlacement.Maximized else WindowPlacement.Floating
    }

    /** 保存配置值；Wayland 普通顶层窗口忽略焦点能力变更。 */
    var focusable: Boolean
        get() = windowFocusable
        set(value) {
            if (windowFocusable == value) return
            window?.setFocusable(value)
            windowFocusable = value
        }

    /** 全屏期间的修改在退出全屏后应用。 */
    var resizable: Boolean
        get() = windowResizable
        set(value) {
            if (windowResizable == value) return
            window?.let { if (!it.state.fullscreen) it.setResizable(value) }
            windowResizable = value
        }

    /** 整窗鼠标穿透；保留绘制内容，需要无系统标题栏。焦点设置独立控制。 */
    var clickThrough: Boolean
        get() = windowClickThrough
        set(value) {
            if (windowClickThrough == value) return
            require(!value || windowUndecorated) { "Mouse passthrough requires an undecorated window" }
            window?.setClickThrough(value)
            windowClickThrough = value
        }

    var visible: Boolean
        get() = shown
        set(value) {
            if (shown == value) return
            window?.setVisible(value)
            shown = value
        }

    var alwaysOnTop: Boolean
        get() = topmost
        set(value) {
            if (topmost == value) return
            window?.setAlwaysOnTop(value)
            topmost = value
        }

    var title: String
        get() = windowTitle
        set(value) {
            if (windowTitle == value) return
            validateTitle(value)
            window?.setTitle(value)
            windowTitle = value
        }

    var size: DpSize
        get() = windowSize
        set(value) {
            if (windowSize == value) return
            validateSize(value)
            window?.let { resize(it, value) }
            windowSize = value
        }

    var undecorated: Boolean
        get() = windowUndecorated
        set(value) {
            if (windowUndecorated == value) return
            if (!value && windowClickThrough) clickThrough = false
            window?.setUndecorated(value)
            windowUndecorated = value
        }

    internal fun close() = window?.close()

    internal fun attach(window: NativeWindowScope) {
        check(this.window == null) { "WindowController is already attached" }
        this.window = window
        window.setTitle(windowTitle)
        window.setUndecorated(windowUndecorated)
        window.setFocusable(windowFocusable)
        window.setResizable(windowResizable)
        window.setClickThrough(windowClickThrough)
        updateMinimumSize(window)
        resize(window, windowSize)
        if (window.isPositionSupported) move(window, windowPosition)
        else windowPosition = WindowPosition.PlatformDefault
        window.setAlwaysOnTop(topmost)
        updatePlacement(window, windowPlacement)
        window.setMinimized(minimized)
        window.setVisible(shown)
        tray.attach(window) { title }
        active = true
    }

    internal fun detach() {
        tray.detach()
        window = null
        active = false
        pendingPosition = null
        placementAfterFullscreen = null
    }

    @Composable
    internal fun BindIcon() {
        val window = window
        val painter = iconPainter
        val density = LocalDensity.current
        val direction = LocalLayoutDirection.current
        LaunchedEffect(window, painter, painter.intrinsicSize, icon, density, direction) {
            window?.setIcon(painter, density, direction)
        }
    }

    @Composable
    internal fun BindTray() = tray.Bind(iconPainter)

    internal fun onEvent(event: NativeWindowEvent) {
        val window = window ?: return
        when (event) {
            is NativeWindowEvent.WindowStateChanged -> {
                val state = window.state
                minimized = state.minimized
                shown = state.visible
                val nextPlacement = placementAfterFullscreen
                if (nextPlacement != null && !state.fullscreen) {
                    placementAfterFullscreen = null
                    window.setMaximized(nextPlacement == WindowPlacement.Maximized)
                    windowPlacement = nextPlacement
                }
                else if (nextPlacement == null && !state.minimized) {
                    windowPlacement = when {
                        state.fullscreen -> WindowPlacement.Fullscreen
                        state.maximized -> WindowPlacement.Maximized
                        else -> WindowPlacement.Floating
                    }
                }
                windowTitle = state.title
                topmost = state.alwaysOnTop
                if (!state.fullscreen && state.resizable != windowResizable) window.setResizable(windowResizable)
                updateGeometry(window, state)
                applyPendingPosition(window)
            }
            NativeWindowEvent.EnvironmentChanged -> {
                updateMinimumSize(window)
                updateGeometry(window, window.state)
            }
            else -> Unit
        }
    }

    private fun updatePlacement(window: NativeWindowScope, placement: WindowPlacement) {
        placementAfterFullscreen = null
        if (placement == WindowPlacement.Fullscreen) window.setFullscreen(true)
        else {
            if (window.state.fullscreen) {
                placementAfterFullscreen = placement
                window.setFullscreen(false)
                if (window.state.fullscreen) return
                placementAfterFullscreen = null
            }
            window.setMaximized(placement == WindowPlacement.Maximized)
        }
    }

    private fun move(window: NativeWindowScope, position: WindowPosition): Boolean {
        if (!window.isPositionSupported || position == WindowPosition.PlatformDefault) return false
        val scale = window.displayScale / window.pixelDensity
        return when (position) {
            is WindowPosition.Absolute -> window.setPosition(desktopCoordinate(position.x.value * scale), desktopCoordinate(position.y.value * scale))
            is WindowPosition.Aligned -> {
                val bounds = window.displayBounds
                val size = IntSize((windowSize.width.value * scale).roundToInt(), (windowSize.height.value * scale).roundToInt())
                val offset = position.alignment.align(size, IntSize(bounds.width, bounds.height), LayoutDirection.Ltr)
                window.setPosition(bounds.x + offset.x, bounds.y + offset.y)
            }
            WindowPosition.PlatformDefault -> false
        }
    }

    private fun applyPendingPosition(window: NativeWindowScope) {
        val position = pendingPosition ?: return
        val state = window.state
        if (windowPlacement != WindowPlacement.Floating || state.fullscreen || state.maximized || state.minimized) return
        updateGeometry(window, state)
        pendingPosition = null
        if (move(window, position)) windowPosition = position
    }

    private fun resize(window: NativeWindowScope, size: DpSize) {
        val scale = window.displayScale / window.pixelDensity
        val width = (size.width.value * scale).roundToInt()
        val height = (size.height.value * scale).roundToInt()
        val state = window.state
        if (state.width != width || state.height != height) window.setSize(width, height)
    }

    private fun updateMinimumSize(window: NativeWindowScope) {
        val scale = window.displayScale / window.pixelDensity
        window.setMinimumSize((minSize.width.value * scale).roundToInt().coerceAtLeast(1), (minSize.height.value * scale).roundToInt().coerceAtLeast(1))
    }

    private fun updateGeometry(window: NativeWindowScope, state: NativeWindowState) {
        if (state.width <= 0 || state.height <= 0) return
        val scale = window.displayScale / window.pixelDensity
        windowSize = DpSize((state.width / scale).dp, (state.height / scale).dp)
        if (window.isPositionSupported) windowPosition = WindowPosition.Absolute((state.x / scale).dp, (state.y / scale).dp)
    }

    private fun validateTitle(title: String) {
        require(title.length <= 1024 && '\u0000' !in title) { "Invalid Native window title" }
    }

    private fun validateSize(size: DpSize) {
        require(size.width.value.isFinite() && size.height.value.isFinite() && size.width.value > 0f && size.height.value > 0f) { "Native window size must be finite and positive" }
    }

    private fun validatePosition(position: WindowPosition) {
        if (position is WindowPosition.Absolute) require(position.x.value.isFinite() && position.y.value.isFinite()) { "Native window position must be finite" }
    }

    private fun desktopCoordinate(value: Float): Int {
        require(value.isFinite() && value.toDouble() in Int.MIN_VALUE.toDouble() .. Int.MAX_VALUE.toDouble()) { "Native window position is out of range" }
        return value.roundToInt()
    }
}
