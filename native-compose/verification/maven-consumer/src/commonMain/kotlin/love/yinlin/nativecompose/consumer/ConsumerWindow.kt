package love.yinlin.nativecompose.consumer

import org.jetbrains.skia.Color
import org.jetbrains.skiko.window.NativeWindow
import org.jetbrains.skiko.window.NativeWindowBackend
import org.jetbrains.skiko.window.NativeWindowOptions

/** Verifies published window API, platform bindings and static SDL without a local SDK. */
internal fun runConsumerWindow() {
    NativeWindow.run(NativeWindowOptions(
        title = "Maven-only Native window",
        backend = NativeWindowBackend.SOFTWARE,
        smoke = true,
    )) { canvas, _, _, _ -> canvas.clear(Color.BLUE) }
}
