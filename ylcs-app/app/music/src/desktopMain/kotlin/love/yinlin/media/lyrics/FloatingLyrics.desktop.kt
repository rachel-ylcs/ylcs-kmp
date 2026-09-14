package love.yinlin.media.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.window.*
import androidx.compose.ui.window.v2.Window
import androidx.compose.ui.window.v2.WindowBoundsProvider
import androidx.compose.ui.window.v2.WindowPositionProvider
import androidx.compose.ui.window.v2.WindowSizeProvider
import androidx.compose.ui.window.v2.WindowState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.Theme
import love.yinlin.compose.bold
import love.yinlin.compose.ui.node.condition
import love.yinlin.compose.ui.window.DragArea
import love.yinlin.platform.NativeWindow
import love.yinlin.startup.StartupMusicPlayer
import kotlin.time.Duration.Companion.milliseconds

@Stable
@OptIn(ExperimentalComposeUiApi::class)
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

    actual fun attach() { isAttached = true }

    actual fun detach() { isAttached = false }

    actual suspend fun initDelay() {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    internal fun updateWindowState(size: DpSize, position: DpOffset) {
        windowState.requestBounds(DpRect(position, size))
    }

    @OptIn(FlowPreview::class)
    @Composable
    fun Content() {
        if (isAttached) {
            Window(
                onCloseRequest = {},
                state = windowState,
                title = "",
                decoration = WindowDecoration.Undecorated(),
                transparent = true,
                resizable = canMove,
                focusable = false,
                alwaysOnTop = true
            ) {
                LaunchedEffect(canMove) {
                    NativeWindow.updateClickThrough(window.windowHandle, !canMove)
                }

                LaunchedEffect(windowState) {
                    snapshotFlow { if (windowState.isInitialized) windowState.size else DpSize.Unspecified }.distinctUntilChanged().debounce(300.milliseconds).filter { it.isSpecified }.onEach { size: DpSize ->
                        app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(width = size.width.value, height = size.height.value))
                    }.launchIn(this)
                    snapshotFlow { if (windowState.isInitialized) windowState.position else DpOffset.Unspecified }.distinctUntilChanged().debounce(300.milliseconds).filter { it.isSpecified }.onEach { position: DpOffset ->
                        app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(x = position.x.value, y = position.y.value))
                    }.launchIn(this)
                }

                if (mp.isInit) {
                    DragArea(enabled = canMove) {
                        app.ComposedLayout(
                            modifier = Modifier.fillMaxSize().condition(canMove) { background(Colors.Black.copy(alpha = 0.5f)) },
                            bgColor = Colors.Transparent
                        ) {
                            if (mp.isPlaying) {
                                mp.engine.FloatingLyricsCanvas(modifier = Modifier.fillMaxSize(), config = app.config.lyricsEngineConfig, textStyle = Theme.typography.v3.bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
