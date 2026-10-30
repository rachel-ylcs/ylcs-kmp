package love.yinlin.exception

import kotlinx.browser.window
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import love.yinlin.extension.raw
import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
internal actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable {
    val previous = window.onerror
    window.onerror = { message, source, lineno, colno, error ->
        val e = ThrowableCompatible(error).build() ?: Throwable(message?.toString() ?: "error")
        val errorString = "$source - $lineno - $colno\n$message\n${e.stackTraceToString()}"
        handler.handle(crashKey, e, errorString)
        false.raw
    }
    val installed = window.onerror
    return AutoCloseable {
        if (window.onerror === installed) window.onerror = previous
    }
}
