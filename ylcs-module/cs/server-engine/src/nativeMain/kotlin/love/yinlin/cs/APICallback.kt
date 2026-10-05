package love.yinlin.cs

open class APICallbackScope {
    fun expire(): Nothing = throw UnauthorizedException(null)
    fun failure(message: String? = null): Nothing = throw FailureException(message)
}

typealias APICallback<I, O, C> = suspend APICallbackScope.(C, I) -> O
typealias APICallbackMap<K, I, O, C> = MutableMap<K, APICallback<I, O, C>>

fun <K : Any, I, O, C : Any> buildCallBackMap(): APICallbackMap<K, I, O, C> = mutableMapOf<K, APICallback<I, O, C>>()