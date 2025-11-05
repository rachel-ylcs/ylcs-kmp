package love.yinlin.startup

import androidx.compose.runtime.Stable
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
@Stable
actual class StartupExceptionHandler : SyncStartup {
    @Stable
    actual fun interface Handler {
        actual fun handle(key: String, e: Throwable, error: String)
    }

    private lateinit var mCrashKey: String

    actual val crashKey: String get() = mCrashKey

    @OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
    actual override fun init(context: Context, args: StartupArgs) {
        mCrashKey = args[0]
        val handler: Handler = args[1]
        setUnhandledExceptionHook { e ->
            handler.handle(crashKey, e, e.stackTraceToString())
        }
        val exceptionHandler: CPointer<NSUncaughtExceptionHandler> = staticCFunction { e ->
            if (e != null) handler.handle(crashKey, Throwable(e.reason), "${e.reason}\n${e.callStackSymbols.joinToString(",")}")
        }
        NSSetUncaughtExceptionHandler(exceptionHandler)
    }
}