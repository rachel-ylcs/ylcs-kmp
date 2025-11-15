package love.yinlin.platform

import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.io.Sink
import love.yinlin.extension.Json
import love.yinlin.extension.catchingDefault
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmName

expect object NetClient {
    suspend inline fun <reified Body : Any, reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend ResponseScope<Body>.() -> Output
    ): Output?
    suspend fun internalPrepareStatement(method: HttpMethod, uploadFile: Boolean, url: String, block: HttpRequestBuilder.() -> Unit): HttpStatement
    suspend fun internalWebSocketSession(block: HttpRequestBuilder.() -> Unit): ClientWebSocketSession
}

@JvmName("requestWithBody")
suspend inline fun <reified Body : Any, reified Output : Any> NetClient.request(
    url: String,
    crossinline onRequest: RequestScope.() -> Unit = {},
    crossinline onResponse: suspend (Body) -> Output
): Output? = request<Body, Output>(onRequest = {
    this.url = url
    onRequest()
}) {
    onResponse(body)
}

@JvmName("requestWithString")
suspend inline fun <reified Output : Any> NetClient.request(
    url: String,
    crossinline onRequest: RequestScope.() -> Unit = {},
    crossinline onResponse: suspend (String) -> Output
): Output? = request<String, Output>(onRequest = {
    this.url = url
    onRequest()
}) {
    onResponse(bodyString)
}

@JvmName("requestWithByteArray")
suspend inline fun <reified Output : Any> NetClient.request(
    url: String,
    crossinline onRequest: RequestScope.() -> Unit = {},
    crossinline onResponse: suspend (ByteArray) -> Output
): Output? = request<ByteArray, Output>(onRequest = {
    this.url = url
    onRequest()
}) {
    onResponse(bodyBytes)
}

suspend inline fun NetClient.download(
    url: String,
    sink: Sink,
    crossinline headers: HeadersBuilder.() -> Unit = {},
    crossinline isCancel: suspend () -> Boolean,
    crossinline onGetSize: suspend (Long) -> Unit,
    crossinline onTick: suspend (Long, Long) -> Unit,
): Boolean = catchingDefault(false) {
    var downloadedBytes = 0L
    var totalBytes = 0L
    Coroutines.io {
        internalPrepareStatement(HttpMethod.Get, true, url) {
            this.headers {
                accept(ContentType.Any)
                headers()
            }
            onDownload { current, total ->
                if (isCancel()) throw CancellationException()
                if (current - downloadedBytes > 1024 * 64L) {
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
    }
}

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useJson() {
    defaultRequest {
        contentType(ContentType.Application.Json)
    }

    install(ContentNegotiation) {
        json(Json)
    }
}

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useWebSockets() {
    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
}

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useCommonTimeout() {
    install(HttpTimeout) {
        connectTimeoutMillis = 5000L
        requestTimeoutMillis = 10000L
    }
}

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useFileTimeout() {
    install(HttpTimeout) {
        connectTimeoutMillis = 5000L
        requestTimeoutMillis = 180000L
    }
}