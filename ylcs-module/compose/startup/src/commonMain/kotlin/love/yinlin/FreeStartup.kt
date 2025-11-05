package love.yinlin

import androidx.compose.runtime.Stable

@Stable
fun interface FreeStartup : Startup {
    suspend fun init(context: Context, args: StartupArgs)
    suspend fun initDelay(context: Context, args: StartupArgs) { }
}