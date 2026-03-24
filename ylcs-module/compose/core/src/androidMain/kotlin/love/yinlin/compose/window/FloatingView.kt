package love.yinlin.compose.window

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import love.yinlin.extension.catching
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.toAndroidUri
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

abstract class FloatingView {
    open val layoutInScreen: Boolean = true
    open val touchable: Boolean = true

    abstract fun onAttached()

    abstract fun onDetached()

    @Composable
    abstract fun Content()

    private var view: ComposeView? = null

    private val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { onAttached() }
        override fun onViewDetachedFromWindow(v: View) { onDetached() }
    }

    private fun canAttach(context: Context): Boolean = Settings.canDrawOverlays(context)

    private val Context.windowManager get() = this.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    @OptIn(ExperimentalUuidApi::class)
    fun attach(activity: ComponentActivity, onPermissionFailed: (() -> Unit)? = null) {
        if (canAttach(activity)) {
            val manager = activity.windowManager

            if (manager != null) {
                val composeView = ComposeView(activity)
                composeView.setViewTreeLifecycleOwner(activity)
                composeView.setViewTreeSavedStateRegistryOwner(activity)
                composeView.setViewTreeViewModelStoreOwner(activity)
                composeView.addOnAttachStateChangeListener(listener)
                composeView.setContent(::Content)
                view = composeView

                var flags = 0
                if (layoutInScreen) {
                    flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                }
                if (!touchable) {
                    flags = flags or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    flags,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                }

                manager.addView(composeView, params)
            }
        }
        else if (onPermissionFailed != null) {
            catching {
                activity.activityResultRegistry.register(
                    key = Uuid.generateV7().toString(),
                    contract = ActivityResultContracts.StartActivityForResult()
                ) {
                    val result = canAttach(activity)
                    if (result) attach(activity, onPermissionFailed)
                    else onPermissionFailed()
                }.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    setData(Uri(scheme = Scheme.Package, host = activity.packageName).toAndroidUri())
                })
            }
        }
    }

    fun detach() {
        view?.let { composeView ->
            composeView.context.windowManager?.removeViewImmediate(composeView)
            composeView.removeOnAttachStateChangeListener(listener)
        }
        view = null
    }

    fun updateLayoutParams(gravity: Int, offset: Offset) {
        view?.let { composeView ->
            composeView.context.windowManager?.let { manager ->
                val params = composeView.layoutParams as WindowManager.LayoutParams
                params.gravity = gravity
                params.x = offset.x.toInt()
                params.y = offset.y.toInt()
                manager.updateViewLayout(composeView, params)
            }
        }
    }
}