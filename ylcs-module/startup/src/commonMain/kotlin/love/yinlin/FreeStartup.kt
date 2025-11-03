package love.yinlin

fun interface FreeStartup : Startup {
    suspend fun init(context: Context, args: StartupArgs)
    suspend fun initDelay(context: Context, args: StartupArgs) { }
}