package love.yinlin.platform

import android.content.Context
import love.yinlin.extension.DateEx

class AppContext(val context: Context) : AppContextBase() {
	override val screenWidth: Int = context.resources.displayMetrics.widthPixels
	override val screenHeight: Int = context.resources.displayMetrics.heightPixels
	override val fontScale: Float = 1f
	override val kv: KV = KV(context)

	override fun initialize(): AppContext {
		super.initialize()
		// 注册异常回调
		Thread.setDefaultUncaughtExceptionHandler { _, e ->
			kv.set(CRASH_KEY, "${DateEx.CurrentString}\n${e.stackTraceToString()}")
		}
		return this
	}
}

val appNative: AppContext get() = app as AppContext