package love.yinlin.compose.ui

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.LifecycleOwner

@Stable
actual open class PAGState : PlatformView<PlatformPAGView>(), Releasable<PlatformPAGView> {
    internal var stateProgress: Double by mutableDoubleStateOf(0.0)
    internal var flushFlag: Long by mutableLongStateOf(0L)

    private val pagListener = object : PlatformPAGListener {
        override fun onAnimationStart(view: PlatformPAGView) = this@PAGState.onAnimationStart()
        override fun onAnimationEnd(view: PlatformPAGView) = this@PAGState.onAnimationEnd()
        override fun onAnimationCancel(view: PlatformPAGView) = this@PAGState.onAnimationCancel()
        override fun onAnimationRepeat(view: PlatformPAGView) = this@PAGState.onAnimationRepeat()
        override fun onAnimationUpdate(view: PlatformPAGView) { this@PAGState.stateProgress = view.progress }
    }

    override fun build(context: Context, lifecycleOwner: LifecycleOwner, activityResultRegistry: ActivityResultRegistry?): PlatformPAGView {
        val view = PlatformPAGView(context)
        view.addListener(pagListener)
        return view
    }

    override fun release(view: PlatformPAGView) {
        view.removeListener(pagListener)
    }

    actual var progress: Double get() = stateProgress
        set(value) {
            host?.let {
                it.progress = value
                it.flush()
            }
        }

    actual fun flush() { ++flushFlag }
    actual fun freeCache() { host?.freeCache() }
    actual fun makeSnapshot(): ImageBitmap? = host?.makeSnapshot()?.asImageBitmap()

    actual open fun onAnimationStart() { }
    actual open fun onAnimationEnd() { }
    actual open fun onAnimationCancel() { }
    actual open fun onAnimationRepeat() { }
}