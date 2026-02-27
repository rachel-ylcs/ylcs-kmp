package love.yinlin.media.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.extension.lazyProvider
import love.yinlin.foundation.Context
import love.yinlin.platform.NativeWindow
import love.yinlin.startup.StartupMusicPlayer

@Stable
actual class FloatingLyrics {
    private val mp by lazyProvider { app.startup<StartupMusicPlayer>() }

    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val config get() = app.config.lyricsEngineConfig

    private val windowState = WindowState(
        placement = WindowPlacement.Floating,
        isMinimized = false,
        position = WindowPosition.Absolute(config.desktop.x.dp, config.desktop.y.dp),
        width = config.desktop.width.dp,
        height = config.desktop.height.dp
    )

    var canMove: Boolean by mutableStateOf(false)

    actual fun attach() { isAttached = true }

    actual fun detach() { isAttached = false }

    actual suspend fun initDelay(context: Context) {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    actual fun update() { }

    @OptIn(FlowPreview::class)
    @Composable
    actual fun Content() {
        Window(
            onCloseRequest = {},
            state = windowState,
            title = "",
            undecorated = true,
            transparent = true,
            resizable = canMove,
            focusable = false,
            alwaysOnTop = true
        ) {
            LaunchedEffect(canMove) {
                NativeWindow.updateClickThrough(window.windowHandle, !canMove)
            }

            LaunchedEffect(windowState) {
                snapshotFlow { windowState.size }.debounce(300L).onEach { size: DpSize ->
                    app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(width = size.width.value, height = size.height.value))
                }.launchIn(this)
                snapshotFlow { windowState.position }.debounce(300L).filter { it.isSpecified }.onEach { position: WindowPosition ->
                    app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(x = position.x.value, y = position.y.value))
                }.launchIn(this)
            }

            mp?.let { player ->
                if (player.isPlaying) {
                    DragArea(enabled = canMove) {
                        app.ComposedLayout(
                            modifier = Modifier.fillMaxSize().condition(canMove) { background(Colors.Black.copy(alpha = 0.5f)) },
                            bgColor = Colors.Transparent
                        ) {
                            player.engine.FloatingLyricsCanvas(config = app.config.lyricsEngineConfig, textStyle = Theme.typography.v3.bold)
                        }
                    }
                }
            }
        }
    }
}