package love.yinlin

import android.app.Activity
import android.app.Application
import androidx.activity.result.ActivityResultRegistry

actual class AppContext actual constructor(val context: Application) {
    lateinit var activity: Activity
    lateinit var activityResultRegistry: ActivityResultRegistry

    fun bindActivity(activity: Activity, activityResultRegistry: ActivityResultRegistry) {
        this.activity = activity
        this.activityResultRegistry = activityResultRegistry
    }
}