package love.yinlin.service

fun interface StartupLazyFetcher<T> {
    fun fetch(): T
}