package love.yinlin

fun interface AsyncStartup : Startup {
    suspend fun init(context: Context, args: StartupArgs)
    suspend fun initDelay(context: Context, args: StartupArgs) { }
    fun destroy(context: Context, args: StartupArgs) { }
    fun destroyDelay(context: Context, args: StartupArgs) { }
}