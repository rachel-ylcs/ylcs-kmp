package love.yinlin.startup

import love.yinlin.foundation.Context
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupHandler
import love.yinlin.foundation.SyncStartup
import kotlin.experimental.ExperimentalNativeApi

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
actual class StartupExceptionHandler : SyncStartup() {
    actual fun interface Handler {
        actual fun handle(key: String, e: Throwable, error: String)
    }

    private lateinit var mCrashKey: String

    actual val crashKey: String get() = mCrashKey

    @OptIn(ExperimentalNativeApi::class)
    actual override fun init(context: Context, args: StartupArgs) {
        mCrashKey = args[0]
        val handler: Handler = args[1]
        setUnhandledExceptionHook { e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
    }
}