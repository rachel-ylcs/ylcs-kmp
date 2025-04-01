@file:JvmName("AppContextAndroid")
package love.yinlin.platform

import android.content.Context
import androidx.activity.result.ActivityResultRegistry
import love.yinlin.extension.DateEx

class ActualAppContext(val context: Context) : AppContext() {
	override val screenWidth: Int = context.resources.displayMetrics.widthPixels
	override val screenHeight: Int = context.resources.displayMetrics.heightPixels
	override val fontScale: Float = 1f
	override val kv: KV = KV(context)

	var activityResultRegistry: ActivityResultRegistry? = null

	override fun initialize(): ActualAppContext {
		super.initialize()
		// 注册异常回调
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${e.stackTraceToString()}")
		}
		return this
	}
}

val appNative: ActualAppContext get() = app as ActualAppContext