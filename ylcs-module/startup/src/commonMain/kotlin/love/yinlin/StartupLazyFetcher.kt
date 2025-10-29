package love.yinlin

fun interface StartupLazyFetcher<T> {
    fun fetch(): T
}