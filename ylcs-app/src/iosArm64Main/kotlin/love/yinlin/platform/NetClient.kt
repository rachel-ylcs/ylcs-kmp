package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual object NetClient {
	private fun HttpClientConfig<DarwinClientEngineConfig>.useEngine() {
		engine {
			configureRequest {
				setAllowsCellularAccess(true)
			}
			configureSession {
				setAllowsCellularAccess(true)
			}
		}
	}

	actual val common: HttpClient = HttpClient(Darwin) {
		useEngine()
		useJson()
		useCommonTimeout()
	}

	actual val file: HttpClient = HttpClient(Darwin) {
		useEngine()
		useJson()
		useFileTimeout()
	}
}