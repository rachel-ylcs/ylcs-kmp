package androidx.compose.ui.window

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.NativeSceneHost
import androidx.compose.ui.platform.registerSkikoComposeImplementation
import org.jetbrains.skiko.window.NativeWindowOptions
import org.jetbrains.skiko.window.NativeWindow as SkikoNativeWindow
import kotlin.math.roundToInt

@ExperimentalComposeUiApi
actual fun NativeWindow(options: NativeWindowOptions, onCloseRequest: () -> Boolean, content: @Composable () -> Unit) {
    registerSkikoComposeImplementation()
    val host = NativeSceneHost(onCloseRequest)
    try {
        SkikoNativeWindow.open(options, onOpen = {
            val scale = it.displayScale / it.pixelDensity
            if (scale != 1f) it.setSize((options.width * scale).roundToInt(), (options.height * scale).roundToInt())
            host.attach(it)
            host.setContent(content)
        }, onEvent = host::onEvent, onClose = host::close, render = host::render)
    }
    finally { host.close() }
}
