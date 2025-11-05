package love.yinlin

import androidx.compose.runtime.Stable

@Stable
fun interface AsyncStartup : Startup {
    suspend fun init(context: Context, args: StartupArgs)
    suspend fun initDelay(context: Context, args: StartupArgs) { }
    fun destroy(context: Context, args: StartupArgs) { }
    fun destroyDelay(context: Context, args: StartupArgs) { }
}