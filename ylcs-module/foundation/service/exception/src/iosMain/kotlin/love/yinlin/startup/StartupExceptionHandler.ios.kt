package love.yinlin.startup

import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import love.yinlin.Context
import love.yinlin.StartupArg
import love.yinlin.StartupArgs
import love.yinlin.StartupHandler
import love.yinlin.SyncStartup
import platform.Foundation.NSSetUncaughtExceptionHandler
import platform.Foundation.NSUncaughtExceptionHandler
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
    private lateinit var mHandler: Handler

    actual val crashKey: String get() = mCrashKey

    companion object {
        private lateinit var instance: StartupExceptionHandler
    }

    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    actual override fun init(context: Context, args: StartupArgs) {
        instance = this
        mCrashKey = args[0]
        mHandler = args[1]
        setUnhandledExceptionHook { e ->
            mHandler.handle(crashKey, e, e.stackTraceToString())
        }
        // staticCFunction must take an unbound, non-capturing function or lambda
        val exceptionHandler: CPointer<NSUncaughtExceptionHandler> = staticCFunction { e ->
            e?.let {
                instance.mHandler.handle(instance.crashKey,
                    Throwable(e.reason), "${e.reason}\n${e.callStackSymbols.joinToString(",")}")
            }
        }
        NSSetUncaughtExceptionHandler(exceptionHandler)
    }
}