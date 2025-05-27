package love.yinlin.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.window.WindowDraggableArea
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
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.rememberWindowState
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
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

@Suppress("FunctionName")
interface ObjCRuntime : Library {
    companion object {
        val INSTANCE: ObjCRuntime = Native.load("objc", ObjCRuntime::class.java)
    }

    fun objc_getClass(className: String?): Pointer?
    fun sel_registerName(selectorName: String?): Pointer?
    fun objc_msgSend(receiver: Pointer?, selector: Pointer?, vararg args: Any?): Pointer?
}

@Stable
class ActualFloatingLyrics : FloatingLyrics() {
    override var isAttached: Boolean by mutableStateOf(false)
    private var currentLyrics: String? by mutableStateOf(null)

    var canMove: Boolean by mutableStateOf(false)

    override fun updateLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }

    private fun modifyWindow(enabled: Boolean, window: ComposeWindow) {
        when (OS.platform) {
            Platform.Windows -> {
                val hwnd = WinDef.HWND(Native.getComponentPointer(window))
                var exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE)
                exStyle = exStyle or 0x00000080 // WS_EX_TOOLWINDOW
                exStyle = if (enabled) exStyle or WinUser.WS_EX_TRANSPARENT else exStyle and WinUser.WS_EX_TRANSPARENT.inv()
                User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle)
            }
            Platform.Linux -> {
                // TODO
            }
            Platform.MacOS -> {
                val objc: ObjCRuntime = ObjCRuntime.INSTANCE
                val nsWindow = Pointer(window.windowHandle)
                val setIgnoresMouseEventsSel: Pointer? = objc.sel_registerName("setIgnoresMouseEvents:")
                objc.objc_msgSend(nsWindow, setIgnoresMouseEventsSel, enabled)
            }
            else -> {}
        }
    }

    @Composable
    private fun WindowScope.DragArea(enabled: Boolean, content: @Composable () -> Unit) {
        if (enabled) WindowDraggableArea(content = content)
        else content()
    }

    @OptIn(FlowPreview::class)
    @Composable
    override fun Content() {
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
                    modifyWindow(!floatingLyrics.canMove, window)
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