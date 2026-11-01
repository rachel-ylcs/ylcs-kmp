package love.yinlin.compose

import androidx.compose.runtime.Stable
import org.jetbrains.skiko.window.NativeWindowBackend
import org.jetbrains.skiko.window.NativeWindowOptions

@Stable
data class WindowOptions(
    val title: String = "Skiko Native",
    val width: Int = 800,
    val height: Int = 480,
    val backend: NativeWindowBackend = NativeWindowBackend.GPU,
    val transparent: Boolean = false,
    val undecorated: Boolean = false,
    val focusable: Boolean = true,
    val resizable: Boolean = true,
    val clickThrough: Boolean = false
) {
    val asNativeWindowOptions: NativeWindowOptions = NativeWindowOptions(
        title = title,
        width = width,
        height = height,
        backend = backend,
        transparent = transparent,
        undecorated = undecorated,
        focusable = focusable,
        resizable = resizable,
        clickThrough = clickThrough
    )
}