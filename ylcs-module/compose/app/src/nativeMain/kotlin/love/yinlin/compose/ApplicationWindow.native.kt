package love.yinlin.compose

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.openNativeWindow
import love.yinlin.extension.cleaning
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowScope

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ApplicationWindow.NativeWindow(
    options: WindowOptions = WindowOptions(title = "Compose Native"),
    onOpen: (NativeWindowScope) -> Unit = {},
    onEvent: (NativeWindowEvent) -> Unit = {},
    onClose: () -> Unit = {},
    content: @Composable NativeWindowScope.() -> Unit,
) {
    val currentContent by rememberUpdatedState(content)
    val currentOpen by rememberUpdatedState(onOpen)
    val currentEvent by rememberUpdatedState(onEvent)
    val currentClose by rememberUpdatedState(onClose)

    DisposableEffect(this) {
        val window = openNativeWindow(options.asNativeWindowOptions, onCloseRequest = {
            close()
            true
        }, onOpen = {
            currentOpen(it)
        }, onEvent = { currentEvent(it) }, onClose = {
            cleaning(currentClose, ::close)
        }) {
            currentContent(this)
        }

        onDispose {
            window.close()
        }
    }
}
