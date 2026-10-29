package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skia.Canvas
import org.jetbrains.skiko.internal.sdl.*

internal class SdlWindowEntry(
    val window: COpaquePointer,
    val scope: SdlWindowScope,
    private val renderer: WindowRenderer,
    wakeEventType: UInt,
    private val send: (NativeWindowEvent) -> Unit,
    private val onClose: () -> Unit,
    private val draw: (Canvas, Int, Int, Long) -> Unit,
) {
    val id: UInt = RachelSDL_GetWindowID(window)
    private val events = SdlWindowEvents(window, wakeEventType, send)
    var dirty = true
        private set
    private var closed = false
    var nanoTime = 0L
        private set

    fun dispatch(event: SDL_Event) {
        if (!closed && !scope.shouldClose) {
            events.dispatch(event)
            dirty = true
        }
    }

    fun opened() {
        send(NativeWindowEvent.Focus(RachelSDL_GetWindowFlags(window) and SDL_WINDOW_INPUT_FOCUS != 0uL))
        println("Renderer: ${renderer.description}; statically linked SDL3")
    }

    fun render() {
        scope.rethrowCallbackFailure()
        if (closed || scope.shouldClose || !dirty) return
        val width = scope.pixelWidth
        val height = scope.pixelHeight
        check(width in 0 .. 16384 && height in 0 .. 16384 && width.toLong() * height <= 67108864)
        dirty = false
        nanoTime = SDL_GetTicksNS().toLong()
        if (width > 0 && height > 0) renderer.render(width, height)
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (!closed) draw(canvas, width, height, nanoTime)
    }

    fun close() {
        if (closed) return
        closed = true
        var failure: Throwable? = null
        val cleanup: List<() -> Unit> = [onClose, scope::dispose, renderer::close, { RachelSDL_DestroyWindow(window) }]
        for (action in cleanup) {
            try { action() }
            catch (error: Throwable) {
                val previous = failure
                if (previous == null) failure = error
                else if (previous !== error) previous.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }
}
