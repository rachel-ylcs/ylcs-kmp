package love.yinlin.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.request.contentType
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveNullable
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import io.ktor.util.flattenForEach
import love.yinlin.extension.Json
import love.yinlin.extension.catchingError
import love.yinlin.platform.Coroutines
import java.net.Proxy

class Proxy(
    private val name: String,
    private val whitelist: List<Regex>
) {
    private val client = HttpClient(OkHttp) {
        engine {
            proxy = Proxy.NO_PROXY
            config {
                followRedirects(true)
                followSslRedirects(true)
            }
        }

        defaultRequest {
            contentType(ContentType.Application.Json)
        }

        install(ContentNegotiation) {
            json(Json)
        }

        install(HttpTimeout) {
            connectTimeoutMillis = 5000L
            requestTimeoutMillis = 10000L
        }
    }

    fun Routing.listen() {
        route(path = "/$name") {
            handle {
                catchingError {
                    Coroutines.io {
                        val request = call.request
                        val dest = request.headers["Proxy"]!!

                        require(whitelist.any { it.matches(dest) })

                        client.prepareRequest(urlString = dest) {
                            method = request.httpMethod

                            headers {
                                request.headers.forEach { key, value ->
                                    if (!key.equals(HttpHeaders.ContentLength, ignoreCase = true)) {
                                        appendAll(key, value)
                                    }
                                }
                            }

                            val contentType = request.contentType()
                            when {
                                contentType.contentType == ContentType.Text.TYPE -> setBody(call.receiveText())
                                contentType == ContentType.Application.FormUrlEncoded -> setBody(FormDataContent(call.receiveParameters()))
                                else -> setBody(call.receiveNullable<ByteArray>())
                            }
                        }.execute {
                            it.headers.flattenForEach { key, value ->
                                if (!key.equals(HttpHeaders.ContentLength, ignoreCase = true)) {
                                    call.response.headers.append(key, value)
                                }
                            }
                            call.respondBytes(bytes = it.bodyAsBytes(), status = HttpStatusCode.OK)
                        }
                    }
                }?.let {
                    call.respond(status = HttpStatusCode.Forbidden, message = it.message ?: "")
                }
            }
        }
    }
}