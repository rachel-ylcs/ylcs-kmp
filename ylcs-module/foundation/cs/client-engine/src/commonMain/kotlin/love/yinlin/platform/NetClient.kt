package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.onDownload
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.appendAll
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.io.Sink
import love.yinlin.extension.Json
import love.yinlin.extension.catchingDefault
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.JvmName

expect val internalCommon: HttpClient
expect val internalFile: HttpClient
expect val internalSockets: HttpClient

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

object NetClient {
    @OptIn(InternalAPI::class)
    suspend inline fun <reified Body : Any, reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend ResponseScope<Body>.() -> Output
    ): Output? = catchingNull {
        val context = currentCoroutineContext()
        Coroutines.io {
            internalCommon.prepareRequest {
                val builder = this
                var contentType = ContentType.Text.Plain
                val requestScope = object : RequestScope {
                    override var method: HttpMethod = HttpMethod.Get
                        set(value) {
                            field = value
                            builder.method = value
                        }

                    override var url: String = ""
                        set(value) {
                            field = value
                            builder.url(urlString = value)
                        }

                    override var data: ByteArray = byteArrayOf()
                        set(value) {
                            field = value
                            builder.setBody(value)
                        }

                    override var form: Map<String, String> = emptyMap()
                        set(value) {
                            field = value
                            builder.setBody(FormDataContent(parameters { appendAll(value) }))
                            contentType = ContentType.Application.FormUrlEncoded
                        }

                    override fun headers(block: HeadersBuilder.() -> Unit) {
                        builder.headers.block()
                    }
                }
                headers {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.Accept, ContentType.Any.toString())
                }
                onRequest(requestScope)
            }.execute { response ->
                // 代理
                val (headers, cookies, data) = if (response.status == HttpStatusCode.Accepted) {
                    val rawContent = response.bodyAsBytes()
                    val rawCookiesLength = rawContent.copyOfRange(0, 8).decodeToString().toInt()
                    val rawCookies = rawContent.copyOfRange(8, 8 + rawCookiesLength).decodeToString().parseJsonValue<Map<String, String>>()
                    val rawData = rawContent.copyOfRange(8 + rawCookiesLength, rawContent.size)
                    val actualCookies = rawCookies.map { (key, value) -> Cookie(key, value) }
                    Triple(response.headers, actualCookies, rawData)
                } else Triple(response.headers, response.setCookie(), response.bodyAsBytes())
                Coroutines.with(context) {
                    onResponse(object : ResponseScope<Body> {
                        override val headers: Headers = headers
                        override val cookies: List<Cookie> = cookies
                        override val body: Body get() = data.decodeToString().parseJsonValue<Body>()
                        override val bodyString: String get() = data.decodeToString()
                        override val bodyBytes: ByteArray = data
                    })
                }
            }
        }
    }

    suspend fun internalPrepareStatement(method: HttpMethod, uploadFile: Boolean, url: String, block: HttpRequestBuilder.() -> Unit): HttpStatement {
        val client = if (uploadFile) internalFile else internalCommon
        return client.prepareRequest(urlString = url) {
            this.method = method
            block()
        }
    }

    suspend fun internalWebSocketSession(block: HttpRequestBuilder.() -> Unit): ClientWebSocketSession = internalSockets.webSocketSession(block)

    @JvmName("requestWithBody")
    suspend inline fun <reified Body : Any, reified Output : Any> request(
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
    suspend inline fun <reified Output : Any> request(
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
    suspend inline fun <reified Output : Any> request(
        url: String,
        crossinline onRequest: RequestScope.() -> Unit = {},
        crossinline onResponse: suspend (ByteArray) -> Output
    ): Output? = request<ByteArray, Output>(onRequest = {
        this.url = url
        onRequest()
    }) {
        onResponse(bodyBytes)
    }

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

    suspend inline fun simpleDownload(url: String, sink: Sink): Boolean = catchingDefault(false) {
        Coroutines.io {
            internalPrepareStatement(HttpMethod.Get, true, url) {
                this.headers { accept(ContentType.Any) }
            }.execute { response ->
                response.bodyAsChannel().copyAndClose(sink.asByteWriteChannel()) > 0L
            }
        }
    }
}