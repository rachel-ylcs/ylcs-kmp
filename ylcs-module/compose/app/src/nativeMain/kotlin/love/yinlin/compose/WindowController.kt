package love.yinlin.compose

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
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
) {
    private var window by mutableRefStateOf<NativeWindowScope?>(null)
    private var active by mutableStateOf(false)
    private var minimized by mutableStateOf(isMinimized)
    private var maximized by mutableStateOf(false)
    private var shown by mutableStateOf(true)
    private var topmost by mutableStateOf(false)
    private var windowTitle by mutableStateOf(initTitle)
    private var windowSize by mutableStateOf(initSize)
    private var windowUndecorated by mutableStateOf(initUndecorated)
    internal val dragAreas = mutableSetOf<WindowDragAreaNode>()

    var icon by mutableRefStateOf(initIcon)
    var roundedCorner by mutableStateOf(initRoundedCorner)
    val tray = Tray()

    val iconPainter: Painter @Composable get() = icon?.let { painterResource(it) } ?: rememberVectorPainter(Icons.ComposeMultiplatform)

    init {
        validateTitle(initTitle)
        validateSize(initSize)
        validateSize(minSize)
    }

    val isActive: Boolean get() = active
    val maximize: Boolean get() = maximized

    var minimize: Boolean
        get() = minimized
        set(value) {
            if (minimized == value) return
            window?.setMinimized(value)
            minimized = value
        }

    fun toggleMaximize() {
        val value = !maximized
        window?.setMaximized(value)
        maximized = value
        if (value) minimized = false
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
            window?.setUndecorated(value)
            windowUndecorated = value
        }

    internal fun close() = window?.close()

    internal fun attach(window: NativeWindowScope) {
        check(this.window == null) { "WindowController is already attached" }
        this.window = window
        window.setTitle(windowTitle)
        window.setUndecorated(windowUndecorated)
        updateMinimumSize(window)
        resize(window, windowSize)
        window.setAlwaysOnTop(topmost)
        window.setMaximized(maximized)
        window.setMinimized(minimized)
        window.setVisible(shown)
        window.setDragArea(::isDragArea)
        tray.attach(window) { title }
        active = true
    }

    internal fun detach() {
        tray.detach()
        window?.setDragArea(null)
        window = null
        active = false
        dragAreas.clear()
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

    private fun isDragArea(x: Float, y: Float): Boolean {
        var draggable = false
        for (area in dragAreas) {
            if (area.contains(x, y)) {
                if (!area.draggable) return false
                draggable = true
            }
        }
        return draggable
    }

    internal fun onEvent(event: NativeWindowEvent) {
        val window = window ?: return
        when (event) {
            is NativeWindowEvent.WindowStateChanged -> {
                val state = window.state
                when (event.kind) {
                    NativeWindowEvent.WindowStateChanged.Kind.Placement -> {
                        minimized = state.minimized
                        maximized = state.maximized
                    }
                    NativeWindowEvent.WindowStateChanged.Kind.Visibility -> shown = state.visible
                    NativeWindowEvent.WindowStateChanged.Kind.Size -> Unit
                }
                windowTitle = state.title
                topmost = state.alwaysOnTop
                updateSize(window, state)
            }
            NativeWindowEvent.EnvironmentChanged -> {
                updateMinimumSize(window)
                updateSize(window, window.state)
            }
            else -> Unit
        }
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

    private fun updateSize(window: NativeWindowScope, state: NativeWindowState) {
        if (state.width <= 0 || state.height <= 0) return
        val scale = window.displayScale / window.pixelDensity
        windowSize = DpSize((state.width / scale).dp, (state.height / scale).dp)
    }

    private fun validateTitle(title: String) {
        require(title.length <= 1024 && '\u0000' !in title) { "Invalid Native window title" }
    }

    private fun validateSize(size: DpSize) {
        require(size.width.value.isFinite() && size.height.value.isFinite() && size.width.value > 0f && size.height.value > 0f) { "Native window size must be finite and positive" }
    }
}
