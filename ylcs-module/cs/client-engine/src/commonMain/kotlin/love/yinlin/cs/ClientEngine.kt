package love.yinlin.cs

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.currentCoroutineContext
import love.yinlin.data.Data
import love.yinlin.coroutines.Coroutines
import love.yinlin.coroutines.IOCoroutine
import love.yinlin.foundation.WebSocketClient
import love.yinlin.foundation.buildCommonNetClient
import love.yinlin.foundation.buildFileClient
import love.yinlin.foundation.buildSocketClient
import love.yinlin.uri.Uri
import kotlin.jvm.JvmName

object ClientEngine {
    @PublishedApi internal val Common by lazy { buildCommonNetClient() }
    @PublishedApi internal val File by lazy { buildFileClient() }
    @PublishedApi internal val Socket by lazy { buildSocketClient() }

    var baseUrl = ""

    fun init(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    fun proxy(proxy: String, url: String): String = "$baseUrl/$proxy?$proxy=${Uri.encodeUri(url)}"
}

@IOCoroutine
suspend inline fun <reified R : Any> API<out APIType>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    uploadFile: Boolean,
    crossinline block: suspend (HttpResponse) -> R
): R {
    val result = try {
        val context = currentCoroutineContext()
        val url = "${ClientEngine.baseUrl}$route"
        val client = if (uploadFile) ClientEngine.File else ClientEngine.Common

        Coroutines.io {
            client.internalPrepareStatement(HttpMethod.Post, url, builder).execute { response ->
                when (response.status) {
                    HttpStatusCode.OK -> Coroutines.with(context) { Data.Success(block(response)) }
                    HttpStatusCode.Accepted -> Data.Failure(FailureException(response.bodyAsText()))
                    HttpStatusCode.Unauthorized -> Data.Failure(UnauthorizedException("Unauthorized: 登录验证已过期"))
                    HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> Data.Failure(RequestTimeoutException(response.responseTime.timestamp - response.requestTime.timestamp))
                    else -> Data.Failure(IllegalArgumentException("HTTP Error: ${response.status}"))
                }
            }
        }
    }
    catch (e: Throwable) { Data.Failure(e) }
    when (result) {
        is Data.Success -> return result.data
        is Data.Failure -> throw result.throwable
    }
}

@JvmName("internalPostRequest")
@IOCoroutine
suspend inline fun <reified R : Any> API<APIType.Post>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> R
): R = internalRequest(builder, false, block)

@JvmName("internalFormRequest")
@IOCoroutine
suspend inline fun <reified R : Any> API<APIType.Form>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> R
): R = internalRequest(builder, true, block)

@IOCoroutine
suspend fun Sockets.openConnection(connection: WebSocketClient.Connection) = Coroutines.io {
    ClientEngine.Socket.connect(Uri.parse(ClientEngine.baseUrl)?.host, path, connection)
}