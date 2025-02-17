package love.yinlin.platform

import android.content.Context
import androidx.compose.runtime.Stable

class AppContext(val context: Context) : IAppContext() {
	override val screenWidth: Int = context.resources.displayMetrics.widthPixels
	override val screenHeight: Int = context.resources.displayMetrics.heightPixels
	override val fontScale: Float = 1f
	override val kv: KV = KV(context)
}

@Stable
val appNative: AppContext get() = appContext as AppContext