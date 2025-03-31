package love.yinlin.platform

import kotlinx.browser.document
import kotlinx.browser.window
import love.yinlin.ui.component.screen.DialogProgressState
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get

actual val osPlatform: Platform = Platform.WebWasm

actual fun osNetOpenUrl(url: String) {
	window.open(url, "_blank")
}

actual suspend fun osNetDownloadImage(url: String, state: DialogProgressState) = osNetOpenUrl(url)

@OptIn(ExperimentalUnsignedTypes::class)
actual fun test(block: (ByteArray) -> Unit) {
	val input = document.createElement("input") as HTMLInputElement
	input.type = "file"
	input.onchange = {
		input.files?.get(0)?.let {
			val u = FileReader()
			u.onloadend = {
				(u.result as? ArrayBuffer)?.let { buffer ->
					block(Int8Array(buffer).toByteArray())
				}
			}
			u.readAsArrayBuffer(it)
		}
	}
	input.click()
}