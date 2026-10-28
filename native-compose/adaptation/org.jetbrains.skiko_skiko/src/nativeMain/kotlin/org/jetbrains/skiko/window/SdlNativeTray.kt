package org.jetbrains.skiko.window

import kotlinx.cinterop.*
import org.jetbrains.skiko.internal.sdl.*

internal class SdlNativeTray private constructor(private val handle: COpaquePointer, onClose: (SdlNativeTray) -> Unit) : NativeTray {
    private var onClose: ((SdlNativeTray) -> Unit)? = onClose
    private var closed = false
    private var tooltip: String? = null
    override val isActive: Boolean get() = !closed

    companion object {
        fun create(tooltip: String, onClose: (SdlNativeTray) -> Unit): SdlNativeTray? {
            require(tooltip.length <= 1024 && '\u0000' !in tooltip) { "Invalid tray tooltip" }
            val handle = RachelSDL_CreateTray(tooltip) ?: return null
            return SdlNativeTray(handle, onClose)
        }
    }

    override fun setIcon(width: Int, height: Int, pixels: IntArray) {
        checkHostThread()
        require(width in 1 .. 1024 && height in 1 .. 1024 && pixels.size == width * height)
        check(pixels.usePinned { RachelSDL_SetTrayIcon(handle, width, height, it.addressOf(0)) }) { "Unable to copy the tray icon" }
    }

    override fun setTooltip(tooltip: String) {
        checkHostThread()
        require(tooltip.length <= 1024 && '\u0000' !in tooltip) { "Invalid tray tooltip" }
        if (this.tooltip == tooltip) return
        RachelSDL_SetTrayTooltip(handle, tooltip)
        this.tooltip = tooltip
    }

    override fun close() {
        if (closed) return
        checkHostThread()
        closed = true
        RachelSDL_DestroyTray(handle)
        val callback = onClose
        onClose = null
        callback?.invoke(this)
    }

    private fun checkHostThread() {
        check(!closed && SDL_IsMainThread()) { "Tray controls require an active tray on the main thread" }
    }
}
