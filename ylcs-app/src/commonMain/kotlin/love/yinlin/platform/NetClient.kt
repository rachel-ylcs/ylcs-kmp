package love.yinlin.platform

import androidx.compose.runtime.Stable
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.io.Sink
import love.yinlin.data.Data
import love.yinlin.data.MimeType
import love.yinlin.extension.Json
import kotlin.coroutines.cancellation.CancellationException

expect object NetClient {
	@Stable
	val common: HttpClient
	@Stable
	val file: HttpClient
	@Stable
	val sockets: HttpClient
}

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useJson() {
	defaultRequest {
		contentType(ContentType.Application.Json)
	}

	install(ContentNegotiation) {
		json(Json)
	}
}

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useWebSockets() {
	install(WebSockets) {
		contentConverter = KotlinxWebsocketSerializationConverter(Json)
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

suspend inline fun <R> HttpClient.safeCallData(
	crossinline block: suspend (HttpClient) -> Data<R>
): Data<R> = try {
	block(this)
}
catch (e: HttpRequestTimeoutException) {
	Data.Failure(love.yinlin.data.RequestError.Timeout, "网络连接超时", e)
}
catch (e: CancellationException) {
	Data.Failure(love.yinlin.data.RequestError.Canceled, "操作取消", e)
}
catch (e: Throwable) {
	Data.Failure(love.yinlin.data.RequestError.ClientError, "未知异常", e)
}

suspend inline fun <R> HttpClient.safeCall(
	crossinline block: suspend (HttpClient) -> R
): Data<R> = this.safeCallData { Data.Success(block(this)) }

suspend inline fun <reified T, R> HttpClient.safeGet(
	url: String,
	crossinline headers: HeadersBuilder.() -> Unit = {},
	crossinline block: suspend (T) -> R
): Data<R> = this.safeCall { client ->
	client.prepareGet(url) {
		this.headers {
			append(HttpHeaders.ContentType, ContentType.Text.Plain)
			append(HttpHeaders.Accept, MimeType.ANY)
			headers()
		}
	}.execute { response ->
		val data = response.body<T>()
		Coroutines.main { block(data) }
	}
}

suspend inline fun <reified U, reified T, R> HttpClient.safePost(
	url: String,
	data: U,
	crossinline headers: HeadersBuilder.() -> Unit = {},
	crossinline block: suspend (T) -> R
): Data<R> = this.safeCall { client ->
	client.preparePost(url) {
		this.headers {
			append(HttpHeaders.Accept, MimeType.ANY)
			headers()
		}
		setBody(data)
	}.execute { response ->
		val data = response.body<T>()
		Coroutines.main { block(data) }
	}
}

const val TRANSFER_BUFFER_SIZE = 1024 * 64L

suspend inline fun HttpClient.safeDownload(
	url: String,
	sink: Sink,
	crossinline headers: HeadersBuilder.() -> Unit = {},
	crossinline isCancel: suspend () -> Boolean,
	crossinline onGetSize: suspend (Long) -> Unit,
	crossinline onTick: suspend (Long, Long) -> Unit,
): Boolean = (this.safeCall { client ->
	var downloadedBytes = 0L
	var totalBytes = 0L
	client.prepareGet(url) {
		this.headers {
			append(HttpHeaders.Accept, MimeType.ANY)
			headers()
		}
		onDownload { current, total ->
			if (isCancel()) throw CancellationException()
			if (current - downloadedBytes > TRANSFER_BUFFER_SIZE) {
				downloadedBytes = current
				onTick(downloadedBytes, totalBytes)
			}
			if (totalBytes != total && total != null) {
				totalBytes = total
				onGetSize(totalBytes)
			}
		}
	}.execute { response ->
		response.bodyAsChannel().copyAndClose(sink.asByteWriteChannel()) > 0L
	}
} as? Data.Success)?.data == true