package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.RachelSDL_CreateWindowHitTest
import org.jetbrains.skiko.internal.sdl.RachelSDL_DestroyWindowHitTest
import org.jetbrains.skiko.internal.sdl.SDL_GetError

internal class SdlWindowHitTest(private val window: COpaquePointer, var hitTest: (Float, Float) -> Boolean, private val onFailure: (Throwable) -> Unit) {
    private val reference = StableRef.create(this)
    private val handle: COpaquePointer

    init {
        val callback = staticCFunction { userdata: COpaquePointer?, x: Float, y: Float ->
            userdata?.asStableRef<SdlWindowHitTest>()?.get()?.accept(x, y) ?: false
        }
        val pointer = RachelSDL_CreateWindowHitTest(window, callback, reference.asCPointer())
        if (pointer == null) {
            reference.dispose()
            error(SDL_GetError()?.toKString() ?: "Unable to install Native window hit testing")
        }
        handle = pointer
    }

    private fun accept(x: Float, y: Float): Boolean = try {
        hitTest(x, y)
    }
    catch (failure: Throwable) {
        onFailure(failure)
        false
    }

    fun dispose() {
        RachelSDL_DestroyWindowHitTest(window, handle)
        reference.dispose()
    }
}
