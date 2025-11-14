package love.yinlin.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.preparePost
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import love.yinlin.extension.catchingError
import love.yinlin.platform.NetClient

data object ClientEngine {
    var baseUrl = ""

    fun init(baseUrl: String) {
        this.baseUrl = baseUrl
    }
}



suspend inline fun API<out APIType>.internalRequest(builder: HttpRequestBuilder.() -> Unit, crossinline block: suspend (HttpResponse) -> Unit): Throwable? = catchingError {
    val url = "${ClientEngine.baseUrl}$route"
    NetClient.common.preparePost(urlString = url, block = builder).execute { response ->
        when (response.status) {
            HttpStatusCode.OK -> block(response)
            HttpStatusCode(1211, "") -> throw FailureException(response.bodyAsText())
            HttpStatusCode.Unauthorized -> throw UnauthorizedException("登录认证已过期")
            HttpStatusCode.RequestTimeout, HttpStatusCode.GatewayTimeout -> throw RequestTimeoutException(response.responseTime.timestamp - response.requestTime.timestamp)
            else -> throw IllegalArgumentException("非法异常")
        }
    }
}