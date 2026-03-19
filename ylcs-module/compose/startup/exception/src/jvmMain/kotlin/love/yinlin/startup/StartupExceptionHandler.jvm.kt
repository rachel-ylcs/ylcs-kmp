package love.yinlin.startup

import kotlinx.coroutines.CoroutineScope
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupHandler
import love.yinlin.foundation.SyncStartup

@StartupArg(index = 0, name = "crashKey", type = String::class)
@StartupHandler(
    index = 1,
    name = "onError",
    handlerType = StartupExceptionHandler.Handler::class,
    returnType = Unit::class,
    String::class, Throwable::class, String::class
)
actual class StartupExceptionHandler actual constructor(context: PlatformContextProvider) : SyncStartup(context) {
    actual fun interface Handler {
        actual fun handle(key: String, e: Throwable, error: String)
    }

    private lateinit var mCrashKey: String

    actual val crashKey: String get() = mCrashKey

    actual override fun init(scope: CoroutineScope, args: StartupArgs) {
        mCrashKey = args[0]
        val handler: Handler = args[1]
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
    }
}