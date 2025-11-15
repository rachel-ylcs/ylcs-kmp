package love.yinlin.platform

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.websocket.ClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.Cookie
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.setCookie
import kotlinx.coroutines.currentCoroutineContext
import love.yinlin.extension.catchingNull
import love.yinlin.extension.parseJsonValue
import java.net.Proxy

actual object NetClient {
    private fun HttpClientConfig<OkHttpConfig>.useEngine() {
        engine {
            proxy = Proxy.NO_PROXY
            config {
                followRedirects(true)
                followSslRedirects(true)
            }
        }
    }

    val common: HttpClient by lazy { HttpClient(OkHttp) {
        useEngine()
        useJson()
        useCommonTimeout()
    } }

    val file: HttpClient by lazy { HttpClient(OkHttp) {
        useEngine()
        useJson()
        useFileTimeout()
    } }

    val sockets: HttpClient by lazy { HttpClient(OkHttp) {
        useWebSockets()
    } }

    actual suspend inline fun <reified Body : Any, reified Output : Any> request(
        crossinline onRequest: RequestScope.() -> Unit,
        crossinline onResponse: suspend ResponseScope<Body>.() -> Output
    ): Output? = catchingNull {
        val context = currentCoroutineContext()
        Coroutines.io {
            common.prepareRequest {
                val builder = this
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

                    override fun headers(block: HeadersBuilder.() -> Unit) {
                        builder.headers.block()
                    }
                }
                headers {
                    append(HttpHeaders.ContentType, ContentType.Text.Plain.toString())
                    append(HttpHeaders.Accept, ContentType.Any.toString())
                }
                onRequest(requestScope)
            }.execute { response ->
                val data = response.bodyAsBytes()
                val responseScope = object : ResponseScope<Body> {
                    override val headers: Headers = response.headers
                    override val cookies: List<Cookie> = response.setCookie()
                    override val body: Body get() = data.decodeToString().parseJsonValue<Body>()
                    override val bodyString: String get() = data.decodeToString()
                    override val bodyBytes: ByteArray = data
                }
                Coroutines.with(context) {
                    onResponse(responseScope)
                }
            }
        }
    }

    actual suspend fun internalPrepareStatement(method: HttpMethod, uploadFile: Boolean, url: String, block: HttpRequestBuilder.() -> Unit): HttpStatement = (if (uploadFile) file else common).prepareRequest(urlString = url) {
        this.method = method
        block()
    }

    actual suspend fun internalWebSocketSession(block: HttpRequestBuilder.() -> Unit): ClientWebSocketSession = sockets.webSocketSession(block)
}