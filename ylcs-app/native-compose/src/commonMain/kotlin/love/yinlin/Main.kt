package love.yinlin

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.NativeWindow
import org.jetbrains.skiko.window.NativeWindowBackend
import org.jetbrains.skiko.window.NativeWindowOptions

/** Current UI port, using official commonMain dependencies and the Native bridge. */
@OptIn(ExperimentalComposeUiApi::class)
fun main(args: Array<String>) {
    NativeWindow(
        options = NativeWindowOptions(
            title = "Rachel / Native Foundation + Animation",
            width = 1000,
            height = 660,
            backend = if ("--software" in args) NativeWindowBackend.SOFTWARE else NativeWindowBackend.GPU,
            smoke = "--smoke" in args,
        ),
    ) {
        ShowcaseScene()
    }
}
