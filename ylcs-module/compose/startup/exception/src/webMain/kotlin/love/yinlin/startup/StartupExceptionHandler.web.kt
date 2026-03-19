package love.yinlin.startup

import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import love.yinlin.extension.raw
import love.yinlin.foundation.PlatformContextProvider
import love.yinlin.foundation.StartupArg
import love.yinlin.foundation.StartupArgs
import love.yinlin.foundation.StartupHandler
import love.yinlin.foundation.SyncStartup
import kotlin.js.ExperimentalWasmJsInterop

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

    @OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
    actual override fun init(scope: CoroutineScope, args: StartupArgs) {
        mCrashKey = args[0]
        val handler: Handler = args[1]
        window.onerror = { message, source, lineno, colno, error ->
            val e = ThrowableCompatible(error).build() ?: Throwable(message?.toString() ?: "error")
            val errorString = "$source - $lineno - $colno\n$message\n${e.stackTraceToString()}"
            handler.handle(mCrashKey, e, errorString)
            false.raw
        }
    }
}