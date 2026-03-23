package love.yinlin.cs

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.route
import love.yinlin.extension.Json
import love.yinlin.platform.UnsupportedPlatformText
import java.net.Proxy

class Proxy(
    private val name: String = DEFAULT_NAME,
    private val whitelist: List<Regex> = emptyList()
) {
    companion object {
        const val DEFAULT_NAME = "proxy"
    }

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
//                catchingError {
//                    Coroutines.io {
//                        val request = call.request
//                        val rawParameters = request.queryParameters.toMap().mapValues { (_, v) -> v.first() }.toMutableMap()
//                        val dest = Uri.decodeUri(rawParameters[name]!!)
//                        rawParameters.remove(name)
//                        require(whitelist.any { it.matches(dest) })
//
//                        client.prepareRequest(urlString = dest) {
//                            method = request.httpMethod
//
//                            val headersMap = mutableMapOf<String, String>()
//                            headers {
//                                var vHeaders = mapOf<String, String>()
//                                request.headers.flattenForEach { key, value ->
//                                    when {
//                                        key.equals(HttpHeaders.ContentLength, ignoreCase = true) -> { }
//                                        key.equals(HttpHeaders.Host, ignoreCase = true) -> { }
//                                        key.equals(HttpHeaders.Origin, ignoreCase = true) -> { }
//                                        key.equals(HttpHeaders.Referrer, ignoreCase = true) -> { }
//                                        key.equals(HttpHeaders.AcceptEncoding, ignoreCase = true) -> { }
//                                        key.equals("VHeaders", ignoreCase = true) -> vHeaders = Base64.decode(value).decodeToString().parseJsonValue<Map<String, String>>()
//                                        else -> headersMap[key] = value
//                                    }
//                                }
//                                for ((vKey, vValue) in vHeaders) {
//                                    headersMap.remove(vKey)
//                                    headersMap.remove(vKey.lowercase())
//                                    headersMap[vKey] = vValue
//                                }
//                                for ((hKey, hValue) in rawParameters) headersMap[hKey] = Uri.decodeUri(hValue)
//                                appendAll(headersMap)
//                            }
//
//                            val contentType = request.contentType()
//                            when {
//                                contentType.contentType == ContentType.Text.TYPE -> setBody(call.receiveText())
//                                contentType.match(ContentType.Application.FormUrlEncoded) -> setBody(FormDataContent(call.receiveParameters()))
//                                else -> setBody(call.receiveNullable<ByteArray>())
//                            }
//                        }.execute { response ->
//                            response.headers.flattenForEach { key, value ->
//                                when {
//                                    // 跳过Content-Length 和 SetCookie
//                                    key.equals(HttpHeaders.ContentLength, ignoreCase = true) -> { }
//                                    key.equals(HttpHeaders.SetCookie, ignoreCase = true) -> { }
//                                    else -> call.response.headers.append(key, value)
//                                }
//                            }
//                            // 修改cookie安全限制
//                            val rawBytes = response.bodyAsBytes()
//                            val rawCookies = mutableMapOf<String, String>()
//                            for (cookie in response.setCookie()) rawCookies[cookie.name] = cookie.value
//                            if (rawCookies.isNotEmpty()) {
//                                val cookiesJson = rawCookies.toJsonString().encodeToByteArray()
//                                val cookiesLength = cookiesJson.size
//                                val actualBytes = cookiesLength.toString().padStart(8, '0').encodeToByteArray() + cookiesJson + rawBytes
//                                call.respondBytes(bytes = actualBytes, status = HttpStatusCode.Accepted)
//                            }
//                            else call.respondBytes(bytes = rawBytes, status = HttpStatusCode.OK)
//                        }
//                    }
//                }?.let {
//                    call.respond(status = HttpStatusCode.Forbidden, message = it.message ?: "")
//                }
                call.respond(status = HttpStatusCode.Forbidden, message = UnsupportedPlatformText)
            }
        }
    }
}