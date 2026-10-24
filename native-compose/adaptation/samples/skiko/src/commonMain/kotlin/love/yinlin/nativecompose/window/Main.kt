package love.yinlin.nativecompose.window

import org.jetbrains.skiko.window.NativeWindow
import org.jetbrains.skiko.window.NativeWindowBackend
import org.jetbrains.skiko.window.NativeWindowOptions

fun main(args: Array<String>) {
    NativeWindow.run(NativeWindowOptions(
        title = "Skiko Core + Skottie / static SDL",
        backend = if ("--software" in args) NativeWindowBackend.SOFTWARE else NativeWindowBackend.GPU,
        smoke = "--smoke" in args,
    )) { canvas, width, height, _ -> drawSkikoDemo(canvas, width, height) }
}
