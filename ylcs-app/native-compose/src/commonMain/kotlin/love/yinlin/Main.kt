package love.yinlin

import org.jetbrains.skiko.window.NativeWindow
import org.jetbrains.skiko.window.NativeWindowBackend
import org.jetbrains.skiko.window.NativeWindowKey
import org.jetbrains.skiko.window.NativeWindowOptions

/** Interactive showcase of the current Skiko port, using official commonMain dependencies. */
fun main(args: Array<String>) {
    val scene = ShowcaseScene()
    try {
        NativeWindow.run(
            options = NativeWindowOptions(
                title = "Rachel / Native graphics lab",
                width = 1000,
                height = 660,
                backend = if ("--software" in args) NativeWindowBackend.SOFTWARE else NativeWindowBackend.GPU,
                continuous = true,
                smoke = "--smoke" in args,
            ),
            onKey = { if (it == NativeWindowKey.SPACE) scene.paused = !scene.paused },
        ) { canvas, width, height, delta ->
            scene.advance(delta)
            scene.render(canvas, width, height)
        }
    }
    finally { scene.close() }
}
