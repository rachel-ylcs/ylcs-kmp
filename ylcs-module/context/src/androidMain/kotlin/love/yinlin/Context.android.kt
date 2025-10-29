package love.yinlin

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry

actual class Context actual constructor(delegate: PlatformContextDelegate) {
    val application: Application = delegate
    lateinit var activity: ComponentActivity
        private set
    lateinit var activityResultRegistry: ActivityResultRegistry
        private set

    fun bindActivity(activity: ComponentActivity) {
        this.activity = activity
        this.activityResultRegistry = activity.activityResultRegistry
    }
}