package love.yinlin

import io.ktor.client.HttpClient
import kotlinx.browser.window
import love.yinlin.platform.KV
import love.yinlin.platform.WasmClient

actual val platform: Platform = Platform.WebWasm

class WasmContext : AppContext() {
	override val screenWidth: Int = window.innerWidth
	override val screenHeight: Int = window.innerHeight
	override val fontScale: Float = 1f
	override val kv: KV = KV()
	override val client: HttpClient = WasmClient.common
	override val fileClient: HttpClient = WasmClient.file
}