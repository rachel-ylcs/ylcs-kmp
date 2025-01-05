package love.yinlin

import android.content.Context
import io.ktor.client.HttpClient
import love.yinlin.platform.AndroidClient
import love.yinlin.platform.KV

actual val platform: Platform = Platform.Android

class AndroidContext(private val context: Context) : AppContext() {
	override val screenWidth: Int = context.resources.displayMetrics.widthPixels
	override val screenHeight: Int = context.resources.displayMetrics.heightPixels
	override val fontScale: Float = 1f
	override val kv: KV = KV(context)
	override val client: HttpClient = AndroidClient.common
	override val fileClient: HttpClient = AndroidClient.file
}