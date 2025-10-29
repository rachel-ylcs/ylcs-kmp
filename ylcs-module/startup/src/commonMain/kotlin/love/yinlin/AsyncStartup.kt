package love.yinlin

fun interface AsyncStartup : Startup {
    suspend fun init(context: Context, args: StartupArgs)
}