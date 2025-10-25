package love.yinlin.service

fun interface SyncStartup : Startup {
    fun init(context: PlatformContext, args: Array<Any?>)
}