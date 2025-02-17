package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.engine.js.*

actual object NetClient {
	private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
		engine {

		}
	}

	actual val common: HttpClient = HttpClient(Js) {
		useEngine()
		useJson()
		useCommonTimeout()
	}

	actual val file: HttpClient = HttpClient(Js) {
		useEngine()
		useFileTimeout()
	}
}