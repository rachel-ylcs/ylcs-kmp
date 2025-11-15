package love.yinlin.api

open class APICallbackScope {
    fun expire(): Nothing = throw UnauthorizedException(null)
    fun failure(message: String? = null): Nothing = throw FailureException(message)
}

typealias APICallback<I, O> = suspend APICallbackScope.(I) -> O
typealias APICallbackMap<K, I, O> = MutableMap<K, APICallback<I, O>>

fun <K : Any, I, O> buildCallBackMap(): APICallbackMap<K, I, O> = mutableMapOf<K, APICallback<I, O>>()