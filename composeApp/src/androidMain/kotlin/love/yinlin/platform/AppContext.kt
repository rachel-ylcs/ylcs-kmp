package love.yinlin.platform

import android.content.Context

class AppContext(val context: Context) : IAppContext() {
	override val screenWidth: Int = context.resources.displayMetrics.widthPixels
	override val screenHeight: Int = context.resources.displayMetrics.heightPixels
	override val fontScale: Float = 1f
	override val kv: KV = KV(context)
}

val appNative: AppContext get() = app as AppContext