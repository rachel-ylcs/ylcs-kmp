package love.yinlin.media.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.v2.Window
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowPositionProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.v2.WindowState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.ApplicationWindow
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.platform.updateClickThrough
import love.yinlin.startup.StartupMusicPlayer
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalComposeUiApi::class)
@Stable
actual class FloatingLyrics actual constructor(val mp: StartupMusicPlayer) {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val config get() = app.config.lyricsEngineConfig

    private val windowState = WindowState(
        initialPlacement = WindowPlacement.Floating,
        initialBoundsProvider = WindowBoundsProvider(
            positionProvider = WindowPositionProvider.Absolute(config.desktop.x.dp, config.desktop.y.dp),
            sizeProvider = WindowSizeProvider.Fixed(config.desktop.width.dp, config.desktop.height.dp)
        ),
        initiallyMinimized = false
    )

    var canMove: Boolean by mutableStateOf(false)
    private var currentWindow: ApplicationWindow? = null

    actual fun attach() {
        if (isAttached) return
        isAttached = true
        val window = createWindow()
        currentWindow = window
        app.openWindow(window)
    }

    actual fun detach() {
        currentWindow?.close()
        currentWindow = null
        isAttached = false
    }

    actual suspend fun initDelay() {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    internal fun updateWindowState(size: DpSize, position: DpOffset) {
        windowState.requestBounds(DpRect(position, size))
    }

    @OptIn(FlowPreview::class)
    private fun createWindow(): ApplicationWindow = object : ApplicationWindow() {
        override val background = Colors.Transparent

        override fun uninitialize() {
            if (currentWindow === this) {
                currentWindow = null
                isAttached = false
            }
        }

        @Composable
        override fun Window(content: @Composable () -> Unit) {
            Window(
                onCloseRequest = ::close,
                state = windowState,
                title = "",
                decoration = WindowDecoration.Undecorated(),
                transparent = true,
                resizable = canMove,
                focusable = false,
                alwaysOnTop = true
            ) {
                LaunchedEffect(canMove) {
                    updateClickThrough(window.windowHandle, !canMove)
                }

                LaunchedEffect(windowState.isInitialized) {
                    if (windowState.isInitialized) {
                        snapshotFlow { windowState.size }.distinctUntilChanged().debounce(300.milliseconds).filter { it.isSpecified }.onEach { size: DpSize ->
                            app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(width = size.width.value, height = size.height.value))
                        }.launchIn(this)
                        snapshotFlow { windowState.position }.distinctUntilChanged().debounce(300.milliseconds).filter { it.isSpecified }.onEach { position: DpOffset ->
                            app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(x = position.x.value, y = position.y.value))
                        }.launchIn(this)
                    }
                }

                if (mp.isInit) {
                    DragArea(enabled = canMove) { content() }
                }
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
