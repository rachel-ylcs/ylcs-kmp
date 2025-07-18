package love.yinlin.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import love.yinlin.DeviceWrapper
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ThemeValue
import love.yinlin.ui.component.node.condition

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override var isAttached: Boolean by mutableStateOf(false)
    private var currentLyrics: String? by mutableStateOf(null)

    var canMove: Boolean by mutableStateOf(false)

    override fun updateLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }

    private external fun modifyWindow(windowHandle: Long, clickThrough: Boolean)

    @Composable
    private fun WindowScope.DragArea(enabled: Boolean, content: @Composable () -> Unit) {
        if (enabled) WindowDraggableArea(content = content)
        else content()
    }

    @OptIn(FlowPreview::class)
    @Composable
    fun Content() {
        (app.musicFactory.floatingLyrics as? ActualFloatingLyrics)?.let { floatingLyrics ->
            val config by derivedStateOf { app.config.floatingLyricsDesktopConfig }
            val state = rememberWindowState(
                placement = WindowPlacement.Floating,
                isMinimized = false,
                position = WindowPosition.Absolute(config.x.dp, config.y.dp),
                width = config.width.dp,
                height = config.height.dp
            )
            Window(
                onCloseRequest = {},
                state = state,
                title = "",
                undecorated = true,
                transparent = true,
                resizable = canMove,
                focusable = false,
                alwaysOnTop = true
            ) {
                LaunchedEffect(floatingLyrics.canMove) {
                    modifyWindow(window.windowHandle, !floatingLyrics.canMove)
                }

                LaunchedEffect(state) {
                    snapshotFlow { state.size }.debounce(300L).onEach { size: DpSize ->
                        app.config.floatingLyricsDesktopConfig = config.copy(width = size.width.value, height = size.height.value)
                    }.launchIn(this)
                    snapshotFlow { state.position }.debounce(300L).filter { it.isSpecified }.onEach { position: WindowPosition ->
                        app.config.floatingLyricsDesktopConfig = config.copy(x = position.x.value, y = position.y.value)
                    }.launchIn(this)
                }

                currentLyrics?.let { lyrics ->
                    DragArea(enabled = floatingLyrics.canMove) {
                        BoxWithConstraints(
                            modifier = Modifier.fillMaxSize().condition(floatingLyrics.canMove) { background(Colors.Black.copy(alpha = 0.3f)) },
                            contentAlignment = Alignment.Center
                        ) {
                            DeviceWrapper(
                                device = remember(this.maxWidth) { Device(this.maxWidth) },
                                themeMode = app.config.themeMode,
                                fontScale = 1f
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = lyrics,
                                        style = MaterialTheme.typography.displayMedium.copy(
                                            fontSize = MaterialTheme.typography.displayLarge.fontSize * config.textSize
                                        ),
                                        color = Colors.from(config.textColor),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.background(color = Colors.from(config.backgroundColor)).padding(ThemeValue.Padding.Value)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}