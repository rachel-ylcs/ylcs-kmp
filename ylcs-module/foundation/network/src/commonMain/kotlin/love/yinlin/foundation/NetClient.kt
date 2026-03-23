package love.yinlin.foundation

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.*
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.io.Sink
import kotlinx.serialization.json.JsonObject
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.extension.Json
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmName

class NetClient internal constructor(val delegate: HttpClient) {
    companion object {
        val Common by lazy { buildCommonNetClient() }
        val File by lazy { buildFileClient() }
    }

    suspend fun internalPrepareStatement(method: HttpMethod, url: String, block: HttpRequestBuilder.() -> Unit): HttpStatement = delegate.prepareRequest(urlString = url) {
        this.method = method
        block()
    }

    @IOCoroutine
    suspend inline fun <reified Body : Any, reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend ResponseScope<Body>.() -> Output
    ): Output? = catchingNull {
        val context = currentCoroutineContext()
        Coroutines.io {
            delegate.prepareRequest {
                val scope = RequestScope()
                scope.onRequest()
                method = scope.method
                url.takeFrom(scope.url)
                scope.data?.let(::setBody)
                scope.form?.let { form ->
                    setBody(FormDataContent(parameters {
                        for ((k, v) in form) append(k, v)
                    }))
                }
                scope.buildHeaders(headers)
            }.execute { response ->
                val headers = response.headers
                val cookies = response.setCookie()
                val rawBody = response.bodyAsBytes()
                val scope = object : ResponseScope<Body> {
                    override val headers: Headers = headers
                    override val cookies: List<Cookie> = cookies
                    override val rawBody: ByteArray = rawBody
                    override val bodyString: String get() = this.rawBody.decodeToString()
                    override val body: Body get() = this.bodyString.parseJsonValue()
                }
                Coroutines.with(context) { scope.onResponse() }
            }
        }
    }

    @JvmName("requestForString")
    @IOCoroutine
    suspend inline fun <reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend (String) -> Output
    ): Output? = request<String, Output>(onRequest) { onResponse(bodyString) }

    @JvmName("requestForByteArray")
    @IOCoroutine
    suspend inline fun <reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend (ByteArray) -> Output
    ): Output? = request<ByteArray, Output>(onRequest) { onResponse(rawBody) }

    @JvmName("requestForJsonObject")
    @IOCoroutine
    suspend inline fun <reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend (JsonObject) -> Output
    ): Output? = request<JsonObject, Output>(onRequest) { onResponse(body) }

    @IOCoroutine
    suspend inline fun download(
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
            internalPrepareStatement(HttpMethod.Get, url) {
                this.headers {
                    append(HttpHeaders.ContentType, ContentType.Any.toString())
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

    @IOCoroutine
    suspend inline fun download(url: String, sink: Sink): Boolean = catchingDefault(false) {
        Coroutines.io {
            internalPrepareStatement(HttpMethod.Get, url) {
                this.headers {
                    append(HttpHeaders.ContentType, ContentType.Any.toString())
                }
            }.execute { response ->
                response.bodyAsChannel().copyAndClose(sink.asByteWriteChannel()) > 0L
            }
        }
    }

    @IOCoroutine
    suspend inline fun download(url: String): ByteArray? = catchingNull {
        Coroutines.io {
            internalPrepareStatement(HttpMethod.Get, url) {
                this.headers {
                    append(HttpHeaders.ContentType, ContentType.Any.toString())
                }
            }.execute { response ->
                response.bodyAsBytes()
            }
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

internal fun <T : HttpClientEngineConfig> HttpClientConfig<T>.useTimeout(milliseconds: Long) {
    install(HttpTimeout) {
        connectTimeoutMillis = 5000L
        requestTimeoutMillis = milliseconds
    }
}

expect fun buildCommonNetClient(timeout: Long = 10000L): NetClient
expect fun buildFileClient(timeout: Long = 180000L): NetClient
expect fun buildSocketClient(): WebSocketClient