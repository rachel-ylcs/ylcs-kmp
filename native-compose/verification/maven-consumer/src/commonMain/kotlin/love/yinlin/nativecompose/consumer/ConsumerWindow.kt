package love.yinlin.nativecompose.consumer

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Color
import org.jetbrains.skiko.window.NativeWindow
import org.jetbrains.skiko.window.NativeWindowBackend
import org.jetbrains.skiko.window.NativeWindowEvent
import org.jetbrains.skiko.window.NativeWindowOptions
import org.jetbrains.skiko.window.NativeWindowScope
import kotlin.time.Duration.Companion.seconds

internal fun runConsumerWindow() {
    val ready = CompletableDeferred<NativeWindowScope>()
    val timedOut = CompletableDeferred<Unit>()
    val watchdog = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    watchdog.launch {
        val window = ready.await()
        delay(10.seconds)
        timedOut.complete(Unit)
        window.close()
    }
    var window: NativeWindowScope? = null
    var frames = 0
    var initialWidth = 0
    var initialHeight = 0
    var resized = false
    try {
        NativeWindow.open(
            options = NativeWindowOptions(title = "Maven-only Native window", backend = NativeWindowBackend.SOFTWARE),
            onOpen = {
                window = it
                ready.complete(it)
            },
            onEvent = { if (it == NativeWindowEvent.CloseRequested) window?.close() },
        ) { canvas, width, height, _ ->
            canvas.clear(Color.BLUE)
            frames++
            if (frames == 1) {
                initialWidth = width
                initialHeight = height
                checkNotNull(window).setSize(960, 540)
            }
            else if (width != initialWidth || height != initialHeight) {
                resized = true
                println("PASS: Maven-only static SDL; resize ${initialWidth}x$initialHeight -> ${width}x$height; $frames frames")
                window?.close()
            }
        }
        check(!timedOut.isCompleted) { "Native window verification timed out" }
        check(resized) { "Native window closed before verifying resize" }
    }
    finally { watchdog.cancel() }
}
