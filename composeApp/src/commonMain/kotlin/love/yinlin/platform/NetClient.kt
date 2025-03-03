package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import love.yinlin.IOThread
import love.yinlin.MainThread
import love.yinlin.data.Data
import love.yinlin.data.RequestError
import love.yinlin.extension.Json
import kotlin.coroutines.cancellation.CancellationException

expect object NetClient {
	val common: HttpClient
	val file: HttpClient
}

fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useJson() {
	defaultRequest {
		contentType(ContentType.Application.Json)
	}

	install(ContentNegotiation) {
		json(Json)
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
	Data.Error(RequestError.Timeout, "网络连接超时", e)
}
catch (e: CancellationException) {
	Data.Error(RequestError.Canceled, "操作取消", e)
}
catch (e: Throwable) {
	Data.Error(RequestError.ClientError, "未知异常", e)
}

suspend inline fun <R> HttpClient.safeCall(
	crossinline block: suspend (HttpClient) -> R
): Data<R> = this.safeCallData { Data.Success(block(this)) }

suspend inline fun <reified T, R> HttpClient.safeGet(
	url: String,
	crossinline block: @MainThread suspend (T) -> R
): Data<R> = this.safeCall { client ->
	client.prepareGet(url).execute { response ->
		val data = response.body<T>()
		Coroutines.main { block(data) }
	}
}

suspend inline fun <reified U, reified T, R> HttpClient.safePost(
	url: String,
	data: U,
	crossinline block: @MainThread suspend (T) -> R
): Data<R> = this.safeCall { client ->
	client.preparePost(url) { setBody(data) }.execute { response ->
		val data = response.body<T>()
		Coroutines.main { block(data) }
	}
}

const val TRANSFER_BUFFER_SIZE = 1024 * 64L

suspend inline fun <T> HttpClient.safeDownload(
	url: String,
	crossinline isCancel: @MainThread suspend () -> Boolean,
	crossinline onStart: @MainThread suspend (Long) -> Unit,
	crossinline onTick: @MainThread suspend (Long, Long) -> Unit,
	crossinline onWriteBegin: @IOThread suspend () -> T?,
	crossinline onWriteChannel: @IOThread suspend (T) -> ByteWriteChannel,
	crossinline onWriteEnd: @IOThread suspend (T) -> Unit,
) {
	this.safeCall { client ->
		var downloadedBytes = 0L
		var totalBytes = 0L
		client.prepareGet(url) {
			onDownload { current, total ->
				if (isCancel()) throw CancellationException()
				if (current - downloadedBytes > TRANSFER_BUFFER_SIZE) {
					downloadedBytes = current
					onTick(downloadedBytes, totalBytes)
				}
				if (totalBytes != total && total != null) {
					totalBytes = total
					onStart(totalBytes)
				}
			}
		}.execute { response ->
			val sink = onWriteBegin()
			if (sink != null) {
				try {
					response.bodyAsChannel().copyAndClose(onWriteChannel(sink))
				}
				finally {
					onWriteEnd(sink)
				}
			}
		}
	}
}

suspend inline fun HttpClient.safeDownload(
	url: String,
	file: Path,
	crossinline isCancel: @MainThread suspend () -> Boolean,
	crossinline onStart: @MainThread suspend (Long) -> Unit,
	crossinline onTick: @MainThread suspend (Long, Long) -> Unit
) = this.safeDownload(
	url = url,
	isCancel = isCancel,
	onStart = onStart,
	onTick = onTick,
	onWriteBegin = { SystemFileSystem.sink(file) },
	onWriteChannel = { it.asByteWriteChannel() },
	onWriteEnd = { it.close() }
)