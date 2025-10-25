package love.yinlin.startup

import android.app.Activity
import androidx.activity.result.ActivityResultRegistry
import love.yinlin.service.PlatformContext
import love.yinlin.service.SyncStartup

actual class StartupContext : SyncStartup {
    lateinit var mContext: PlatformContext
    lateinit var activity: Activity
    lateinit var activityResultRegistry: ActivityResultRegistry

    actual val platformContext: PlatformContext get() = mContext

    actual override fun init(context: PlatformContext, args: Array<Any?>) {
        mContext = context
    }

    fun bindActivity(activity: Activity, activityResultRegistry: ActivityResultRegistry) {
        this.activity = activity
        this.activityResultRegistry = activityResultRegistry
    }
}