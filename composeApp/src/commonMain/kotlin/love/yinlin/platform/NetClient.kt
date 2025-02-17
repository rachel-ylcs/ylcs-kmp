package love.yinlin.platform

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.core.remaining
import kotlinx.serialization.json.Json
import love.yinlin.data.Data
import love.yinlin.data.RequestError
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

const val TRANSFER_BUFFER_SIZE = 1024 * 64L

fun HttpResponse.bodyLength(): Long = this.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L

@OptIn(InternalAPI::class)
suspend fun transfer(
	readChannel: ByteReadChannel,
	writeChannel: ByteWriteChannel,
	isCancel: () -> Boolean,
	onTick: suspend (Long) -> Unit
) {
	var count = 0L
	while (!readChannel.isClosedForRead) {
		if (isCancel()) throw CancellationException()
		readChannel.awaitContent(TRANSFER_BUFFER_SIZE.toInt())
		val readCount = minOf(readChannel.readBuffer.remaining, TRANSFER_BUFFER_SIZE)
		readChannel.readBuffer.readTo(writeChannel.writeBuffer, readCount)
		count += readCount
		onTick(count)
	}
}