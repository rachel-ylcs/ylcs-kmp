package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.engine.js.*

object WasmClient {
	private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
		engine {

		}
	}

	val common: HttpClient = HttpClient(Js) {
		useEngine()
		useJson()
		useCommonTimeout()
	}

	val file: HttpClient = HttpClient(Js) {
		useEngine()
		useFileTimeout()
	}
}