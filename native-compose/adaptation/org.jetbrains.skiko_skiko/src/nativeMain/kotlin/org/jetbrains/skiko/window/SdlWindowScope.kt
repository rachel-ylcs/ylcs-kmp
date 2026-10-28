@file:OptIn(ExperimentalAtomicApi::class)

package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.*
import org.jetbrains.skiko.internal.sdl.SDL_SystemCursor.*
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

internal class SdlWindowScope(private val window: COpaquePointer, private val wakeEventType: UInt) : NativeWindowScope {
    private val active = AtomicBoolean(true)
    private val closing = AtomicBoolean(false)
    private val cursors = mutableMapOf<NativeWindowCursor, COpaquePointer>()
    private var dropFailure: Throwable? = null
    private val dropTarget = SdlWindowDropTarget(window) { failure ->
        if (dropFailure == null) dropFailure = failure
        close()
    }
    val shouldClose: Boolean get() = closing.load()

    override val displayScale: Float get() {
        checkHostThread()
        return RachelSDL_GetWindowDisplayScale(window).takeIf { it.isFinite() && it > 0f } ?: 1f
    }

    override val pixelDensity: Float get() {
        checkHostThread()
        return RachelSDL_GetWindowPixelDensity(window).takeIf { it.isFinite() && it > 0f } ?: 1f
    }

    override val pixelWidth: Int get() = memScoped {
        checkHostThread()
        val width = alloc<IntVar>()
        check(RachelSDL_GetWindowSizeInPixels(window, width.ptr, null)) { SDL_GetError()?.toKString() ?: "Unable to read window width" }
        width.value
    }

    override val pixelHeight: Int get() = memScoped {
        checkHostThread()
        val height = alloc<IntVar>()
        check(RachelSDL_GetWindowSizeInPixels(window, null, height.ptr)) { SDL_GetError()?.toKString() ?: "Unable to read window height" }
        height.value
    }

    override fun invalidate() {
        if (active.load()) RachelSDL_WakeEventLoop(wakeEventType)
    }

    override fun close() {
        if (active.load()) {
            closing.store(true)
            invalidate()
        }
    }

    override fun setCursor(cursor: NativeWindowCursor) {
        checkHostThread()
        val handle = cursors.getOrPut(cursor) {
            val kind = when (cursor) {
                NativeWindowCursor.DEFAULT -> SDL_SYSTEM_CURSOR_DEFAULT
                NativeWindowCursor.CROSSHAIR -> SDL_SYSTEM_CURSOR_CROSSHAIR
                NativeWindowCursor.TEXT -> SDL_SYSTEM_CURSOR_TEXT
                NativeWindowCursor.HAND -> SDL_SYSTEM_CURSOR_POINTER
                NativeWindowCursor.MOVE -> SDL_SYSTEM_CURSOR_MOVE
                NativeWindowCursor.NOT_ALLOWED -> SDL_SYSTEM_CURSOR_NOT_ALLOWED
                NativeWindowCursor.WAIT -> SDL_SYSTEM_CURSOR_WAIT
            }
            checkNotNull(RachelSDL_CreateCursor(kind)) { SDL_GetError()?.toKString() ?: "Unable to create cursor" }
        }
        check(RachelSDL_SetCursor(handle)) { SDL_GetError()?.toKString() ?: "Unable to set cursor" }
    }

    override fun setSize(width: Int, height: Int) {
        checkHostThread()
        require(width in 1 .. 16384 && height in 1 .. 16384 && width.toLong() * height <= 67108864)
        check(RachelSDL_SetWindowSize(window, width, height)) { SDL_GetError()?.toKString() ?: "Unable to resize window" }
    }

    override fun setDropTarget(target: NativeWindowDropTarget?) {
        checkHostThread()
        dropTarget.target = target
    }

    fun rethrowDropFailure() {
        dropFailure?.let { throw it }
    }

    override fun startTextInput() {
        checkHostThread()
        check(RachelSDL_StartTextInput(window)) { SDL_GetError()?.toKString() ?: "Unable to start text input" }
    }

    override fun stopTextInput() {
        checkHostThread()
        check(RachelSDL_StopTextInput(window)) { SDL_GetError()?.toKString() ?: "Unable to stop text input" }
    }

    override fun setTextInputArea(x: Int, y: Int, width: Int, height: Int) {
        checkHostThread()
        require(width >= 0 && height >= 0) { "Text input area has a negative size" }
        check(RachelSDL_SetTextInputArea(window, x, y, width, height)) { SDL_GetError()?.toKString() ?: "Unable to set text input area" }
    }

    fun dispose() {
        active.store(false)
        dropTarget.dispose()
        cursors.values.forEach(::RachelSDL_DestroyCursor)
        cursors.clear()
    }

    private fun checkHostThread() {
        check(active.load() && SDL_IsMainThread()) { "Native window controls require an active host on the main thread" }
    }
}
