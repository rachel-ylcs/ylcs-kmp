package love.yinlin.startup

import androidx.compose.runtime.Stable
import love.yinlin.Context
import love.yinlin.StartupArg
import love.yinlin.StartupArgs
import love.yinlin.StartupHandler
import love.yinlin.SyncStartup

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
@Stable
expect class StartupExceptionHandler() : SyncStartup {
    @Stable
    fun interface Handler {
        fun handle(key: String, e: Throwable, error: String)
    }

    val crashKey: String

    override fun init(context: Context, args: StartupArgs)
}