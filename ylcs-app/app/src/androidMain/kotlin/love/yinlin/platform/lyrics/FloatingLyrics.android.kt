package love.yinlin.platform.lyrics

import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import love.yinlin.AndroidContext
import love.yinlin.Context
import love.yinlin.app
import love.yinlin.extension.catching
import love.yinlin.uri.Scheme
import love.yinlin.uri.Uri
import love.yinlin.uri.toAndroidUri
import java.util.UUID

@Stable
actual class FloatingLyrics {
    actual var isAttached: Boolean by mutableStateOf(false)
        private set

    private lateinit var activity: ComponentActivity
    private var view: ComposeView? = null

    fun applyPermission(onResult: (Boolean) -> Unit) = catching {
        activity.activityResultRegistry.register(
            key = UUID.randomUUID().toString(),
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            onResult(canAttach(activity))
        }.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            setData(Uri(scheme = Scheme.Package, host = activity.packageName).toAndroidUri())
        })
    }

    fun canAttach(context: AndroidContext): Boolean = Settings.canDrawOverlays(context)

    actual fun attach() {
        if (canAttach(activity)) {
            val manager = activity.getSystemService(AndroidContext.WINDOW_SERVICE) as? WindowManager

            if (manager != null) {
                view = ComposeView(activity).apply {
                    setViewTreeLifecycleOwner(activity)
                    setViewTreeSavedStateRegistryOwner(activity)
                    setViewTreeViewModelStoreOwner(activity)
                    addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) { isAttached = true }
                        override fun onViewDetachedFromWindow(v: View) { isAttached = false }
                    })
                    setContent {
                        if (isAttached) this@FloatingLyrics.Content()
                    }
                }

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

                manager.addView(view, params)
            }
        }
        else {
            applyPermission { result ->
                if (result) attach()
                else app.config.enabledFloatingLyrics = false
            }
        }
    }

    actual fun detach() {
        val manager = activity.getSystemService(AndroidContext.WINDOW_SERVICE) as? WindowManager
        manager?.removeViewImmediate(view)
        view = null
    }

    actual suspend fun initDelay(context: Context) {
        activity = context.activity
        if (app.config.enabledFloatingLyrics && !isAttached) attach()
    }

    @Composable
    actual fun Content() {
        app.Layout(modifier = Modifier.fillMaxWidth()) {
            with(app.mp.engine) {
                Content(config = app.config.lyricsEngineConfig)
            }
        }
    }
}