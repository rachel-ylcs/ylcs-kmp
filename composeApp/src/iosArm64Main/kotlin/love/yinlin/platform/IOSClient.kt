package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

object IOSClient {
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

	val common: HttpClient = HttpClient(Darwin) {
		useEngine()
		useJson()
		useCommonTimeout()
	}

	val file: HttpClient = HttpClient(Darwin) {
		useEngine()
		useFileTimeout()
	}
}