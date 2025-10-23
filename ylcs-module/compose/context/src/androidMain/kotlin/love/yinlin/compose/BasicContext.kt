package love.yinlin.compose

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultRegistry
import androidx.compose.runtime.Stable

@Stable
actual open class BasicContext actual constructor(val context: Application) {
    lateinit var activity: Activity
    lateinit var activityResultRegistry: ActivityResultRegistry

    fun bindActivity(activity: Activity, activityResultRegistry: ActivityResultRegistry) {
        this.activity = activity
        this.activityResultRegistry = activityResultRegistry
    }
}