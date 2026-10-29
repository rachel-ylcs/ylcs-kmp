package love.yinlin.compose

import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.openNativeWindow
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowOptions
import org.jetbrains.skiko.window.NativeWindowScope

/**
 * 创建应用管理的独立 Native 子窗口。options 为创建参数，运行时操作可通过 onOpen 或内容中的 NativeWindowScope 完成。
 * 离开组合时关闭窗口，系统关闭窗口时同时结束 ApplicationWindow 的生命周期。
 */
@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun ApplicationWindow.NativeWindow(
    options: NativeWindowOptions = NativeWindowOptions(title = "Compose Native"),
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
        var scope: NativeWindowScope? = null
        val window = openNativeWindow(options, onCloseRequest = {
            close()
            true
        }, onOpen = {
            scope = it
            currentOpen(it)
        }, onEvent = { currentEvent(it) }, onClose = {
            try { currentClose() }
            finally { close() }
        }) {
            scope?.let { currentContent(it) }
        }
        onDispose { window.close() }
    }
}
