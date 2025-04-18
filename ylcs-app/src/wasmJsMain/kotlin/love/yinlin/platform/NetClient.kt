package love.yinlin.platform

import androidx.compose.runtime.Stable
import io.ktor.client.*
import io.ktor.client.engine.js.*

actual object NetClient {
	private fun HttpClientConfig<JsClientEngineConfig>.useEngine() {
		engine {

		}
	}

	@Stable
	actual val common: HttpClient = HttpClient(Js) {
		useEngine()
		useJson()
		useCommonTimeout()
	}

	@Stable
	actual val file: HttpClient = HttpClient(Js) {
		useEngine()
		useJson()
		useFileTimeout()
	}
}