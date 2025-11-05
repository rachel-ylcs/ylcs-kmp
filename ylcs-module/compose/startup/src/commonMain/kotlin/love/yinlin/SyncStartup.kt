package love.yinlin

fun interface SyncStartup : Startup {
    fun init(context: Context, args: StartupArgs)
    fun initDelay(context: Context, args: StartupArgs) { }
    fun destroy(context: Context, args: StartupArgs) { }
    fun destroyDelay(context: Context, args: StartupArgs) { }
}