package love.yinlin.compose.ui

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.LifecycleOwner

class PlatformViewEnvironment(
    val context: Context,
    val lifecycleOwner: LifecycleOwner,
    val activityResultRegistry: ActivityResultRegistry?,
)
