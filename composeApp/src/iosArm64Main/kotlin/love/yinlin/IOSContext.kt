package love.yinlin

import io.ktor.client.HttpClient
import kotlinx.cinterop.ExperimentalForeignApi
import love.yinlin.platform.IOSClient
import love.yinlin.platform.KV
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.UIKit.UIScreen

actual val platform: Platform = Platform.IOS

@OptIn(ExperimentalForeignApi::class)
class IOSContext : AppContext() {
	private val screenBounds = UIScreen.mainScreen.bounds

	override val screenWidth: Int = CGRectGetWidth(screenBounds).toInt()
	override val screenHeight: Int = CGRectGetHeight(screenBounds).toInt()
	override val fontScale: Float = 1f
	override val kv: KV = KV()
	override val client: HttpClient = IOSClient.common
	override val fileClient: HttpClient = IOSClient.file
}