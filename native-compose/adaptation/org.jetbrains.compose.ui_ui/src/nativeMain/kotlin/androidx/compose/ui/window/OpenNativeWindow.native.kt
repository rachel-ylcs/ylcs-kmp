package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.NativeSceneHost
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowOptions
import org.jetbrains.skiko.window.NativeWindowScope
import org.jetbrains.skiko.window.NativeWindow as SkikoNativeWindow
import kotlin.math.roundToInt

/**
 * Opens an independent Compose scene in the main NativeWindow event loop, without blocking it.
 * Close the returned handle on the UI thread to dispose the scene; SDL releases its peer on the next loop turn.
 * Window operations are available through onOpen and the content receiver. onClose runs after the scene has been disposed.
 */
@ExperimentalComposeUiApi
fun openNativeWindow(
    options: NativeWindowOptions = NativeWindowOptions(title = "Compose Native"),
    onCloseRequest: () -> Boolean = { true },
    onOpen: (NativeWindowScope) -> Unit = {},
    onEvent: (NativeWindowEvent) -> Unit = {},
    onClose: () -> Unit = {},
    content: @Composable NativeWindowScope.() -> Unit,
): AutoCloseable {
    registerSkikoComposeImplementation()
    val host = NativeSceneHost(onCloseRequest)
    var closed = false
    val dispose: () -> Unit = {
        if (!closed) {
            closed = true
            try { host.close() }
            finally { onClose() }
        }
    }
    try {
        val window = SkikoNativeWindow.openChild(options, onOpen = {
            val scale = it.displayScale / it.pixelDensity
            if (scale != 1f) it.setSize((options.width * scale).roundToInt(), (options.height * scale).roundToInt())
            host.attach(it)
            onOpen(it)
            val window = it
            host.setContent { content(window) }
        }, onEvent = {
            host.onEvent(it)
            onEvent(it)
        }, onClose = dispose, render = { canvas, width, height, time ->
            if (!closed) host.render(canvas, width, height, time)
        })
        return object : AutoCloseable {
            override fun close() {
                window.close()
                dispose()
            }
        }
    }
    catch (failure: Throwable) {
        try { dispose() }
        catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
        throw failure
    }
}
