@file:OptIn(ExperimentalForeignApi::class)
package love.yinlin.compose.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import platform.darwin.NSObject

@Stable
actual open class PAGState : PlatformView<PlatformPAGView>(), Releasable<PlatformPAGView> {
    internal var stateProgress: Double by mutableDoubleStateOf(0.0)
    internal var flushFlag: Long by mutableLongStateOf(0L)

    private val pagListener = object : NSObject(), PlatformPAGListener {
        override fun onAnimationStart(pagView: PlatformPAGView?) = this@PAGState.onAnimationStart()
        override fun onAnimationEnd(pagView: PlatformPAGView?) = this@PAGState.onAnimationEnd()
        override fun onAnimationCancel(pagView: PlatformPAGView?) = this@PAGState.onAnimationCancel()
        override fun onAnimationRepeat(pagView: PlatformPAGView?) = this@PAGState.onAnimationRepeat()
        override fun onAnimationUpdate(pagView: PlatformPAGView?) { this@PAGState.stateProgress = pagView?.getProgress() ?: 0.0 }
    }

    override fun build(): PlatformPAGView {
        val view = PlatformPAGView()
        view.addListener(pagListener)
        return view
    }

    override fun release(view: PlatformPAGView) {
        view.removeListener(pagListener)
    }

    actual var progress: Double get() = stateProgress
        set(value) {
            host?.let {
                it.setProgress(value)
                it.flush()
            }
        }

    actual fun flush() { ++flushFlag }
    actual fun freeCache() { host?.freeCache() }
    actual fun makeSnapshot(): ImageBitmap? = host?.makeSnapshot()?.let(::createImageFromPixelBuffer)

    actual open fun onAnimationStart() { }
    actual open fun onAnimationEnd() { }
    actual open fun onAnimationCancel() { }
    actual open fun onAnimationRepeat() { }
}