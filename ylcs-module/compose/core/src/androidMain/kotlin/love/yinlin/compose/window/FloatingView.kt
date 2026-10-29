package love.yinlin.compose.window

import android.content.Context
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import love.yinlin.extension.cleaning

abstract class FloatingView {
    open val layoutInScreen: Boolean = true
    open val touchable: Boolean = true

    abstract fun onAttached()

    abstract fun onDetached()

    @Composable
    abstract fun Content()

    private var view: ComposeView? = null
    private var manager: WindowManager? = null
    private var owner: FloatingViewLifecycleOwner? = null
    private var attached = false

    private val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            attached = true
            owner?.start()
            onAttached()
        }

        override fun onViewDetachedFromWindow(v: View) = detach(removeView = false)
    }

    fun attach(context: Context) {
        if (view != null) return

        val application = context.applicationContext
        val display = checkNotNull(application.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)) { "No display for the overlay window" }
        val displayContext = application.createDisplayContext(display)
        val windowContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            displayContext.createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
        }
        else displayContext
        val windowManager = checkNotNull(windowContext.getSystemService(WindowManager::class.java))
        val composeView = ComposeView(windowContext)
        val lifecycleOwner = FloatingViewLifecycleOwner(application)

        owner = lifecycleOwner
        manager = windowManager
        view = composeView
        try {
            composeView.setViewTreeLifecycleOwner(lifecycleOwner)
            composeView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            composeView.setViewTreeViewModelStoreOwner(lifecycleOwner)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            composeView.addOnAttachStateChangeListener(listener)
            composeView.setContent(::Content)

            var flags = 0
            if (layoutInScreen) flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            if (!touchable) {
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                flags,
                PixelFormat.TRANSLUCENT,
            ).apply { gravity = Gravity.TOP or Gravity.START }
            windowManager.addView(composeView, params)
        }
        catch (failure: Throwable) {
            try { detach() }
            catch (cleanup: Throwable) { if (cleanup !== failure) failure.addSuppressed(cleanup) }
            throw failure
        }
    }

    fun detach() = detach(removeView = true)

    private fun detach(removeView: Boolean) {
        val composeView = view ?: return
        val windowManager = manager
        val lifecycleOwner = owner
        val wasAttached = attached
        view = null
        manager = null
        owner = null
        attached = false
        composeView.removeOnAttachStateChangeListener(listener)

        cleaning({
            if (removeView && (composeView.isAttachedToWindow || composeView.parent != null)) windowManager?.removeViewImmediate(composeView)
        }) {
            cleaning({ composeView.disposeComposition() }) {
                cleaning({ lifecycleOwner?.destroy() }) {
                    composeView.setViewTreeLifecycleOwner(null)
                    composeView.setViewTreeSavedStateRegistryOwner(null)
                    composeView.setViewTreeViewModelStoreOwner(null)
                    if (wasAttached) onDetached()
                }
            }
        }
    }

    fun updateLayoutParams(gravity: Int, offset: Offset) {
        val composeView = view ?: return
        val windowManager = manager ?: return
        val params = composeView.layoutParams as? WindowManager.LayoutParams ?: return
        val x = offset.x.toInt()
        val y = offset.y.toInt()
        if (params.gravity == gravity && params.x == x && params.y == y) return
        params.gravity = gravity
        params.x = x
        params.y = y
        windowManager.updateViewLayout(composeView, params)
    }
}
