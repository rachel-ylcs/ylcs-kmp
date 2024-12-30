package love.yinlin.platform

import android.content.Context

class AndroidContext(context: Context) : AppContext() {
	override val screenWidth: Int = context.resources.displayMetrics.widthPixels
	override val screenHeight: Int = context.resources.displayMetrics.heightPixels
	override val fontScale: Float = 1f
}

actual val platform: Platform = Platform.Android