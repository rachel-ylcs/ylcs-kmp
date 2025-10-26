package love.yinlin.startup

import android.app.Activity
import androidx.activity.result.ActivityResultRegistry
import love.yinlin.service.PlatformContext
import love.yinlin.service.PlatformPage
import love.yinlin.service.StartupArgs
import love.yinlin.service.SyncStartup

actual class StartupContext : SyncStartup {
    private lateinit var mContext: PlatformContext
    private lateinit var mPage: PlatformPage

    actual val platformContext: PlatformContext get() = mContext
    actual val platformPage: PlatformPage get() = mPage

    lateinit var activity: Activity
    lateinit var activityResultRegistry: ActivityResultRegistry

    actual override fun init(context: PlatformContext, args: StartupArgs) {
        mContext = context
    }

    fun bindActivity(activity: Activity, activityResultRegistry: ActivityResultRegistry) {
        this.mPage = activity
        this.activity = activity
        this.activityResultRegistry = activityResultRegistry
    }
}