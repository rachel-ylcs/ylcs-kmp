package love.yinlin

fun interface SyncStartup : Startup {
    fun init(context: Context, args: StartupArgs)
}