package love.yinlin.platform.lyrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import love.yinlin.Context
import love.yinlin.app
import love.yinlin.compose.Colors
import love.yinlin.compose.ui.node.condition
import love.yinlin.extension.NativeLib
import love.yinlin.platform.setWindowClickThrough

@Composable
private fun WindowScope.DragArea(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) WindowDraggableArea(content = content)
    else content()
}

@Stable
actual class FloatingLyrics {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private val config by derivedStateOf { app.config.lyricsEngineConfig }

    private val windowState = WindowState(
        placement = WindowPlacement.Floating,
        isMinimized = false,
        position = WindowPosition.Absolute(config.desktop.x.dp, config.desktop.y.dp),
        width = config.desktop.width.dp,
        height = config.desktop.height.dp
    )

    var canMove: Boolean by mutableStateOf(false)

    actual fun attach() {
        isAttached = true
    }

    actual fun detach() {
        isAttached = false
    }

    actual suspend fun initDelay(context: Context) {
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    actual fun update() = Unit

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
                @NativeLib
                setWindowClickThrough(window.windowHandle, !canMove)
            }

            LaunchedEffect(windowState) {
                snapshotFlow { windowState.size }.debounce(300L).onEach { size: DpSize ->
                    app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(width = size.width.value, height = size.height.value))
                }.launchIn(this)
                snapshotFlow { windowState.position }.debounce(300L).filter { it.isSpecified }.onEach { position: WindowPosition ->
                    app.config.lyricsEngineConfig = config.copy(desktop = config.desktop.copy(x = position.x.value, y = position.y.value))
                }.launchIn(this)
            }

            if (app.mp.isPlaying) {
                DragArea(enabled = canMove) {
                    app.Layout(modifier = Modifier.fillMaxSize().condition(canMove) { background(Colors.Black.copy(alpha = 0.3f)) }) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            app.mp.engine.LyricsCanvas(config = app.config.lyricsEngineConfig, textStyle = MaterialTheme.typography.displayLarge)
                        }
                    }
                }
            }
        }
    }
}