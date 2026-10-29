package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.NativeSceneHost
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import org.jetbrains.skiko.window.NativeWindowOptions
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowScope
import org.jetbrains.skiko.window.NativeWindow as SkikoNativeWindow
import kotlin.math.roundToInt

@Suppress("FunctionName")
@ExperimentalComposeUiApi
fun NativeWindow(
    options: NativeWindowOptions = NativeWindowOptions(title = "Compose Native"),
    onCloseRequest: () -> Boolean = { true },
    content: @Composable () -> Unit
) = NativeWindow(options = options, onCloseRequest = onCloseRequest, onOpen = {}, content = content)

@Suppress("FunctionName")
@ExperimentalComposeUiApi
fun NativeWindow(
    options: NativeWindowOptions = NativeWindowOptions(title = "Compose Native"),
    onCloseRequest: () -> Boolean = { true },
    onOpen: (NativeWindowScope) -> Unit,
    onEvent: (NativeWindowEvent) -> Unit = {},
    onClose: () -> Unit = {},
    applicationContent: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    registerSkikoComposeImplementation()
    val application = applicationContent?.let { NativeApplicationComposition(it) }
    val host = try { NativeSceneHost(onCloseRequest) }
    catch (failure: Throwable) {
        try { application?.close() }
        catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
        throw failure
    }
    var closed = false
    val dispose: () -> Unit = {
        if (!closed) {
            closed = true
            try { onClose() }
            finally {
                try { host.close() }
                finally { application?.close() }
            }
        }
    }
    try {
        SkikoNativeWindow.open(options, onOpen = {
            val scale = it.displayScale / it.pixelDensity
            if (scale != 1f) it.setSize((options.width * scale).roundToInt(), (options.height * scale).roundToInt())
            host.attach(it)
            onOpen(it)
            application?.attach(it)
            host.setContent(content)
        }, onEvent = {
            host.onEvent(it)
            onEvent(it)
        }, onClose = dispose, onFrame = { application?.frame(it) }, render = host::render)
    }
    finally { dispose() }
}
