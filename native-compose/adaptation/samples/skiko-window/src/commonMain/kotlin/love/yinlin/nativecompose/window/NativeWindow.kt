package love.yinlin.nativecompose.window

import org.jetbrains.skia.Canvas

/** Runs the SDL event loop and the platform's required Ganesh backend on the calling window thread. */
expect fun runNativeWindow(args: Array<String>, draw: (Canvas, Int, Int) -> Unit)
