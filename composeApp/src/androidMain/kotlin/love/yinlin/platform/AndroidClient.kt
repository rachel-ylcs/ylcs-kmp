package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object AndroidClient {
	val common: HttpClient = HttpClient(OkHttp) {
		defaultRequest {
			contentType(ContentType.Application.Json)
		}

		install(ContentNegotiation) {
			json(Json {
				ignoreUnknownKeys = true
				prettyPrint = false
			})
		}

		install(HttpTimeout) {
			connectTimeoutMillis = 500L
			requestTimeoutMillis = 1000L
		}
	}

	val file: HttpClient = HttpClient(OkHttp) {
		install(HttpTimeout) {
			connectTimeoutMillis = 500L
			requestTimeoutMillis = 18000L
		}
	}
}