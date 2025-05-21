package love.yinlin.platform

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override var isAttached: Boolean by mutableStateOf(false)
    private var currentLyrics: String? by mutableStateOf(null)

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
        Window(
            onCloseRequest = {},
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                isMinimized = false,
                position = WindowPosition.Aligned(Alignment.BottomCenter),
                width = 1200.dp,
                height = 100.dp
            ),
            title = "",
            undecorated = true,
            transparent = true,
            resizable = false,
            focusable = false,
            alwaysOnTop = true
        ) {
            LaunchedEffect(Unit) {
                modifyWindow(window)
            }


        }

//        val config = app.config.floatingLyricsConfig
//        currentLyrics?.let { lyrics ->
//            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
//                Box(
//                    modifier = Modifier.padding(
//                        start = this.maxWidth * config.left.coerceIn(0f, 1f),
//                        end = this.maxWidth * (1 - config.right).coerceIn(0f, 1f),
//                        top = ThemeValue.Padding.VerticalExtraSpace * 4f * config.top
//                    ).fillMaxWidth(),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Text(
//                        text = lyrics,
//                        style = MaterialTheme.typography.labelLarge.copy(
//                            fontSize = MaterialTheme.typography.labelLarge.fontSize * config.textSize
//                        ),
//                        color = Color(config.textColor),
//                        textAlign = TextAlign.Center,
//                        maxLines = 2,
//                        overflow = TextOverflow.Ellipsis,
//                        modifier = Modifier.background(color = Color(config.backgroundColor)).padding(ThemeValue.Padding.Value)
//                    )
//                }
//            }
//        }
    }
}