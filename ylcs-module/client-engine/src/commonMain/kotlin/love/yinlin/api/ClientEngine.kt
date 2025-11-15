package love.yinlin.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.currentCoroutineContext
import love.yinlin.extension.catchingDefault
import love.yinlin.platform.Coroutines
import love.yinlin.platform.NetClient

data object ClientEngine {
    var baseUrl = ""

    fun init(baseUrl: String) {
        this.baseUrl = baseUrl
    }
}

suspend inline fun API<out APIType>.internalRequest(
    noinline builder: HttpRequestBuilder.() -> Unit,
    uploadFile: Boolean = false,
    crossinline block: suspend (HttpResponse) -> Unit
): Throwable? = catchingDefault(IllegalStateException("非法异常")) {
    val context = currentCoroutineContext()
    val url = "${ClientEngine.baseUrl}$route"
    Coroutines.io {
        NetClient.internalPrepareStatement(HttpMethod.Post, uploadFile, url, builder).execute { response ->
            when (response.status) {
                HttpStatusCode.OK -> {
                    Coroutines.with(context) { block(response) }
                    null
                }
                HttpStatusCode(1211, "") -> FailureException(response.bodyAsText())
                HttpStatusCode.Unauthorized -> UnauthorizedException("登录验证已过期")
                HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> RequestTimeoutException(response.responseTime.timestamp - response.requestTime.timestamp)
                else -> IllegalArgumentException()
            }
        }
    }
}