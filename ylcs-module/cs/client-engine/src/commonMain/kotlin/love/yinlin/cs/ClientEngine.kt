package love.yinlin.cs

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.currentCoroutineContext
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
}

@IOCoroutine
suspend inline fun <reified R : Any> API<out APIType>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    uploadFile: Boolean,
    crossinline block: suspend (HttpResponse) -> R
): R {
    val context = currentCoroutineContext()
    val url = "${ClientEngine.baseUrl}$route"
    val client = if (uploadFile) ClientEngine.File else ClientEngine.Common

    return Coroutines.io {
        client.internalPrepareStatement(HttpMethod.Post, url, builder).execute { response ->
            when (response.status) {
                HttpStatusCode.OK -> Coroutines.with(context) { block(response) }
                HttpStatusCode.Accepted -> throw FailureException(response.bodyAsText())
                HttpStatusCode.Unauthorized -> throw UnauthorizedException("Unauthorized: 登录验证已过期")
                HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> throw RequestTimeoutException(response.responseTime.timestamp - response.requestTime.timestamp)
                else -> throw IllegalArgumentException("HTTP Error: ${response.status}")
            }
        }
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