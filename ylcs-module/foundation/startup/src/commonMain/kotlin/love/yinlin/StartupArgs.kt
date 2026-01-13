package love.yinlin

class StartupArgs(val args: Array<Any?>) {
    inline operator fun <reified T> get(index: Int): T = args[index] as T
    inline fun <reified T> fetch(index: Int): T = (args[index] as StartupLazyFetcher<*>).fetch() as T
    inline fun <reified T> map(): List<T> = args.map { it as T }
}