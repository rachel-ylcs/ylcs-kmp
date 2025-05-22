package love.yinlin.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import love.yinlin.DeviceWrapper
import love.yinlin.common.Colors
import love.yinlin.common.Device
import love.yinlin.common.ThemeValue

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override var isAttached: Boolean by mutableStateOf(false)
    private var currentLyrics: String? by mutableStateOf(null)

    var canMove: Boolean by mutableStateOf(false)

    override fun updateLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }

    private fun modifyWindow(window: ComposeWindow) {
        when (OS.platform) {
            Platform.Windows -> {
                val hwnd = WinDef.HWND(Native.getComponentPointer(window))
                val exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE) or
                        WinUser.WS_EX_LAYERED or
                        WinUser.WS_EX_TRANSPARENT or
                        0x00000080 // WS_EX_TOOLWINDOW
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
            }
            Platform.Linux -> {

            }
            Platform.MacOS -> {

            }
            else -> {}
        }
    }

    @Composable
    override fun Content() {
        val config = app.config.floatingLyricsDesktopConfig
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
            LaunchedEffect(Unit) {
                modifyWindow(window)
            }

            LaunchedEffect(state) {
                snapshotFlow { state.size }.onEach { size: DpSize ->
                    println(size)
                }.launchIn(this)
                snapshotFlow { state.position }.filter { it.isSpecified }.onEach { position: WindowPosition ->
                    println(position)
                }.launchIn(this)
            }

            currentLyrics?.let { lyrics ->
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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