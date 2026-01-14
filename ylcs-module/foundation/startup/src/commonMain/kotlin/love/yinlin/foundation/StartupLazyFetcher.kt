package love.yinlin.foundation

fun interface StartupLazyFetcher<T> {
    fun fetch(): T
}