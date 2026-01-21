package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import love.yinlin.compose.extension.mutableRefStateOf
import love.yinlin.extension.createElement
import org.w3c.dom.HTMLCanvasElement
import org.w3c.dom.HTMLDivElement
import kotlin.js.ExperimentalWasmJsInterop

@Stable
@OptIn(ExperimentalWasmJsInterop::class)
actual open class PAGState : PlatformView<HTMLCanvasElement>(), Releasable<HTMLCanvasElement> {
    internal var stateProgress: Double by mutableDoubleStateOf(0.0)
    internal var flushFlag: Long by mutableLongStateOf(0L)
    internal var pagView: PlatformPAGView? by mutableRefStateOf(null)

    override fun build(): HTMLCanvasElement {
        return createElement<HTMLCanvasElement> {
            style.setProperty("pointer-events", "none")
            (parentElement as? HTMLDivElement)?.style?.setProperty("pointer-events", "none")
        }
    }

    override fun release(view: HTMLCanvasElement) {
        pagView?.let {
            it.removeListener("onAnimationStart", null)
            it.removeListener("onAnimationEnd", null)
            it.removeListener("onAnimationCancel", null)
            it.removeListener("onAnimationRepeat", null)
            it.removeListener("onAnimationUpdate", null)
            it.destroy()
        }
        pagView = null
    }

    actual var progress: Double get() = stateProgress
        set(value) {
            pagView?.let {
                it.setProgress(value)
                it.flush()
            }
        }

    actual fun flush() { ++flushFlag }
    actual fun freeCache() { pagView?.freeCache() }
    actual fun makeSnapshot(): ImageBitmap? = null

    actual open fun onAnimationStart() { }
    actual open fun onAnimationEnd() { }
    actual open fun onAnimationCancel() { }
    actual open fun onAnimationRepeat() { }
}