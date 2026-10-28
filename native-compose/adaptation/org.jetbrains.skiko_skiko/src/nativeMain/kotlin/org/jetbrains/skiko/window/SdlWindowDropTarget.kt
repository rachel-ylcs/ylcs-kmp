package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.RachelSDL_SetDropCallback
import org.jetbrains.skiko.internal.sdl.RachelSDL_GetWindowPixelDensity

internal class SdlWindowDropTarget(private val window: COpaquePointer, private val onFailure: (Throwable) -> Unit) {
    var target: NativeWindowDropTarget? = null
    private val reference = StableRef.create(this)

    init {
        val callback = staticCFunction { userdata: COpaquePointer?, types: UInt, x: Float, y: Float ->
            userdata?.asStableRef<SdlWindowDropTarget>()?.get()?.accept(types, x, y) ?: 0u
        }
        if (!RachelSDL_SetDropCallback(window, callback, reference.asCPointer())) {
            RachelSDL_SetDropCallback(window, null, null)
            reference.dispose()
            error("Unable to install the Native window drop callback")
        }
    }

    private fun accept(types: UInt, x: Float, y: Float): UInt = try {
        val scale = RachelSDL_GetWindowPixelDensity(window).takeIf { it.isFinite() && it > 0f } ?: 1f
        target?.accept(types.toInt(), x * scale, y * scale)?.toUInt() ?: 0u
    }
    catch (failure: Throwable) {
        onFailure(failure)
        0u
    }

    fun dispose() {
        RachelSDL_SetDropCallback(window, null, null)
        target = null
        reference.dispose()
    }
}
