package love.yinlin

import love.yinlin.nativecompose.window.drawSkikoDemo
import love.yinlin.nativecompose.window.runNativeWindow
import org.jetbrains.skia.Canvas

/** Latest validated stage, consumed as a Rachel app through official commonMain dependencies. */
fun main(args: Array<String>) = runNativeWindow(args, ::renderShowcase)

private fun renderShowcase(canvas: Canvas, width: Int, height: Int) {
    canvas.save()
    try { drawSkikoDemo(canvas, width, height) }
    finally { canvas.restore() }
}
