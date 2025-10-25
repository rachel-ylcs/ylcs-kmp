package love.yinlin.platform

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultRegistry

class ActualAppContext(val context: Context) : AppContext() {
	override val kv: KV = KV(context)

    var activity: Activity? = null
	var activityResultRegistry: ActivityResultRegistry? = null

	override fun initializeMusicFactory(): MusicFactory = ActualMusicFactory(context)
}

val appNative: ActualAppContext get() = app as ActualAppContext