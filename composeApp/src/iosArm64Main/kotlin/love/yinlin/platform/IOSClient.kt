package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json

object IOSClient {
	val common: HttpClient = HttpClient(Darwin) {
		defaultRequest {
			contentType(ContentType.Application.Json)
		}

		engine {
			configureRequest {
				setAllowsCellularAccess(true)
			}
			configureSession {
				setAllowsCellularAccess(true)
			}
		}

		install(ContentNegotiation) {
			json()
		}

		install(HttpTimeout) {
			connectTimeoutMillis = 500L
			socketTimeoutMillis = 1000L
		}
	}

	val file: HttpClient = HttpClient(Darwin) {
		engine {
			configureRequest {
				setAllowsCellularAccess(true)
			}
			configureSession {
				setAllowsCellularAccess(true)
			}
		}

		install(HttpTimeout) {
			connectTimeoutMillis = 500L
			socketTimeoutMillis = 18000L
		}
	}
}