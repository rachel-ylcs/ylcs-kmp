package love.yinlin.foundation

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultRegistry

actual class Context actual constructor(delegate: PlatformContextDelegate) {
    // 在 init 后可用
    val application: Application = delegate
    // 在 initDelay 后可用
    lateinit var activity: ComponentActivity
        private set
    // 在 initDelay 后可用
    lateinit var activityResultRegistry: ActivityResultRegistry
        private set

    fun bindActivity(activity: ComponentActivity) {
        this.activity = activity
        this.activityResultRegistry = activity.activityResultRegistry
    }
}

typealias AndroidContext = android.content.Context