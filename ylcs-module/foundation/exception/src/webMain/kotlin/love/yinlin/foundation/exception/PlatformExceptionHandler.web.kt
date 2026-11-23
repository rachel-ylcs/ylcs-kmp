@file:OptIn(ExperimentalWasmJsInterop::class, CompatibleRachelApi::class)
package love.yinlin.foundation.exception

import kotlinx.browser.window
import love.yinlin.annotation.CompatibleRachelApi
import love.yinlin.compatible.ThrowableCompatible
import org.w3c.dom.ErrorEvent
import org.w3c.dom.PromiseRejectionEvent
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget

private fun listener(callback: (Event) -> Unit): EventListener = js("({ handleEvent: callback })")
private fun reasonToString(reason: JsAny?): String = js("""
    (function() {
        try { return String(reason); }
        catch (error) { return 'Unknown rejection reason'; }
    })()
""")
private fun reasonStack(reason: JsAny?): String? = js("""
    (function() {
        try {
            const stack = reason == null ? null : reason.stack;
            return typeof stack === 'string' ? stack : null;
        }
        catch (error) { return null; }
    })()
""")

internal actual fun setupPlatformExceptionHandler(crashKey: String, handler: ExceptionHandler): AutoCloseable = window.observeBrowserExceptions(crashKey, handler)

internal fun EventTarget.observeBrowserExceptions(crashKey: String, handler: ExceptionHandler): AutoCloseable {
    val errorListener = listener { event ->
        if (event is ErrorEvent) {
            val cause = event.error
            val error = ThrowableCompatible(cause).build() ?: Throwable(event.message)
            val stack = reasonStack(cause) ?: error.stackTraceToString()
            handler.handle(crashKey, error, "${event.filename} - ${event.lineno} - ${event.colno}\n${event.message}\n$stack")
        }
    }
    val rejectionListener = listener { event ->
        if (event is PromiseRejectionEvent) {
            val reason = event.reason
            val message = "Unhandled promise rejection: ${reasonToString(reason)}"
            val error = ThrowableCompatible(reason).build() ?: Throwable(message)
            val stack = reasonStack(reason) ?: error.stackTraceToString()
            handler.handle(crashKey, error, "$message\n$stack")
        }
    }

    addEventListener("error", errorListener)
    addEventListener("unhandledrejection", rejectionListener)
    return AutoCloseable {
        removeEventListener("error", errorListener)
        removeEventListener("unhandledrejection", rejectionListener)
    }
}
