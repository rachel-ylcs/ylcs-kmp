package love.yinlin.startup

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
actual class StartupExceptionHandler : SyncStartup {
    actual fun interface Handler {
        actual fun handle(key: String, e: Throwable, error: String)
    }

    private lateinit var mCrashKey: String

    actual val crashKey: String get() = mCrashKey

    actual override fun init(context: Context, args: StartupArgs) {
        mCrashKey = args[0]
        val handler: Handler = args[1]
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
    }
}