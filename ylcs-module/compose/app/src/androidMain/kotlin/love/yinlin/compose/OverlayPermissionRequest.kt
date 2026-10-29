package love.yinlin.compose

import android.content.Intent
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import love.yinlin.extension.catchingError
import love.yinlin.extension.cleaning
import kotlin.uuid.Uuid

/** 一次性的权限请求；关闭窗口或销毁 Activity 后不再接收结果，也不保留 Activity。 */
internal class OverlayPermissionRequest(activity: ComponentActivity, onResult: (Boolean?) -> Unit) : LifecycleEventObserver {
    private var activity: ComponentActivity? = activity
    private var onResult: ((Boolean?) -> Unit)? = onResult
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var launched = false

    fun launch() {
        if (launcher != null) return
        val activity = activity ?: return
        if (activity.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            finish(null)
            return
        }
        catchingError {
            launcher = activity.activityResultRegistry.register(
                key = Uuid.generateV7().toString(),
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { finish(Settings.canDrawOverlays(activity)) }
            activity.lifecycle.addObserver(this)
        }?.let { finish(false) }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                val activity = activity ?: return
                if (!launched) {
                    if (Settings.canDrawOverlays(activity)) finish(true)
                    else {
                        launched = true
                        catchingError {
                            launcher?.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${activity.packageName}".toUri()))
                        }?.let { finish(false) }
                    }
                }
            }
            Lifecycle.Event.ON_DESTROY -> finish(null)
            else -> Unit
        }
    }

    private fun finish(granted: Boolean?) {
        val callback = onResult
        close()
        callback?.invoke(granted)
    }

    fun close() {
        val activity = activity
        val launcher = launcher
        this.activity = null
        this.launcher = null
        onResult = null
        cleaning({ activity?.lifecycle?.removeObserver(this) }) { launcher?.unregister() }
    }
}
