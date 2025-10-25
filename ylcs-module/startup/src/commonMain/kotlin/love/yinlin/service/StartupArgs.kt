package love.yinlin.service

class StartupArgs(val args: Array<Any?>) {
    inline operator fun <reified T> get(index: Int): T = args[index] as T
}