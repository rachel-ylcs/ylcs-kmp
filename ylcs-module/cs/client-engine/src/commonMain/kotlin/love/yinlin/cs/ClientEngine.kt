package love.yinlin.cs

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.currentCoroutineContext
import love.yinlin.data.Data
import love.yinlin.extension.catchingDefault
import love.yinlin.coroutines.Coroutines
import love.yinlin.uri.Uri
import kotlin.jvm.JvmName

object ClientEngine {
    var baseUrl = ""

    fun init(baseUrl: String) {
        this.baseUrl = baseUrl
    }

    fun proxy(proxy: String, url: String): String = "$baseUrl/$proxy?$proxy=${Uri.encodeUri(url)}"
}

suspend inline fun <reified R : Any> API<out APIType>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    uploadFile: Boolean,
    crossinline block: suspend (HttpResponse) -> R
): R {
    val result = catchingDefault(Data.Failure(IllegalStateException("出现错误了呀"))) {
        val context = currentCoroutineContext()
        val url = "${ClientEngine.baseUrl}$route"
        Coroutines.io {
            NetClient.internalPrepareStatement(HttpMethod.Post, uploadFile, url, builder).execute { response ->
                when (response.status) {
                    HttpStatusCode.OK -> Coroutines.with(context) { Data.Success(block(response)) }
                    HttpStatusCode.Accepted -> Data.Failure(FailureException(response.bodyAsText()))
                    HttpStatusCode.Unauthorized -> Data.Failure(UnauthorizedException("登录验证已过期"))
                    HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> Data.Failure(RequestTimeoutException(response.responseTime.timestamp - response.requestTime.timestamp))
                    else -> Data.Failure(IllegalArgumentException("出现错误了呀"))
                }
            }
        }
    }
    when (result) {
        is Data.Success -> return result.data
        is Data.Failure -> throw result.throwable
    }
}

@JvmName("internalPostRequest")
suspend inline fun <reified R : Any> API<APIType.Post>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> R
): R = internalRequest(builder, false, block)

@JvmName("internalFormRequest")
suspend inline fun <reified R : Any> API<APIType.Form>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    crossinline block: suspend (HttpResponse) -> R
): R = internalRequest(builder, true, block)