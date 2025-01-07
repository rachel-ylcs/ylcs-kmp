package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.CancellationException
import kotlinx.serialization.json.Json
import love.yinlin.data.Data
import love.yinlin.data.RequestError

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useJson() {
	defaultRequest {
		contentType(ContentType.Application.Json)
	}

	install(ContentNegotiation) {
		json(Json {
			ignoreUnknownKeys = true
			prettyPrint = false
		})
	}
}

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useCommonTimeout() {
	install(HttpTimeout) {
		connectTimeoutMillis = 5000L
		requestTimeoutMillis = 10000L
	}
}

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useFileTimeout() {
	install(HttpTimeout) {
		connectTimeoutMillis = 5000L
		requestTimeoutMillis = 180000L
	}
}



inline fun <R> HttpClient.safeCall(block: (HttpClient) -> R): Data<R> = try {
	Data.Success(block(this))
}
catch (e: HttpRequestTimeoutException) {
	Data.Error(RequestError.Timeout, e)
}
catch (e: CancellationException) {
	Data.Error(RequestError.Canceled, e)
}
catch (e: Exception) {
	Data.Error(RequestError.ClientError, e)
}
