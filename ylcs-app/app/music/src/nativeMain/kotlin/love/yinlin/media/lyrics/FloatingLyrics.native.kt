package love.yinlin.media.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import love.yinlin.app
import love.yinlin.compose.ApplicationWindow
import love.yinlin.compose.Colors
import love.yinlin.compose.NativeWindow
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.coroutines.Coroutines
import love.yinlin.startup.StartupMusicPlayer
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowOptions
import org.jetbrains.skiko.window.NativeWindowScope
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@Stable
actual class FloatingLyrics actual constructor(val mp: StartupMusicPlayer) {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val config get() = app.config.lyricsEngineConfig
    private var windowSize by mutableStateOf(DpSize(config.desktop.width.dp, config.desktop.height.dp))
    private var windowPosition by mutableStateOf(DpOffset(config.desktop.x.dp, config.desktop.y.dp))
    private var currentWindow: ApplicationWindow? = null

    var canMove: Boolean by mutableStateOf(false)

    actual fun attach() = Coroutines.dispatchUI {
        if (currentWindow == null) {
            val window = createWindow()
            currentWindow = window
            app.openWindow(window)
        }
    }

    actual fun detach() = Coroutines.dispatchUI {
        val window = currentWindow
        currentWindow = null
        isAttached = false
        window?.close()
    }

    actual suspend fun initDelay() {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    internal fun updateWindowState(size: DpSize, position: DpOffset) {
        windowSize = size
        windowPosition = position
    }

    private fun applyGeometry(window: NativeWindowScope) {
        val scale = window.displayScale / window.pixelDensity
        val width = (windowSize.width.value * scale).roundToInt()
        val height = (windowSize.height.value * scale).roundToInt()
        val state = window.state
        if (state.width != width || state.height != height) window.setSize(width, height)
        if (window.isPositionSupported) {
            val x = (windowPosition.x.value * scale).roundToInt()
            val y = (windowPosition.y.value * scale).roundToInt()
            if (state.x != x || state.y != y) window.setPosition(x, y)
        }
    }

    private fun updateGeometry(window: NativeWindowScope) {
        (val width, val height, val x, val y) = window.state
        if (width <= 0 || height <= 0) return
        val scale = window.displayScale / window.pixelDensity
        windowSize = DpSize((width / scale).dp, (height / scale).dp)
        if (window.isPositionSupported) windowPosition = DpOffset((x / scale).dp, (y / scale).dp)
    }

    @OptIn(FlowPreview::class)
    private fun createWindow(): ApplicationWindow = object : ApplicationWindow() {
        override val background = Colors.Transparent
        private var window: NativeWindowScope? = null

        override fun uninitialize() {
            window = null
            if (currentWindow === this) {
                currentWindow = null
                isAttached = false
            }
        }

        @Composable
        override fun Window(content: @Composable () -> Unit) {
            NativeWindow(
                options = NativeWindowOptions(
                    title = "",
                    width = windowSize.width.value.roundToInt(),
                    height = windowSize.height.value.roundToInt(),
                    transparent = true,
                    undecorated = true,
                    focusable = false,
                    resizable = canMove,
                    clickThrough = !canMove,
                ),
                onOpen = {
                    window = it
                    it.setAlwaysOnTop(true)
                    applyGeometry(it)
                    if (currentWindow === this) isAttached = true
                },
                onEvent = { event ->
                    if (event is NativeWindowEvent.WindowStateChanged || event == NativeWindowEvent.EnvironmentChanged) window?.let(::updateGeometry)
                },
                onClose = { window = null },
            ) {
                LaunchedEffect(canMove) {
                    setResizable(canMove)
                    setClickThrough(!canMove)
                }

                LaunchedEffect(windowSize, windowPosition) {
                    applyGeometry(this@NativeWindow)
                }

                LaunchedEffect(Unit) {
                    snapshotFlow { windowSize to windowPosition }.debounce(300.milliseconds).collect { [size, position] ->
                        val current = config
                        val desktop = current.desktop.copy(width = size.width.value, height = size.height.value, x = position.x.value, y = position.y.value)
                        if (desktop != current.desktop) app.config.lyricsEngineConfig = current.copy(desktop = desktop)
                    }
                }

                if (mp.isInit) DragArea(enabled = canMove) { content() }
            }
        }

        @Composable
        override fun Content() {
            Box(modifier = Modifier.fillMaxSize().condition(canMove) { background(Colors.Black.copy(alpha = 0.5f)) }) {
                if (mp.isPlaying) mp.engine.FloatingLyricsCanvas(modifier = Modifier.fillMaxSize(), config = app.config.lyricsEngineConfig, textStyle = Theme.typography.v3.bold)
            }
        }
    }
}
