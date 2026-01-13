package love.yinlin.startup

import kotlinx.browser.window
import love.yinlin.Context
import love.yinlin.StartupArg
import love.yinlin.StartupArgs
import love.yinlin.StartupHandler
import love.yinlin.SyncStartup
import love.yinlin.extension.asThrowableOrNull
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsBoolean

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

    @OptIn(ExperimentalWasmJsInterop::class)
    actual override fun init(context: Context, args: StartupArgs) {
        mCrashKey = args[0]
        val handler: Handler = args[1]
        window.onerror = { message, source, lineno, colno, error ->
            val e = error?.asThrowableOrNull ?: Throwable(message?.toString() ?: "error")
            val errorString = "$source - $lineno - $colno\n$message\n$e"
            handler.handle(mCrashKey, e, errorString)
            false.toJsBoolean()
        }
    }
}