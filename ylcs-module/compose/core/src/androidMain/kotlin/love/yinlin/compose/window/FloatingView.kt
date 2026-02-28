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
    abstract fun onAttached()

    abstract fun onDetached()

    @Composable
    abstract fun Content()

    private var view: ComposeView? = null

    private val listener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) { onAttached() }
        override fun onViewDetachedFromWindow(v: View) { onDetached() }
    }

    private fun canAttach(activity: ComponentActivity): Boolean = Settings.canDrawOverlays(activity)

    @OptIn(ExperimentalUuidApi::class)
    fun attach(activity: ComponentActivity, onPermissionFailed: (() -> Unit)? = null) {
        if (canAttach(activity)) {
            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

            if (manager != null) {
                val composeView = ComposeView(activity)
                composeView.setViewTreeLifecycleOwner(activity)
                composeView.setViewTreeSavedStateRegistryOwner(activity)
                composeView.setViewTreeViewModelStoreOwner(activity)
                composeView.addOnAttachStateChangeListener(listener)
                composeView.setContent(::Content)
                view = composeView

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply { gravity = Gravity.TOP }

                manager.addView(composeView, params)
            }
        }
        else if (onPermissionFailed != null) {
            catching {
                activity.activityResultRegistry.register(
                    key = Uuid.generateV4().toString(),
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

    fun detach(activity: ComponentActivity) {
        view?.let { composeView ->
            composeView.removeOnAttachStateChangeListener(listener)
            val manager = activity.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            manager?.removeViewImmediate(composeView)
        }
        view = null
    }
}