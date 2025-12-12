package love.yinlin.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.setCookie
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
import io.ktor.util.appendAll
import io.ktor.util.decodeBase64String
import io.ktor.util.flattenForEach
import io.ktor.util.toMap
import love.yinlin.extension.Json
import love.yinlin.extension.catchingError
import love.yinlin.extension.parseJsonValue
import love.yinlin.extension.toJsonString
import love.yinlin.platform.Coroutines
import love.yinlin.uri.Uri
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
                        val rawParameters = request.queryParameters.toMap().mapValues { (_, v) -> v.first() }.toMutableMap()
                        val dest = Uri.decodeUri(rawParameters[name]!!)
                        rawParameters.remove(name)
                        require(whitelist.any { it.matches(dest) })

                        client.prepareRequest(urlString = dest) {
                            method = request.httpMethod

                            val headersMap = mutableMapOf<String, String>()
                            headers {
                                var vHeaders = mapOf<String, String>()
                                request.headers.flattenForEach { key, value ->
                                    when {
                                        key.equals(HttpHeaders.ContentLength, ignoreCase = true) -> { }
                                        key.equals(HttpHeaders.Host, ignoreCase = true) -> { }
                                        key.equals(HttpHeaders.Origin, ignoreCase = true) -> { }
                                        key.equals(HttpHeaders.Referrer, ignoreCase = true) -> { }
                                        key.equals(HttpHeaders.AcceptEncoding, ignoreCase = true) -> { }
                                        key.equals("VHeaders", ignoreCase = true) -> vHeaders = value.decodeBase64String().parseJsonValue<Map<String, String>>()
                                        else -> headersMap[key] = value
                                    }
                                }
                                for ((vKey, vValue) in vHeaders) {
                                    headersMap.remove(vKey)
                                    headersMap.remove(vKey.lowercase())
                                    headersMap[vKey] = vValue
                                }
                                for ((hKey, hValue) in rawParameters) headersMap[hKey] = Uri.decodeUri(hValue)
                                appendAll(headersMap)
                            }

                            val contentType = request.contentType()
                            when {
                                contentType.contentType == ContentType.Text.TYPE -> setBody(call.receiveText())
                                contentType.match(ContentType.Application.FormUrlEncoded) -> setBody(FormDataContent(call.receiveParameters()))
                                else -> setBody(call.receiveNullable<ByteArray>())
                            }
                        }.execute { response ->
                            response.headers.flattenForEach { key, value ->
                                when {
                                    // 跳过Content-Length 和 SetCookie
                                    key.equals(HttpHeaders.ContentLength, ignoreCase = true) -> { }
                                    key.equals(HttpHeaders.SetCookie, ignoreCase = true) -> { }
                                    else -> call.response.headers.append(key, value)
                                }
                            }
                            // 修改cookie安全限制
                            val rawBytes = response.bodyAsBytes()
                            val rawCookies = mutableMapOf<String, String>()
                            for (cookie in response.setCookie()) rawCookies[cookie.name] = cookie.value
                            if (rawCookies.isNotEmpty()) {
                                val cookiesJson = rawCookies.toJsonString().encodeToByteArray()
                                val cookiesLength = cookiesJson.size
                                val actualBytes = cookiesLength.toString().padStart(8, '0').encodeToByteArray() + cookiesJson + rawBytes
                                call.respondBytes(bytes = actualBytes, status = HttpStatusCode.Accepted)
                            }
                            else call.respondBytes(bytes = rawBytes, status = HttpStatusCode.OK)
                        }
                    }
                }?.let {
                    call.respond(status = HttpStatusCode.Forbidden, message = it.message ?: "")
                }
            }
        }
    }
}